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
import static com.ericsson.orchestration.mgmt.libs.VnfmArtifact.uploadToNexus
import static com.ericsson.orchestration.mgmt.libs.VnfmDocker.getDockerImagePath
import static com.ericsson.orchestration.mgmt.libs.VnfmDocker.pullDockerImage
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkArgs
import static com.ericsson.orchestration.mgmt.libs.VnfmJenkins.getCredential
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkNotBlank


/* Stage is for Build Maven project. Use:
- Job's ENVs:
    JOB_TYPE
- Args:
    project(require): type String; Name of the project
    pom: type String; Path to the pom file; default is 'pom.xml'
    stage: type String; Stage custom name; default is 'Build'
*/
def BuildMaven(Map Args) {
    String stageName = 'BuildMaven'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    pom: [value: Args['pom'], type: 'list', require: false],
                    stage: [value: Args['stage'], type: 'string', require: false]]
    String comm
    String filePath


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['pom'] = Args.containsKey('pom') ? Args['pom'] : 'pom.xml'
    Args['stage'] = Args.containsKey('stage') ? Args['stage'] : 'Build'


    stage(Args['stage']) {
        dir(Args['project']) {
            switch(Args['project']) {
                case 'am-integration-charts':
                    /*- Use VARs:
                          HELM_MIGRATION_TOOL_DIR
                    */
                    if(fileExists(env.HELM_MIGRATION_TOOL_DIR)) {
                        dir(env.HELM_MIGRATION_TOOL_DIR) {
                            println('INFO: Build Helm Migration Tool project...')
                            comm = 'mvn clean install -ntp -DskipTests'
                            sh(comm)
                        }
                    } else {
                        println('INFO: Nothing to build')
                    }
                break
                case 'eric-eo-batch-manager':
                case 'eric-eo-lm-consumer':
                case 'eric-eo-signature-validation-lib':
                case 'vnfm-orchestrator':
                    println('Build project...')
                    comm = 'mvn clean install -ntp -DskipTests'
                    sh(comm)
                break
                case 'am-package-manager':
                    println('Package vnfsdk-pkgtools...')
                    dir('vnfsdk-pkgtools') {
                        comm = '''  python --version
                                  | pip --version
                                  | pip install -r requirements.txt
                                  | mvn package'''.stripMargin()
                        sh(comm)
                    }

                    println('Check path of the vnfsdk-pkgtools package file...')
                    comm = "ls vnfsdk-pkgtools/dist/vnfsdk-*.whl"
                    filePath = sh(script: comm, returnStdout: true).trim()
                    filePath = filePath.minus('vnfsdk-pkgtools/dist/')

                    println('Copy vnfsdk-pkgtools package file...')
                    comm = "cp vnfsdk-pkgtools/dist/${filePath} ${filePath}"
                    sh(comm)

                    println('Remove vnfsdk-pkgtools directory...')
                    comm = 'rm -rf vnfsdk-pkgtools'
                    sh(comm)

                    println('Install dependancy...')
                    comm = '''  python --version
                              | pip --version
                              | pip install -r requirements.txt'''.stripMargin()
                    sh(comm)

                    println('Build project...')
                    comm = """mvn clean package -ntp \\
                              | -DskipTests \\
                              | -Dvnfsdk-path=./${filePath}""".stripMargin()
                    sh(comm)
                break
                case 'vnfsdk-pkgtools':
                    println('Install dependancy...')
                    comm = '''  python --version
                              | pip --version
                              | pip install -r requirements.txt'''.stripMargin()
                    sh(comm)

                    println('Build project...')
                    comm = 'mvn clean package -ntp -DskipTests'
                    sh(comm)
                break
                case 'gr-controller':
                    println('Build project...')
                    withCredentials([usernamePassword(credentialsId: getCredential('maven-gr'),
                                                      usernameVariable: 'MVN_USER',
                                                      passwordVariable: 'MVN_PASSWORD')]) {
                        comm = """mvn -B \\
                                  | --file ${Args['pom']} \\
                                  | -s settings.xml \\
                                  | clean package \\
                                  | -Dmaven.test.skip=true \\
                                  | -Dmvn.username=\$MVN_USER \\
                                  | -Dmvn.password=\$MVN_PASSWORD""".stripMargin()
                        sh(comm)
                    }
                break
                default:
                    println('Build project...')
                    comm = 'mvn clean install -ntp -DskipTests'
                    comm = env.JOB_TYPE == 'gerrit' ? comm + ' -DskipAssembly=true' : comm
                    sh(comm)
                break
            }
        }
    }
}


