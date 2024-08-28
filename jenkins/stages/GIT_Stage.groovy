/*******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/
import static java.net.URLEncoder.encode as urlEncode
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils
import static com.ericsson.orchestration.mgmt.libs.VnfmDocker.getDockerImagePath
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkArgs
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkNotBlank
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.compareVersion
import static com.ericsson.orchestration.mgmt.libs.VnfmGerrit.abandonChange
import static com.ericsson.orchestration.mgmt.libs.VnfmGerrit.isSubmittable
import static com.ericsson.orchestration.mgmt.libs.VnfmGerrit.getProjectGroup
import static com.ericsson.orchestration.mgmt.libs.VnfmGerrit.listChanges
import static com.ericsson.orchestration.mgmt.libs.VnfmGerrit.lockBranch
import static com.ericsson.orchestration.mgmt.libs.VnfmGerrit.restCall
import static com.ericsson.orchestration.mgmt.libs.VnfmGerrit.setChangeReview
import static com.ericsson.orchestration.mgmt.libs.VnfmGit.checkout
import static com.ericsson.orchestration.mgmt.libs.VnfmGit.preCommit
import static com.ericsson.orchestration.mgmt.libs.VnfmGit.getChangeId
import static com.ericsson.orchestration.mgmt.libs.VnfmJenkins.getCredential
import static com.ericsson.orchestration.mgmt.libs.VnfmUtils.overwriteYaml
import static com.ericsson.orchestration.mgmt.libs.VnfmUtils.upliftVersion


/* Stage for Checkout Project. Use:
- VARs:
    DISABLE_SUBMODULES
- Args:
    project(require): type String; Gerrit project name
    branch(require): type String; Gerrit project branch
    refspec: type String; Gerrit change refspec; default is 'refs/heads/master'
    type: type String; Name of checkout project type; default is 'base'
    tag: type String; Name of the tag; default is '0.1.0'
    stageName: type String; Name of the stage; default is 'Checkout Project: project'
*/
def CheckoutProject(Map Args) {
    String stageName = 'CheckoutProject'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    branch: [value: Args['branch'], type: 'string'],
                    refspec: [value: Args['refspec'], type: 'string', require: false],
                    type: [value: Args['type'], type: 'string', require: false],
                    tag: [value: Args['tag'], type: 'string', require: false],
                    stageName: [value: Args['stageName'], type: 'string', require: false]]
    String projectGroup
    String url
    String comm


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['refspec'] = Args.containsKey('refspec') ? Args['refspec'] : 'refs/heads/master'
    Args['type'] = Args.containsKey('type') ? Args['type'] : 'base'
    Args['tag'] = Args.containsKey('tag') ? Args['tag'] : '0.1.0'
    Args['stageName'] = Args.containsKey('stageName') ? Args['stageName'] : 'Checkout Project: ' + Args['project']


    stage(Args['stageName']) {
        // Set projectGroup
        projectGroup = getProjectGroup(Args['project'])
        url = GERRIT_HTTP_URL + '/' + projectGroup + '/' + Args['project']

        println('INFO: Checkout project...')
        switch(Args['type']) {
            case 'push':
                dir(Args['project']) {
                    withCredentials([gitUsernamePassword( credentialsId: getCredential(url),
                                                          gitToolName: 'Default')]) {
                        comm  = """ git clone ${url} .
                                  | git checkout ${Args['branch']}""".stripMargin()
                        sh(comm)
                    }
                }
            break
            case 'base':
                dir(Args['project']) {
                    checkout( url: url,
                              branch: Args['branch'],
                              offSubmodules: env.DISABLE_SUBMODULES.toBoolean(),
                              this)

                    println('INFO: Set active branch...')
                    withCredentials([gitUsernamePassword( credentialsId: getCredential(url),
                                                          gitToolName: 'Default')]) {
                        comm = """  git checkout ${Args['branch']}
                                  | git pull --rebase""".stripMargin()
                        sh(comm)
                    }
                }
            break
            case 'change':
                dir(Args['project']) {
                    checkout( url: url,
                              branch: Args['branch'],
                              this)

                    println('INFO: Checkout change...')
                    withCredentials([gitUsernamePassword( credentialsId: getCredential(url),
                                                          gitToolName: 'Default')]) {
                        comm = """  git fetch ${url} ${Args['refspec']}
                                  | git checkout FETCH_HEAD""".stripMargin()
                        sh(comm)

                        if(!env.DISABLE_SUBMODULES.toBoolean()) {
                            println('INFO: Update Git modules URL...')
                            String subModules = readFile(file: '.gitmodules')
                            subModules = subModules.replaceAll('ssh://gerritmirror-ha.lmera.ericsson.se:29418', GERRIT_HTTP_URL)
                                                    .replaceAll('ssh://gerrit.ericsson.se:29418', GERRIT_HTTP_URL)
                            writeFile(text: subModules, file: '.gitmodules')

                            println('Checkout submodules...')
                            comm = 'git submodule update --init --remote'
                            sh(comm)
                        }
                    }
                }
            break
            case 'tag':
                dir(Args['project']) {
                    checkout( url: url,
                              branch: Args['branch'],
                              this)

                    println('INFO: Checkout change...')
                    withCredentials([gitUsernamePassword( credentialsId: getCredential(url),
                                                          gitToolName: 'Default')]) {
                        comm = """  git fetch --all --tags
                                  | git checkout tags/${Args['tag']} \\
                                  | -b branch-${Args['tag']}""".stripMargin()
                        sh(comm)
                    }
                }
            break
        }
    }
}


/* Stage for Git Commit Release changes. Use:
- VARs:
    GIT_COMMIT_FILES
- Job's ENVs:
    PROJECT_VERSION
    GIT_EMAIL
    GIT_USERNAME
- Args:
    project(require): type String; Gerrit project name
    version: type String; Value of the committing version; default is env.PROJECT_VERSION
*/
def CommitRelease(Map Args) {
    String stageName = 'CommitRelease'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    version: [value: Args['version'], type: 'string', require: false]]
    String comm
    String output
    Boolean tagExists = false


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['version'] = Args.containsKey('version') ? Args['version'] : env.PROJECT_VERSION


    stage('Git Commit Release') {
        dir(Args['project']) {
            println('Prepare for commit...')
            comm = """  git config --global user.email '${env.GIT_EMAIL}'
                      | git config --global user.name '${env.GIT_USERNAME}'""".stripMargin()
            sh(comm)

            switch(Args['project']) {
                case 'eric-eo-vnfm-helm-executor':
                    println('Update version on eric-product-info.yaml...')
                    overwriteYaml(file: 'eric-product-info.yaml',
                                  key: 'version',
                                  value: Args['version'],
                                  this)
                break
                case 'am-cvnfm-utils':
                    println('Update version on eric-product-info.yaml...')
                    overwriteYaml(file: env.PRODUCT_INFO,
                                  key: 'version',
                                  value: Args['version'],
                                  this)
                break
                case 'am-integration-charts':
                    println('Update version on pom files...')
                    comm = """mvn versions:set \\
                              | -DgenerateBackupPoms=false \\
                              | -q \\
                              | -DnewVersion=${Args['version']}""".stripMargin()
                    sh(comm)
                break
            }

            println('Commit updates...')
            comm = """  git status
                      | git diff
                      | git add ${env.GIT_COMMIT_FILES}
                      | git commit -m 'Committing version ${Args['version']}'
                      | git log -3""".stripMargin()
            sh(comm)

            println('Check if the tag exists...')
            comm = 'git tag --list'
            output = sh(script: comm, returnStdout: true).trim()

            output.split('\n').each {
                if(it == Args['version']) {
                    tagExists = true
                    println('Tag ' + Args['version'] + ' exists')
                }
            }

            if(!tagExists) {
                println('Create tag...')
                comm = "git tag ${Args['version']}"
                sh(comm)
            }
        }
    }
}