/* Stage is for Update POM files. Use:
- VARs:
    POM_FILE(for gr-controller)
- Job's ENVs:
    PROJECT_VERSION
*/
def UpdatePOM(String project) {
    stage('Update POM files') {
        dir(project) {
            String comm


            switch(project) {
                case 'gr-controller':
                    comm = """mvn versions:set -q \\
                              | --file ${env.POM_FILE} \\
                              | -DnewVersion=${env.PROJECT_VERSION} \\
                              | -DgenerateBackupPoms=false""".stripMargin()
                break
                default:
                    comm = """mvn versions:set -q \\
                              | -DnewVersion=${env.PROJECT_VERSION} \\
                              | -DgenerateBackupPoms=false""".stripMargin()
                break
            }

            println('Update files...')
            sh(comm)
        }
    }
}


/* Stage is for Update POM files with project dependency. Use:
- Job's ENVs:
    PROJECT_VERSION
- Args:
    project(require): type String; Name of the project
    property(require): type String; Name of the property
    value(require): type String; Value of the property
*/
def UpdatePOMProperty(Map Args) {
    String stageName = 'UpdatePOMProperty'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    property: [value: Args['property'], type: 'string'],
                    value: [value: Args['value'], type: 'string']]
    String comm


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Update POM files') {
        dir(Args['project']) {
            switch(Args['property']) {
                case 'all':
                    comm = """mvn versions:set -q \\
                              | -DnewVersion=${Args['value']} \\
                              | -DgenerateBackupPoms=false""".stripMargin()
                break
                default:
                    comm = """mvn versions:set-property -q \\
                              | -Dproperty=${Args['property']} \\
                              | -DnewVersion=${Args['value']}""".stripMargin()
                break
            }

            println('Update files...')
            sh(comm)
        }
    }
}


/* Stage is for Release Artifact
*/
def ReleaseArtifact(String project) {
    stage('Release Artifact') {
        dir(project) {
            String comm


            println('INFO: Release artifacts...')
            switch(project) {
                case 'eric-eo-evnfm-sol-agent':
                    comm = 'mvn clean deploy'
                    sh(comm)
                break
                case 'am-cvnfm-utils':
                    /*- Use VARs:
                          SOURCE_FOLDER
                          NEXUS_PATH
                          NEXUS_URL
                      - Job's ENVs:
                          PROJECT_NAME
                          PROJECT_VERSION
                    */
                    // Upload binary to Nexus
                    String uploadPath = env.NEXUS_PATH + '/' + env.PROJECT_VERSION
                    uploadToNexus(name: env.PROJECT_NAME,
                                  dir: env.SOURCE_FOLDER,
                                  url: env.NEXUS_URL,
                                  path: uploadPath,
                                  type: 'binary',
                                  this)
                break
                default:
                    comm = 'mvn clean deploy -DskipTests -ntp'
                    sh(comm)
                break
            }
        }
    }
}


/* Stage is for build NodeJS project. Use:
- Job's ENVs:
    PROJECT_NAME
*/
def BuildNodeJS(String project) {
    stage('Build project') {
        dir(project) {
            String comm
            String dockerImage = getDockerImagePath('node-chrome')


            println('Install project...')
            comm = """docker run --init --rm \\
                      | --user root:\$(id -g) \\
                      | --workdir /app/${env.PROJECT_NAME}-gui \\
                      | --volume ${WORKSPACE}/${project}:/app \\
                      | ${dockerImage} \\
                      | npm install""".stripMargin()
            sh(comm)

            println('Build UI content...')
            comm = """docker run --init --rm \\
                      | --user root:\$(id -g) \\
                      | --workdir /app/${env.PROJECT_NAME}-gui \\
                      | --volume ${WORKSPACE}/${project}:/app \\
                      | ${dockerImage} \\
                      | npm run build""".stripMargin()
            sh(comm)

            println('Build UI service...')
            comm = 'mvn -ntp clean install -DskipTests -P ui-service'
            sh(comm)
        }
    }
}