/* Stage for Git Commit Snapshot changes. Use:
- VARs:
    GIT_COMMIT_FILES
    POM_FILE(for gr-controller)
- Job's ENVs:
    PROJECT_VERSION
    GIT_EMAIL
    GIT_USERNAME
- Args:
    project(require): type String; Gerrit project name
    version: type String; Value of the committing version; default is env.PROJECT_VERSION
    skip: type Boolean; If true to skip the current stage; default is false
*/
def CommitSnapshot(Map Args) {
    String stageName = 'CommitSnapshot'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    version: [value: Args['version'], type: 'string', require: false],
                    skip: [value: Args['skip'], type: 'bool', require: false]]
    String newVersion
    String comm
    String filePath
    def tempFile


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['version'] = Args.containsKey('version') ? Args['version'] : env.PROJECT_VERSION
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false


    stage('Git Commit Snapshot') {
        if(Args['skip']) {
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            dir(Args['project']) {
                println('Prepare for commit...')
                comm = """  git config --global user.email '${env.GIT_EMAIL}'
                          | git config --global user.name '${env.GIT_USERNAME}'""".stripMargin()
                sh(comm)

                println('Update Project version...')
                switch(Args['project']) {
                    case 'eric-eo-vnfm-helm-executor':
                        newVersion = upliftVersion( version: Args['version'],
                                                    flow: 'release-old',
                                                    snapshot: true,
                                                    this)
                        overwriteYaml(file: 'eric-product-info.yaml',
                                      key: 'version',
                                      value: newVersion,
                                      this)
                        comm = """  mvn versions:set \\
                                  | -DnewVersion=${newVersion} \\
                                  | -DgenerateBackupPoms=false""".stripMargin()
                        sh(comm)
                    break
                    case 'am-common-wfs':
                        newVersion = upliftVersion( version: Args['version'],
                                                    flow: 'base',
                                                    snapshot: true,
                                                    this)
                        comm = """  mvn versions:set \\
                                  | -DnewVersion=${newVersion} \\
                                  | -DgenerateBackupPoms=false""".stripMargin()
                        sh(comm)

                        filePath = 'charts/' + env.PROJECT_NAME + '/eric-product-info.yaml'
                        println('Set repository to snapshots in eric-product-info.yaml file...')
                        overwriteYaml(file: filePath,
                                      key: 'images.commonWfs.repoPath',
                                      value: 'proj-am/snapshots',
                                      this)
                        overwriteYaml(file: filePath,
                                      key: 'images.pgInitContainer.repoPath',
                                      value: 'proj-am/snapshots',
                                      this)
                    break
                    case 'am-common-wfs-ui':
                    case 'am-onboarding-service':
                    case 'am-package-manager':
                    case 'eric-eo-batch-manager':
                    case 'eric-eo-evnfm-crypto':
                    case 'eric-eo-evnfm-sol-agent':
                    case 'eric-eo-fh-event-to-alarm-adapter':
                    case 'eric-eo-lm-consumer':
                    case 'vnfm-orchestrator':
                        newVersion = upliftVersion( version: Args['version'],
                                                    flow: 'base',
                                                    snapshot: true,
                                                    this)

                        comm = """  mvn versions:set \\
                                  | -DnewVersion=${newVersion} \\
                                  | -DgenerateBackupPoms=false""".stripMargin()
                        sh(comm)

                        filePath = "charts/${env.PROJECT_NAME}/values.yaml"
                        if(fileExists(filePath)) {
                            println('Set repository to snapshots in values.yaml file...')
                            tempFile = readFile(file: filePath).trim()
                            tempFile = tempFile.replace('proj-am/releases', 'proj-am/snapshots')
                            writeFile(text: tempFile, file: filePath)
                        }

                        filePath = "charts/${env.PROJECT_NAME}/eric-product-info.yaml"
                        if(fileExists(filePath)) {
                            println('Set repository to snapshots in eric-product-info.yaml file...')
                            tempFile = readFile(file: filePath).trim()
                            tempFile = tempFile.replace('proj-am/releases', 'proj-am/snapshots')
                            writeFile(text: tempFile, file: filePath)
                        }
                    break
                    case 'gr-controller':
                        newVersion = upliftVersion( version: Args['version'],
                                                    flow: 'base',
                                                    snapshot: true,
                                                    this)

                        println('Update version in pom file...')
                        comm = """  mvn versions:set \\
                                  | --file ${env.POM_FILE } \\
                                  | -DnewVersion=${newVersion} \\
                                  | -DgenerateBackupPoms=false""".stripMargin()
                        sh(comm)
                    break
                    case 'am-cvnfm-utils':
                        newVersion = upliftVersion( version: Args['version'],
                                                    flow: 'base',
                                                    snapshot: true,
                                                    this)

                        println('INFO: Update version on eric-product-info.yaml...')
                        overwriteYaml(file: env.PRODUCT_INFO,
                                      key: 'version',
                                      value: newVersion,
                                      this)
                    break
                    default:
                        newVersion = upliftVersion( version: Args['version'],
                                                    flow: 'release-old',
                                                    snapshot: true,
                                                    this)
                        comm = """  mvn versions:set \\
                                  | -DnewVersion=${newVersion} \\
                                  | -DgenerateBackupPoms=false""".stripMargin()
                        sh(comm)

                        filePath = "charts/${env.PROJECT_NAME}/values.yaml"
                        if(fileExists(filePath)) {
                            println('Set repository to snapshots in values.yaml file...')
                            tempFile = readFile(file: filePath).trim()
                            tempFile = tempFile.replace('proj-am/releases', 'proj-am/snapshots')
                            writeFile(text: tempFile, file: filePath)
                        }

                        filePath = "charts/${env.PROJECT_NAME}/eric-product-info.yaml"
                        if(fileExists(filePath)) {
                            println('Set repository to snapshots in eric-product-info.yaml file...')
                            tempFile = readFile(file: filePath).trim()
                            tempFile = tempFile.replace('proj-am/releases', 'proj-am/snapshots')
                            writeFile(text: tempFile, file: filePath)
                        }
                    break
                }

                println('Commit updates...')
                comm = """  git status
                          | git diff
                          | git add ${env.GIT_COMMIT_FILES}
                          | git commit -m 'Committing SNAPSHOT version ${newVersion}'
                          | git log -3""".stripMargin()
                sh(comm)
            }
        }
    }
}