/* Stage is for Build CSAR. Use:
- Args:
    dir(require): type String; Build directory
    type: type String; Type of the building csar; default is 'tests'
    chart: type String; Chart of the building csar; default is 'eric-eo-helmfile'
    version: type String; Version of the building chart; default is '2.20.0-177'
    vnfddir: type String; Directory of vnfd file to build csar; default is ''
    noImages: type bool; build CSARs with images; default is true
*/
def BuildCSAR(Map Args) {
    String stageName = 'BuildCSAR'
    Map argsList = [dir: [value: Args['dir'], type: 'string'],
                    type: [value: Args['type'], type: 'string', require: false],
                    chart: [value: Args['chart'], type: 'string', require: false],
                    version: [value: Args['version'], type: 'string', require: false],
                    vnfddir: [value: Args['vnfddir'], type: 'string', require: false],
                    noImages: [value: Args['noImages'], type: 'bool', require: false]]
    String comm
    String dockerImage
    String credId


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['type'] = Args.containsKey('type') ? Args['type'] : 'tests'
    Args['chart'] = Args.containsKey('chart') ? Args['chart'] : 'eric-eo-helmfile'
    Args['version'] = Args.containsKey('version') ? Args['version'] : '2.20.0-177'
    Args['vnfddir'] = Args.containsKey('vnfddir') ? Args['vnfddir'] : ''
    Args['noImages'] = Args.containsKey('noImages') ? Args['noImages'] : true


    stage('Build CSAR') {
        dir(Args['dir']){
            switch(Args['type']) {
                case 'tests':
                    if(checkNotBlank(Args['vnfddir'])) {
                        files = findFiles(glob: Args['vnfddir'] + '**/changelogs/ChangeLog.*.txt')
                    } else {
                        files = findFiles(glob: 'csars/**/changelogs/ChangeLog.*.txt')
                        Args['noImages'] = true
                    }

                    for(def f in files) {
                        String vnfdPath = (f.path).minus('changelogs/ChangeLog.')
                                                  .replace('.txt', '.yaml')
                        println('INFO: Vnfd Name to build CSAR is ' + f.path.minus('csars/'))

                        withCredentials([usernamePassword(credentialsId: getCredential('ldap'),
                                                          usernameVariable: 'NEXUS_USER',
                                                          passwordVariable: 'NEXUS_PASSWORD')]){
                            comm = """scripts/package_csar.py build \\
                                      | --vnfd-path=${vnfdPath} \\
                                      | --login=\$NEXUS_USER \\
                                      | --password=\$NEXUS_PASSWORD""".stripMargin()

                            if(Args['noImages']) {
                                comm += ' --no-images'
                            }

                            println('INFO: Build default CSAR package...')
                            sh(comm)
                        }
                    }
                break
                case 'eric-eo-helmfile':
                    String repositories = Args['chart'] + '/' + 'repositories.yaml'
                    String siteValues = Args['chart'] + '/' + 'site_values.yaml'
                    String helmfile = Args['chart'] + '/' + 'helmfile.yaml'
                    String csarName = Args['chart'] + '-' + Args['version']

                    println('INFO: Pull Docker image ci-scripts...')
                    dockerImage = getDockerImagePath('ci-scripts')
                    pullDockerImage(name: dockerImage.split(':')[0],
                                    tag: dockerImage.split(':')[1],
                                    this)

                    credID = getCredential('ldap')
                    withCredentials([usernamePassword(credentialsId: credID,
                                                      usernameVariable: 'GERRIT_USERNAME',
                                                      passwordVariable: 'GERRIT_PASSWORD')]) {
                        println('INFO: Run prepare steps...')
                        comm = """tar -zxvf ${csarName}.tgz
                                  |touch ${siteValues}
                                  |sed -i "s|{{ env \\"GERRIT_USERNAME\\" }}|\$GERRIT_USERNAME|" ${repositories}
                                  |sed -i "s|{{ env \\"GERRIT_PASSWORD\\" }}|\$GERRIT_PASSWORD|" ${repositories}""".stripMargin()
                        sh(comm)

                        println('INFO: Get release details from eric-eo-helmfile...')
                        comm = """docker run --init --rm \\
                                  | --volume ${WORKSPACE}:/app:rw \\
                                  | --workdir /app \\
                                  | --user \$(id -u):\$(id -g) \\
                                  | ${dockerImage} \\
                                  | script_executor get-release-details-from-helmfile \\
                                  | --state-values-file /app/${siteValues} \\
                                  | --path-to-helmfile /app/${helmfile} \\
                                  | --get-all-images 'false' \\
                                  | --fetch-charts 'true'""".stripMargin()
                        sh(comm)

                        println('INFO: Pull Docker image am-package-manager...')
                        dockerImage = getDockerImagePath('am-package-manager')
                        pullDockerImage(name: dockerImage.split(':')[0],
                                        tag: dockerImage.split(':')[1],
                                        this)

                        println('INFO: Build eo-helmfile CSAR package...')
                        def prop = readFile(file: 'am_package_manager.properties').readLines()
                        for(def p in prop) {
                            String csar_name_version = p.split('=')[0]
                            String csar_chart_content_list = p.replaceAll(',', ' ').split('=')[1]

                            println('INFO: List CSAR chart content...')
                            println(csar_chart_content_list)

                            println('INFO: Build imageless CSAR ' + csar_name_version)
                            comm = """docker run --init --rm \\
                                      | --volume ${WORKSPACE}:/app:rw \\
                                      | --workdir /app \\
                                      | --user \$(id -u):\$(id -g) \\
                                      | ${dockerImage} \\
                                      | generate \\
                                      | --helm ${csar_chart_content_list} \\
                                      | --sol-version 3.3.1 \\
                                      | --name ${csar_name_version} \\
                                      | --no-images""".stripMargin()
                            sh(comm)
                        }
                    }
                break
                case 'simple':
                    String csarName = Args['chart'] + '-' + Args['version']

                    println('INFO: Pull Docker image am-package-manager...')
                    dockerImage = getDockerImagePath('am-package-manager')
                    pullDockerImage(name: dockerImage.split(':')[0],
                                    tag: dockerImage.split(':')[1],
                                    this)

                    println('INFO: Build filename CSAR package...')
                    comm = """docker run --init --rm \\
                              | --volume ${WORKSPACE}:/app:rw \\
                              | --workdir /app \\
                              | --user \$(id -u):\$(id -g) \\
                              | ${dockerImage} \\
                              | generate \\
                              | --helm ${csarName}.tgz \\
                              | --sol-version 3.3.1 \\
                              | --name ${csarName} \\
                              | --no-images""".stripMargin()
                    sh(comm)
                break
            }
        }
    }
}