/* Stage for Push Changes to Git repository. Use:
- Job's ENVs:
    PROJECT_VERSION
- Args:
    project(require): type String; Gerrit project name
    branch(require): type String; Gerrit project branch
    version: type String; Value of the committing version; default is env.PROJECT_VERSION
*/
def PushChanges(Map Args) {
    String stageName = 'PushChanges'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    branch: [value: Args['branch'], type: 'string'],
                    version: [value: Args['version'], type: 'string', require: false]]
    String comm
    String gitUrl
    String credId


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['version'] = Args.containsKey('version') ? Args['version'] : env.PROJECT_VERSION


    stage('Push Changes') {
        dir(Args['project']) {
            println('Set gitUrl value...')
            comm = 'git config --get remote.origin.url'
            gitUrl = sh(script: comm, returnStdout: true).trim()
            println('Git URL: ' + gitUrl)

            println('Push changes to Git repository...')
            credId = getCredential(gitUrl)
            withCredentials([gitUsernamePassword( credentialsId: credId,
                                                  gitToolName: 'Default')]) {
                comm = """git status --long
                          |git push origin ${Args['branch']}
                          |git push origin ${Args['branch']} ${Args['version']}""".stripMargin()
                sh(comm)
            }
        }
    }
}


/* Stage for Create Change in Gerrit. Use:
- Job's ENVs:
    GIT_EMAIL
    GIT_USERNAME
- Jenkins variables:
    GERRIT_URL
- Args:
    project(require): type String; Gerrit project name
    files(require): type String; String list of files to commit, delimeter is " "
    message(require): type String; Message of commit
    branch: type String; branch of change; default is 'master'
    topic: type String; topic of change; default is 'jenkins'
    submit: type Boolean; if true set Submit-To-Pipeline +1; default is false
    version: type String; Version of uplifting service; default is ''
*/
def CreateChange(Map Args) {
    String stageName = 'CreateChange'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    files: [value: Args['files'], type: 'string'],
                    message: [value: Args['message'], type: 'string'],
                    branch: [value: Args['branch'], type: 'string', require: false],
                    topic: [value: Args['topic'], type: 'string', require: false],
                    submit: [value: Args['submit'], type: 'bool', require: false],
                    version: [value: Args['version'], type: 'string', require: false]]
    String comm
    String status
    String gitUrl
    String changeId
    Map labels = ['Code-Review': '+2',
                  'Submit-To-Pipeline': '+1']
    List<String> messTempl = new ArrayList<String>()
    def changes
    Boolean create = true
    String skipMess = 'INFO: _reason_. Create change steps will be skipped'
    String reason


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['branch'] = Args.containsKey('branch') ? Args['branch'] : 'master'
    Args['topic'] = Args.containsKey('topic') ? Args['topic'] : 'jenkins'
    Args['submit'] = Args.containsKey('submit') ? Args['submit'] : false
    Args['version'] = Args.containsKey('version') ? Args['version'] : ''


    stage('Create Change: ' + Args['project']) {
        dir(Args['project']) {
            println('Check git changes...')
            comm = 'git diff --exit-code --quiet && echo "absent" || echo "present"'
            status = sh(script: comm, returnStdout: true).trim()

            if(status == 'absent') {
                println('IFNO: Nothing to commit')
            } else {
                changes = listChanges(project: Args['project'],
                                      url: GERRIT_URL,
                                      topic: Args['topic'],
                                      this)

                changes.each {
                    if(Args['version']) {
                        String version = it['subject'].split(' ').last()

                        switch(compareVersion(version, Args['version'])) {
                            case 0:
                                create = false
                                changes = changes.minus(it)
                                reason = 'Change with the version ' + Args['version']
                                reason += ' is present in Gerrit'
                            break
                            case 1:
                                create = false
                                changes = changes.minus(it)
                                reason = 'Current version is lower than in existing Gerrit change(s)'
                            break
                        }
                    } else if(Args['message'] == it['subject']) {
                        create = false
                        changes = changes.minus(it)
                        reason = 'Change with the current message is present in Gerrit'
                    }
                }

                if(reason) {
                    skipMess = skipMess.replace('_reason_', reason)
                }

                if(changes.size() > 0) {
                    println('INFO: Abandon old changes...')
                    changes.each {
                        abandonChange(number: it['number'].toString(),
                                      url: GERRIT_URL,
                                      this)
                    }
                }

                if(create) {
                    println('Create Change...')
                    preCommit(username: env.GIT_USERNAME,
                              email: env.GIT_EMAIL,
                              hook: true,
                              this)

                    println('Set gitUrl value...')
                    comm = 'git config --get remote.origin.url'
                    gitUrl = sh(script: comm, returnStdout: true).trim()

                    withCredentials([gitUsernamePassword( credentialsId: getCredential(gitUrl),
                                                          gitToolName: 'Default')]) {
                        comm = """  git diff
                                  | git add ${Args['files']}
                                  | git commit -m "${Args['message']}"
                                  | git log -1
                                  | git push origin HEAD:refs/for/${Args['branch']}%topic=${Args['topic']}""".stripMargin()
                        sh(comm)
                    }

                    changeId = getChangeId(this)
                    println('Change-Id of new commit: ' + changeId)

                    if(Args['submit']) {
                        setChangeReview(number: changeId,
                                        url: GERRIT_URL,
                                        labels: labels,
                                        this)
                    }
                } else {
                    println(skipMess)
                }
            }
        }
    }
}


/* Stage for Check Gerrit Commit Message. Use:
- Args:
    message(require): type String; base64 hash of the commit message
*/
def CheckCommit(Map Args) {
    String stageName = 'CheckCommit'
    Map argsList = [message: [value: Args['message'], type: 'string']]
    def prohibited = ["`", "'", "\"",  "\$"]
    def messagePattern = ~'^((SM-\\d+)|(EO-\\d+)|(IDUN-\\d+)|NO\\sJIRA|(JENKINS-\\d+)).*\\s-\\s.*$'
    String charError = 'ERROR: Commit message contains one of the special characters which are prohibited: ${prohibited}. Please, update the commit message'
    String styleError = 'ERROR: Commit message should start with ticketnumber or "NO JIRA" or "JENKINS-" followed by space and dash(e.g. EO-123321 - CommitText)'
    String successMessage = 'INFO: Commit message is fine'
    String comm
    String decodedCommit


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Check Commit Message') {
        if(checkNotBlank(Args['message'])) {
            println('Decode Commit message...')
            comm = "echo ${Args['message']} | base64 -d "
            decodedCommit = sh(script: comm, returnStdout: true).trim()

            println('Check wrong symbol in the Commit message...')
            prohibited.any {
                if(decodedCommit.contains(it)) {
                    error(message: charError)
                }
            }

            println('Check Commit message style...')
            if(!messagePattern.matcher(Args['subject']).matches()) {
                error(message: styleError)
            }

            println(successMessage)
        } else {
            println('INFO: Commit message is empty')
        }
    }
}


/* Stage for Submit Change. Use:
- Args:
    number(require): type String; Gerrit change number
    url(require): type String; Gerrit URL
    skip: type Boolean; If true to skip the current stage; default is false
*/
def SubmitChange(Map Args) {
    String stageName = 'SubmitChange'
    Map argsList = [number: [value: Args['number'], type: 'string'],
                    url: [value: Args['url'], type: 'string'],
                    skip: [value: Args['skip'], type: 'bool', require: false]]
    String dockerImage = getDockerImagePath('adp-int')
    String comm
    String changeId
    Boolean submitStatus


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false


    stage('Submit') {
        if(Args['skip']) {
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            credId = getCredential(Args['url'])
            withCredentials([usernamePassword(credentialsId: credId,
                                              usernameVariable: 'GERRIT_USER',
                                              passwordVariable: 'GERRIT_PASSWORD')]) {
                println('Check submittable...')
                comm = """docker run --init --rm \\
                          | ${dockerImage} \\
                          | gerrit submittable \\
                          | --debug \\
                          | --username \$GERRIT_USER \\
                          | --password \$GERRIT_PASSWORD \\
                          | --change ${Args['number']}""".stripMargin()
                submitStatus = sh(script: comm, returnStatus: true).toInteger() == 0 ? true : false

                if(!submitStatus) {
                    println('ERROR: The PatchSet is not submittable')
                    sh('exit 1')
                }

                println('Submit PatchSet...')
                comm = """docker run --init --rm \\
                          | ${dockerImage} \\
                          | gerrit submit \\
                          | --debug \\
                          | --username \$GERRIT_USER \\
                          | --password \$GERRIT_PASSWORD \\
                          | --change ${Args['number']}""".stripMargin()
                sh(comm)
            }
        }
    }
}