/* Stage is for Build Golang. Use:
- Args:
    project(require): type String; Name of the project
    source_folder(require): type String; Path to src folder
*/
def GolangBuild(Map Args) {
    String stageName = 'Build'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    source_folder: [value: Args['source_folder'], type: 'string']]
    String comm
    String dockerImage = 'armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-gobuilder.adp-base-os:4.82.0'


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Build') {
        println('Build binary...')
        comm = """docker run --init --rm \\
                  | --volume ${WORKSPACE}/${Args['project']}:/app \\
                  | --workdir /app/${Args['source_folder']} \\
                  | --user :\$(id -g) \\
                  | ${dockerImage} \\
                  | sh -c \\
                  | 'go build'""".stripMargin()
        sh(comm)
    }
}


/* Stage is for Upload CSAR. Use:
- Args:
    dir(require): type String; Build directory
    type: type String; Type of the building csar; default is 'tests'
    vnfddir: type String; Directory of vnfd file to upload csar; default is ''
*/
def UploadCSARCNF(Map Args) {
    String stageName = 'UploadCSARCNF'
    Map argsList = [dir: [value: Args['dir'], type: 'string'],
                    type: [value: Args['type'], type: 'string', require: false],
                    vnfddir: [value: Args['vnfddir'], type: 'string', require: false]]
    String comm
    String dockerImage
    String credId


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['type'] = Args.containsKey('type') ? Args['type'] : 'tests'
    Args['vnfddir'] = Args.containsKey('vnfddir') ? Args['vnfddir'] : ''


    stage('Upload CSAR') {
        dir(Args['dir']){
            switch(Args['type']) {
                case 'tests':
                    if(Args['vnfddir']){
                        files = findFiles(glob: Args['vnfddir'] + '**/changelogs/ChangeLog.*.txt')
                    } else {
                        files = findFiles(glob: 'csars/**/changelogs/ChangeLog.*.txt')
                    }

                    for(def i in files) {
                        String vnfdPath = (i.path).replace("changelogs/ChangeLog.", "").replace(".txt", ".yaml")
                        println('Vnfd Name to build CSAR: ' + i.path.minus('csars/'))

                        withCredentials([usernamePassword(credentialsId: getCredential('ldap'),
                                                          usernameVariable: 'NEXUS_USER',
                                                          passwordVariable: 'NEXUS_PASSWORD')]){
                            println('Upload CSAR package without images...')
                            comm = """scripts/package_csar.py upload \\
                                     | --vnfd-path=${vnfdPath} \\
                                     | --login=\$NEXUS_USER \\
                                     | --password=\$NEXUS_PASSWORD \\
                                     | --no-images""".stripMargin()
                            sh(comm)
                        }
                    }
                break
            }
        }
    }
}

return this