/* Stage for Check Submit Status for change. Use:
- Args:
    number(require): type String; Gerrit change number
    url(require): type String; Gerrit URL
*/
def CheckSubmitStatus(Map Args) {
    stage('Check Submit Status') {
        String stageName = 'CheckSubmitStatus'
        Map argsList = [number: [value: Args['number'], type: 'string'],
                        url: [value: Args['url'], type: 'string']]
        String dockerImage = getDockerImagePath('adp-int')
        String comm
        Boolean submitStatus


        // Checking Arguments
        checkArgs(argsList, stageName, this)


        submitStatus = isSubmittable( number: Args['number'],
                                      url: Args['url'],
                                      this)
        if(submitStatus) {
            println('INFO: The PatchSet is SUBMITTABLE')
        } else {
            println('ERROR: The PatchSet is not submittable')
            sh('exit 1')
        }
    }
}


/* Stage to lock/unlock branch. Use:
- Args:
    project(require): type String; Gerrit change number
    url(require): type String; Gerrit URL
    group(require): type String; Gerrit Group ID
    branch(require): type String; Gerrit branch
    action(require): type String; Lock or Unlock
*/
def LockBranch(Map Args) {
    String stageName = 'LockBranch'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    url: [value: Args['url'], type: 'string'],
                    branch: [value: Args['branch'], type: 'string'],
                    group: [value: Args['group'], type: 'string'],
                    action: [value: Args['action'], type: 'string']]
    String output


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Change ACL') {
        println('INFO: Get ACL')
        output = restCall(url: Args['url'],
                          endpoint: urlEncode(Args['project']) + '/access',
                          this)['output']
        println(output)

        if(Args['action'].toLowerCase() == 'lock') {
            println('INFO: Lock ACL')
        } else {
            println('INFO: Unlock ACL')
        }

        lockBranch(project: Args['project'],
                   group: Args['group'],
                   branch: Args['branch'],
                   action: Args['action'].toLowerCase(),
                   url: Args['url'],
                   this)
    }
}


/* Stage for Create and Push new branch to Git repository. Use:
- Args:
    project(require): type String; Gerrit project name
    branch(require): type String; Gerrit project branch
    skip: type Boolean; if true to skip the current stage; default is false
*/
def CreateBranch(Map Args) {
    String stageName = 'CreateBranch'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    branch: [value: Args['branch'], type: 'string'],
                    skip: [value: Args['skip'], type: 'bool', require: false]]
    String comm
    String gitUrl
    String credId


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false


    stage('Create Branch: ' + Args['project']) {
        if(Args['skip']) {
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            dir(Args['project']) {
                println('Set gitUrl value...')
                comm = 'git config --get remote.origin.url'
                gitUrl = sh(script: comm, returnStdout: true).trim()
                println('Git URL: ' + gitUrl)

                println('Create and push branch to Git repository...')
                credId = getCredential(gitUrl)
                withCredentials([gitUsernamePassword( credentialsId: credId,
                                                      gitToolName: 'Default')]) {
                    comm = """  git checkout -b ${Args['branch']}
                              | git push --set-upstream origin ${Args['branch']}""".stripMargin()
                    sh(comm)
                }
            }
        }
    }
}


/* Stage for Delete branch from Git repository. Use:
- Args:
    project(require): type String; Gerrit project name
    branch(require): type String; Gerrit project branch
*/
def DeleteBranch(Map Args) {
    String stageName = 'CreateBranch'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    branch: [value: Args['branch'], type: 'string']]
    String comm
    String gitUrl
    String credId


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Delete Branch ' + Args['branch'] + ': ' + Args['project']) {

        dir(Args['project']) {
             println('Set gitUrl value...')
             comm = 'git config --get remote.origin.url'
             gitUrl = sh(script: comm, returnStdout: true).trim()
             println('Git URL: ' + gitUrl)

             println('Delete branch localy and from Git repository...')
             credId = getCredential(gitUrl)
             withCredentials([gitUsernamePassword( credentialsId: credId,
                        gitToolName: 'Default')]) {
                 comm = """ git push origin --delete ${Args['branch']} """.stripMargin()
                 sh(comm)
            }
        }
    }
}


/* Stage for Testing SSH. Use:
- Args:
    master(require): type String; Gerrit master URL
    sero: type String; Gerrit sero URL
    seli: type String; Gerrit seli URL
*/
def TestAccessSSH(Map Args) {
    String stageName = 'TestAccessSSH'
    Map argsList = [master: [value: Args['master'], type: 'string'],
                    sero: [value: Args['sero'], type: 'string', require: false],
                    seli: [value: Args['seli'], type: 'string', require: false]]
    String comm


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['master'] = Args.containsKey('master') ? Args['master'] : 'gerrit.ericsson.se'
    Args['sero'] = Args.containsKey('sero') ? Args['sero'] : 'gerritmirror-direct.sero.gic.ericsson.se'
    Args['seli'] = Args.containsKey('seli') ? Args['seli'] : 'gerritmirror-ha.lmera.ericsson.se'


    stage('TestAccessSSH') {
        withCredentials([sshUserPrivateKey(credentialsId: 'amadm100-gerrit',
                                           usernameVariable: 'SSH_USERNAME',
                                           keyFileVariable: 'SSH_PRIVATE_KEY')]) {
            sshagent(credentials: ['amadm100-gerrit']) {

            println('Test SSH...')
            comm = """ set -o errexit
                     | set -o nounset
                     | set -o xtrace
                     | ssh $SSH_USERNAME@${Args['master']} -p 29418 || true
                     | ssh $SSH_USERNAME@${Args['sero']} -p 29418 || true
                     | ssh $SSH_USERNAME@${Args['seli']} -p 29418 || true""".stripMargin()
            sh(comm)
            }
        }
    }
}


/* Stage for Testing synchronizing repositories. Use:
- Args:
    master(require): type String; Gerrit master URL
    sero: type String; Gerrit sero URL
    seli: type String; Gerrit seli URL
*/
def CheckMirrorSyncTest(Map Args) {
    String stageName = 'CheckMirrorSyncTest'
    Map argsList = [master: [value: Args['master'], type: 'string'],
                    sero: [value: Args['sero'], type: 'string']]
    String comm
    def repos
    def reportTable
    def masterURL
    def mirrorURL
    def masterRevision
    def mirrorRevision
    def syncStatus
    def projectGroup
    def tableContent


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['master'] = Args.containsKey('master') ? Args['master'] : 'gerrit.ericsson.se'
    Args['sero'] = Args.containsKey('sero') ? Args['sero'] : 'gerritmirror-direct.sero.gic.ericsson.se'

     // List of Gerrit repositories
     repos = ['am-ci-flow', 'am-common-wfs','am-common-wfs-ui', 'am-onboarding-service',
              'am-package-manager', 'am-sandbox', 'am-shared-utilities', 'eric-eo-batch-manager', 'cvnfm-enm-cli-stub',
              'am-cvnfm-utils', 'am-integration-charts', 'eric-eo-evnfm-sol-agent', 'eric-eo-evnfm-crypto',
              'eric-eo-evnfm-library-chart', 'eric-eo-lm-consumer', 'eric-oss-function-orchestration-common',
              'eric-eo-evnfm-library-chart', 'eric-eo-lm-consumer', 'eric-oss-function-orchestration-common',
              'eric-eo-fh-event-to-alarm-adapter', 'evnfm-rbac', 'gr-controller', 'jenkins-shared-libs', 'master',
              'eric-eo-signature-validation-lib', 'eric-eo-vnfm-helm-executor', 'vnfm-orchestrator', 'vnfsdk-pkgtools' ]


    stage('CheckMirrorSyncTest') {

          println('Initialize the table for the call')
          reportTable = [:]

          // Change the synchronization for the skin repository
          for (gerritRepo in repos) {

                projectGroup = getProjectGroup(gerritRepo)
                masterURL = 'https://' + Args['master'] + '/a' + '/' + projectGroup + '/' + gerritRepo
                mirrorURL = 'https://' + Args['sero'] + '/a' + '/' + projectGroup + '/' + gerritRepo

                withCredentials([gitUsernamePassword( credentialsId: getCredential(masterURL),
                            gitToolName: 'Default')]) {
                    comm = "git ls-remote -q $masterURL HEAD"
                    masterRevision = sh(script: comm, returnStdout: true).trim()
                    }

                withCredentials([gitUsernamePassword( credentialsId: getCredential(mirrorURL),
                            gitToolName: 'Default')]) {

                     comm = "git ls-remote -q $mirrorURL HEAD"
                     mirrorRevision = sh(script: comm, returnStdout: true).trim()
                    }

                syncStatus = (masterRevision == mirrorRevision) ? "Synchronized" : "Not synchronized"

                reportTable[gerritRepo] = [
                            "Master Revision": masterRevision,
                            "Mirror Revision": mirrorRevision,
                            "Synchronization status": syncStatus
                    ]
                }

                println('Show synchronizing gerrit repositories...')
                tableContent += "'The name of the repository' " +
                                "| 'Master Revision' " +
                                "| 'Mirror Revision' " +
                                "| 'Synchronization status' |\n"

                reportTable.each { gerritRepo, repoData ->
                    tableContent +=  "| ${gerritRepo} " +
                                     "| ${repoData['Master Revision']} " +
                                     "| ${repoData['Mirror Revision']} " +
                                     "| ${repoData['Synchronization status']} |\n"
                }
                println(tableContent)
          }
}


/* Function to switch Gerrit server. Use:
- Args:
    project(require): type String; The name of the project
    environment(require): type String; The value of the Environment URL
*/
def WithGerritHttpUrl(Map Args, Closure body) {
    String stageName = 'WithGerritHttpUrl'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    environment: [value: Args['environment'], type: 'string']]
    String comm


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    withEnv(['GERRIT_HTTP_URL=https://' + Args['environment'] + '/a']) {
        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
            body()
        }

        // Clear project directory
        comm = 'sudo rm -rf ' + Args['project']
        sh(comm)
    }
}


/* Stage to delete the last commit. Use:
- Args:
    project(require): type String; project name
    branch(require): type String; branch name
*/
def DeleteLastCommit(Map Args) {
    String stageName = 'abandonLastCommit'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    branch: [value: Args['branch'], type: 'string']]
    String comm
    String projectGroup
    String url
    String commitHash
    String currentBranch
    String branchExists


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['project'] = Args.containsKey('project') ? Args['project'] : 'am-ci-flow'
    Args['branch'] = Args.containsKey('branch') ? Args['branch'] : 'servicetest'


    stage('DeleteLastCommit') {
        dir(Args['project']) {

            projectGroup = getProjectGroup(Args['project'])
            url = GERRIT_HTTP_URL + '/' + projectGroup + '/' + Args['project']
            withCredentials([gitUsernamePassword( credentialsId: getCredential(url),
                    gitToolName: 'Default')]) {

                println('Get current branch...')
                comm = 'git rev-parse --abbrev-ref HEAD'
                currentBranch = sh(script: comm, returnStdout: true).trim()

                println('If current branch is ' + Args['branch'])
                if (!currentBranch.contains(Args['branch'])) {

                    comm = "git ls-remote origin ${currentBranch}"
                    branchExists = sh(script: comm, returnStdout: true).trim()

                    if (!branchExists) {
                        println("There is no ${Args['branch']} branch available...")
                    } else {
                        comm = "git checkout ${Args['branch']}"
                        sh(comm)
                    }
                }

                println('Get last commit Hash...')
                comm = 'git rev-parse HEAD'
                commitHash = sh(script: comm, returnStdout: true).trim()

                println('Execute the command git revert...')
                comm = "git revert ${commitHash}"
                sh(comm)

                println('Delete temporary commit...')
                comm = "git reset --hard HEAD~1"
                sh(comm)

                println('Push local changes to Gerrit server...')
                comm = "git push origin ${Args['branch']} --force-with-lease"
                sh(comm)
            }
        }
    }
}


/* Stage to add and delete tag to the commit. Use:
- Args:
    project(require): type String; project name
    branch(require): type String; branch name
*/
def TagTestStage(Map Args) {
    String stageName = 'abandonLastCommit'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    branch: [value: Args['branch'], type: 'string']]
    String comm
    String projectGroup
    String url
    String commitHash
    String currentBranch
    String branchExists


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['project'] = Args.containsKey('project') ? Args['project'] : 'am-ci-flow'
    Args['branch'] = Args.containsKey('branch') ? Args['branch'] : 'servicetest'


    stage('TagTestStage') {
        dir(Args['project']) {

            projectGroup = getProjectGroup(Args['project'])
            url = GERRIT_HTTP_URL + '/' + projectGroup + '/' + Args['project']
            withCredentials([gitUsernamePassword( credentialsId: getCredential(url),
                    gitToolName: 'Default')]) {

                println('Get current branch...')
                comm = 'git rev-parse --abbrev-ref HEAD'
                currentBranch = sh(script: comm, returnStdout: true).trim()

                println('If current branch is ' + Args['branch'])
                if (!currentBranch.contains(Args['branch'])) {

                    comm = "git ls-remote origin ${currentBranch}"
                    branchExists = sh(script: comm, returnStdout: true).trim()

                    if (!branchExists) {
                        println("There is no ${Args['branch']} branch available...")
                    } else {
                        comm = "git checkout ${Args['branch']}"
                        sh(comm)
                    }
                }

                println('Get last commit Hash...')
                comm = 'git rev-parse HEAD'
                commitHash = sh(script: comm, returnStdout: true).trim()

                println('Add test tag...')
                comm = """git tag v1.0 ${commitHash} -m "Gerrit test tag" """
                sh(comm)

                println('Push tag...')
                comm = "git push origin v1.0"
                sh(comm)

                println('Delete locally tag...')
                comm = "git tag -d v1.0"
                sh(comm)

                println('Push local changes to Gerrit server...')
                comm = "git push origin --delete v1.0"
                sh(comm)
            }
        }
    }
}

return this