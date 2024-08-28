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
import static Boolean.parseBoolean
import static org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional
import static com.ericsson.orchestration.mgmt.libs.VnfmArtifact.getArtifactProperty
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkNotBlank
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkArgs
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.lineToList
import static com.ericsson.orchestration.mgmt.libs.VnfmGerrit.getUmbrellaProject
import static com.ericsson.orchestration.mgmt.libs.VnfmGerrit.getContent
import static com.ericsson.orchestration.mgmt.libs.VnfmGit.getCommitInfo
import static com.ericsson.orchestration.mgmt.libs.VnfmGit.getChartPath
import static com.ericsson.orchestration.mgmt.libs.VnfmHelm.getChartParam
import static com.ericsson.orchestration.mgmt.libs.VnfmHelm.getChartRepo
import static com.ericsson.orchestration.mgmt.libs.VnfmHelm.getStableHelmfileVersion
import static com.ericsson.orchestration.mgmt.libs.VnfmHelm.getHelmfileChart
import static com.ericsson.orchestration.mgmt.libs.VnfmJenkins.lockEnvironment
import static com.ericsson.orchestration.mgmt.libs.VnfmJenkins.unlockResource
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.getClusterName
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.getClusterURL
import static com.ericsson.orchestration.mgmt.libs.VnfmUtils.upliftVersion
import static com.ericsson.orchestration.mgmt.libs.VnfmUtils.getVersionCBO
import static com.ericsson.orchestration.mgmt.libs.VnfmUtils.getAppInfo


/* Stage for Update E-VNFM Build ENVs. Use:
- Vars:
    INTEGRATION_CI_CONFIG_DIR
- Job's ENVs:
    JOB_TYPE
*/
def RunUpdateEVNFM(String project, String envName='') {
    stage('Update ENVs') {
        dir(project) {
            String text
            String configFile = env.INTEGRATION_CI_CONFIG_DIR + "/${envName}.conf"
            Map envsList


            switch(env.JOB_TYPE) {
                case ['deployment', 'values-test']:
                    /*- Use Vars:
                          PATH_TO_ENM_SITE_OVERRIDE_FILE
                          PATH_TO_DRAC_SITE_OVERRIDE_FILE
                          GR_BASE_OVERRIDE_FILE
                      - Job's ENVs:
                          RESOURCE_NAME
                          INSTALL_ENM_STUB
                          DRAC_ENABLE
                          GR_ENABLE
                          HOSTNAME_TYPE
                          GAS_HOSTNAME
                          HELM_REGISTRY_HOSTNAME
                          VNFM_HOSTNAME
                          VNFM_REGISTRY_HOSTNAME
                          GLOBAL_VNFM_REGISTRY_HOSTNAME
                          GR_SECONDARY_HOSTNAME
                          INSTALL_ENM_STUB
                          PATH_TO_ENM_SITE_OVERRIDE_FILE
                          DRAC_ENABLE
                          PATH_TO_SITE_VALUES_OVERRIDE_FILE
                          PATH_TO_CERTIFICATES_FILES
                    */
                    println('INFO: Set envsList...')
                    text = readFile(file: configFile).toString()
                    envsList = lineToList(text: text,
                                          delim: '=',
                                          this)

                    println('INFO: Update Build ENVs...')
                    env.CLUSTER = envsList.containsKey('CLUSTER') ? envsList['CLUSTER'] : getClusterName(env.RESOURCE_NAME)
                    env.NAMESPACE = envsList['NAMESPACE']
                    env.KUBECONFIG_FILE = envsList['KUBE_CONFIG']
                    env.INGRESS_IP = envsList['INGRESS_IP']
                    env.INGRESS_CLASS = envsList['INGRESS_CLASS']
                    env.ENM_STUB_IP = envsList['ENM_STUB_IP']
                    env.ADP_CREATE_NAMESPACE_PARAMS = """SKIP_TOKEN_REFRESH=true
                                                        |NAMESPACE=${env.NAMESPACE}
                                                        |EVNFM_CT_REGISTRY_HOST=${env.VNFM_REGISTRY_HOSTNAME}
                                                        |KUBECONFIG_FILE=${env.KUBECONFIG_FILE}""".stripMargin()
                    env.GR_SECONDARY_HOSTNAME = env.GR_SECONDARY_HOSTNAME ?: ''

                    switch(env.HOSTNAME_TYPE) {
                        case 'aws':
                            env.PATH_TO_CERTIFICATES_FILES = env.PATH_TO_CERTIFICATES_FILES ?: envsList['PATH_TO_CERTIFICATES_FILES_AWS']
                            env.GAS_HOSTNAME = env.GAS_HOSTNAME ?: envsList['EO_GAS_HOSTNAME_AWS']
                            env.HELM_REGISTRY_HOSTNAME = env.HELM_REGISTRY_HOSTNAME ?: envsList['EO_HELM_REGISTRY_HOSTNAME_AWS']
                            env.IAM_HOSTNAME = envsList['IAM_HOSTNAME_AWS']
                            env.VNFM_HOSTNAME = env.VNFM_HOSTNAME ?: envsList['EO_VNFM_HOSTNAME_AWS']
                            env.VNFM_REGISTRY_HOSTNAME = env.VNFM_REGISTRY_HOSTNAME ?: envsList['EO_VNFM_REGISTRY_HOSTNAME_AWS']
                            env.GR_HOSTNAME = envsList['EO_GR_HOSTNAME_AWS'] ?: ''
                            env.GLOBAL_VNFM_REGISTRY_HOSTNAME = env.GLOBAL_VNFM_REGISTRY_HOSTNAME ?: envsList['GLOBAL_VNFM_REGISTRY_HOSTNAME_AWS']
                        break
                        default:
                            env.PATH_TO_CERTIFICATES_FILES = env.PATH_TO_CERTIFICATES_FILES ?: envsList['PATH_TO_CERTIFICATES_FILES_ICCR']
                            env.GAS_HOSTNAME = env.GAS_HOSTNAME ?: envsList['EO_GAS_HOSTNAME_ICCR']
                            env.HELM_REGISTRY_HOSTNAME = env.HELM_REGISTRY_HOSTNAME ?: envsList['EO_HELM_REGISTRY_HOSTNAME_ICCR']
                            env.IAM_HOSTNAME = envsList['IAM_HOSTNAME_ICCR']
                            env.VNFM_HOSTNAME = env.VNFM_HOSTNAME ?: envsList['EO_VNFM_HOSTNAME_ICCR']
                            env.VNFM_REGISTRY_HOSTNAME = env.VNFM_REGISTRY_HOSTNAME ?: envsList['EO_VNFM_REGISTRY_HOSTNAME_ICCR']
                            env.GR_HOSTNAME = envsList['EO_GR_HOSTNAME_ICCR'] ?: ''
                            env.GLOBAL_VNFM_REGISTRY_HOSTNAME = env.GLOBAL_VNFM_REGISTRY_HOSTNAME ?: envsList['GLOBAL_VNFM_REGISTRY_HOSTNAME_ICCR'] ?: ''
                        break
                    }

                    // Add Override file for custom features
                    // ENM Stub
                    if(parseBoolean(env.INSTALL_ENM_STUB)) {
                        println('INFO: Enable ENM stub feature...')
                        env.PATH_TO_SITE_VALUES_OVERRIDE_FILE += ', ' + env.PATH_TO_ENM_SITE_OVERRIDE_FILE
                    }
                    // DRAC
                    if(parseBoolean(env.DRAC_ENABLE)) {
                        println('INFO: Enable DRAC feature...')
                        env.PATH_TO_SITE_VALUES_OVERRIDE_FILE += ', ' + env.PATH_TO_DRAC_SITE_OVERRIDE_FILE
                    }
                    // GR
                    if(parseBoolean(env.GR_ENABLE)) {
                        println('INFO: Enbale GR feature...')
                        env.PATH_TO_SITE_VALUES_OVERRIDE_FILE += ', ' + env.GR_BASE_OVERRIDE_FILE
                    }
                break
                case 'pointfix':
                case 'pointfix-release':
                case 'pre-release':
                    /*- Use Vars:
                          ADP_FETCH_UPLOAD_PARAMS
                          ADP_HELMFILE_FBU_PARAMS
                      - Use Job's ENVs:
                          CHART_LIST
                          APP_NAME
                          GERRIT_REFSPEC
                          ALLOW_DOWNGRADE
                          EVNFM_BRANCH
                          EO_BRANCH
                    */
                    env.CHART_NAME = env.CHART_LIST ? getChartParam(env.CHART_LIST, 0) : ''
                    env.CHART_VERSION = env.CHART_LIST ? getChartParam(env.CHART_LIST, 1) : ''
                    env.CHART_REPO = env.CHART_NAME ? getChartRepo(env.CHART_NAME) : ''

                    // Update ADP_HELMFILE_FBU_PARAMS
                    env.ADP_HELMFILE_FBU_PARAMS += "\nVCS_BRANCH=${checkNotBlank(env.EO_BRANCH) ? env.EO_BRANCH : 'master'}"
                break
                case 'helmfile-release':
                    /*- Use Vars:
                          ADP_CHART_RELEASE_PARAMS
                      - Use Job's ENVs:
                          APP_NAME
                          CHART_NAME
                          CHART_REPO
                          CHART_VERSION
                          GERRIT_REFSPEC
                          EVNFM_BRANCH
                    */
                    // Update ADP_CHART_RELEASE_PARAMS
                    env.ADP_CHART_RELEASE_PARAMS += "\nAPP_NAME=${env.APP_NAME}"
                    env.ADP_CHART_RELEASE_PARAMS += "\nCHART_PATH=${getAppInfo(env.APP_NAME)['CHART_PATH']}"
                    env.ADP_CHART_RELEASE_PARAMS += "\nGIT_REPO_URL=${getAppInfo(env.APP_NAME)['GIT_REPO_URL']}"
                    env.ADP_CHART_RELEASE_PARAMS += "\nCHART_NAME=${env.CHART_NAME}"
                    env.ADP_CHART_RELEASE_PARAMS += "\nCHART_REPO=${env.CHART_REPO}"
                    env.ADP_CHART_RELEASE_PARAMS += "\nCHART_VERSION=${env.CHART_VERSION}"
                    env.ADP_CHART_RELEASE_PARAMS += "\nGERRIT_REFSPEC=${env.GERRIT_REFSPEC}"
                    env.ADP_CHART_RELEASE_PARAMS += "\nVCS_BRANCH=${env.EVNFM_BRANCH}"
                    env.ADP_CHART_RELEASE_PARAMS += "\nALWAYS_RELEASE=true"
                    env.ADP_CHART_RELEASE_PARAMS += "\nPLUS_RELEASE_MODE=true"
                break
                case 'unlock-env':
                    if(checkNotBlank(envName)) {
                        println('Set envsList...')
                        text = readFile(file: configFile).toString()
                        envsList = lineToList(text: text,
                                              delim: '=',
                                              this)

                        println('INFO: Update Build ENVs...')
                        env.CLUSTER = envsList.containsKey('CLUSTER') ? envsList['CLUSTER'] : getClusterName(envName)
                        env.NAMESPACE = envsList['NAMESPACE']

                        println('Cluster name: ' + env.CLUSTER)
                        println('Namespace: ' + env.NAMESPACE)
                    }
                break
                case 'gr-deploy':
                    /*- Use Job's ENVs:
                          PRIMARY_ENV
                          SECONDARY_ENV
                    */
                    println('INFO: Set envsList for primary env...')
                    configFile = env.INTEGRATION_CI_CONFIG_DIR + '/' + env.PRIMARY_ENV + '.conf'
                    text = readFile(file: configFile).toString()
                    envsList = lineToList(text: text,
                                          delim: '=',
                                          this)

                    println('INFO: Update Primary Common Build ENVs...')
                    env.GAS_HOSTNAME = envsList['EO_GAS_HOSTNAME_AWS']
                    env.HELM_REGISTRY_HOSTNAME = envsList['EO_HELM_REGISTRY_HOSTNAME_AWS']
                    env.VNFM_HOSTNAME = envsList['EO_VNFM_HOSTNAME_AWS']
                    env.VNFM_REGISTRY_HOSTNAME = envsList['EO_VNFM_REGISTRY_HOSTNAME_AWS']
                    env.GLOBAL_VNFM_REGISTRY_HOSTNAME = envsList['GLOBAL_VNFM_REGISTRY_HOSTNAME_AWS']

                    println('INFO: Set envsList for secondary env...')
                    configFile = env.INTEGRATION_CI_CONFIG_DIR + '/' + env.SECONDARY_ENV + '.conf'
                    text = readFile(file: configFile).toString()
                    envsList = lineToList(text: text,
                                          delim: '=',
                                          this)

                    println('INFO: Update Secondary Common Build ENVs...')
                    env.GR_SECONDARY_HOSTNAME = envsList['EO_GR_HOSTNAME_AWS']
                break
                case 'get-chart-version':
                    /*- Use Job's ENVs:
                          VERSION_TYPE
                    */
                    switch(true) {
                        case project == 'eo-helmfile' && env.VERSION_TYPE == 'stable':
                            env.CHART_VERSION = getStableHelmfileVersion(this)
                            println('INFO: Stable EO-HELMFILE Version is ' + env.CHART_VERSION)
                        break
                    }
                break
            }
        }
    }
}


/* Stage for Update Job ENVs. Use:
- Vars:
    DOCKER_URL
    CLUSTER
    ICCR
    NAMESPACE_SUF
- Job's ENVs:
    JOB_TYPE
- Args:
    project(require): type String; Gerrit project name
    pom: type String; Path to the pom file; default is 'pom.xml'
    stage: type String; Name of the stage; default is 'Update Job ENVs'
    type: type String; Type of the uplifting version(is only used when env.JOB_TYPE == 'release'); default is 'base'
*/
def Update(Map Args) {
    String stageName = 'Update'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    pom: [value: Args['pom'], type: 'list', require: false],
                    stage: [value: Args['stage'], type: 'string', require: false],
                    type: [value: Args['type'], type: 'string', require: false]]
    Map commit
    String comm
    String resultComm
    String name
    String version


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['pom'] = Args.containsKey('pom') ? Args['pom'] : 'pom.xml'
    Args['stage'] = Args.containsKey('stage') ? Args['stage'] : 'Update Job ENVs'
    Args['type'] = Args.containsKey('type') ? Args['type'] : 'base'


    stage(Args['stage']) {
        dir(Args['project']) {
            println('INFO: Get commit information...')
            commit = getCommitInfo(this, WORKSPACE + '/' + Args['project'])

            switch(Args['project']) {
                case 'am-integration-charts':
                case 'eric-oss-function-orchestration-common':
                    String chartPath = getChartPath(project: Args['project'],
                                                    url: GERRIT_HTTP_URL,
                                                    branch: 'master',
                                                    this)
                    dir(chartPath) {
                        println('INFO: Set version value...')
                        comm = 'helm show chart .'
                        resultComm = sh(script: comm, returnStdout: true).trim()

                        name = readYaml(text: resultComm)['name']
                        version = readYaml(text: resultComm)['version']
                    }
                break
                case 'eric-eo-vnfm-helm-executor':
                    name = readYaml(file: 'eric-product-info.yaml')['name']
                    version = readYaml(file: 'eric-product-info.yaml')['version']
                break
                case 'am-cvnfm-utils':
                    name = readYaml(file: env.PRODUCT_INFO)['name']
                    version = readYaml(file: env.PRODUCT_INFO)['version']
                break
                case 'gr-controller':
                    println('INFO: Set project name value...')
                    comm = """mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate \\
                              | --file ${env.POM_FILE} \\
                              | -Dexpression=project.artifactId""".stripMargin()
                    comm += ' | grep -Ev "^(\\[|Download|Progress).*$"'
                    name = sh(script: comm, returnStdout: true).trim()

                    println('INFO: Set Stub version value...')
                    comm = """MAVEN_OPTS="--add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED" \\
                              | mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate \\
                              | --file ${env.POM_FILE} \\
                              | -Dexpression=project.properties""".stripMargin()
                    comm += ' | grep -Ev "^(\\[|Download|Progress).*$"'
                    comm += ' | grep gr-stubs.version'
                    version = sh(script: comm, returnStdout: true).trim()
                    version = version.minus('/')
                    env.STUB_VERSION = version.replace('<gr-stubs.version>', '')
                    println('INFO: STUB_VERSION is ' + env.STUB_VERSION)

                    println('INFO: Set version value...')
                    comm = """mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate \\
                              | --file ${env.POM_FILE} \\
                              | -Dexpression=project.version""".stripMargin()
                    comm += ' | grep -Ev "^(\\[|Download|Progress).*$"'
                    version = sh(script: comm, returnStdout: true).trim()
                break
                default:
                    println('INFO: Set project name value...')
                    comm = """mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate \\
                              | --file ${env.POM_FILE} \\
                              | -Dexpression=project.artifactId""".stripMargin()
                    comm += ' | grep -Ev "^(\\[|Download|Progress).*$"'
                    name = sh(script: comm, returnStdout: true).trim()

                    println('INFO: Set version value...')
                    comm = """mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate \\
                              | --file ${env.POM_FILE} \\
                              | -Dexpression=project.version""".stripMargin()
                    comm += ' | grep -Ev "^(\\[|Download|Progress).*$"'
                    version = sh(script: comm, returnStdout: true).trim()
                break
            }

            switch(env.JOB_TYPE) {
                case 'release':
                    switch(Args['type']) {
                        case 'base':
                            if(version.contains('SNAPSHOT')) {
                                version = version.replace('-SNAPSHOT', '')
                            } else {
                                version = upliftVersion(version: version,
                                                        flow: 'base',
                                                        this)
                            }

                            if(version.split('-').size() < 2) {
                                version += '-1'
                            }
                        break
                        case 'release':
                            version = version.replace('-SNAPSHOT', '')
                            if(version.split('-').size() < 2) {
                                version += '-1'
                            }
                            version = upliftVersion(version: version,
                                                    flow: env.TYPE,
                                                    this)
                        break
                        default:
                            version = version.replace('-SNAPSHOT', '')
                            version = upliftVersion(version: version,
                                                    flow: env.TYPE,
                                                    this)
                        break
                    }
                break
                case 'post-merge':
                    version = version.replace('-SNAPSHOT', '')
                break
                default:
                    version = version + '-' + commit['hash'][0..6]
                break
            }

            println('INFO: Project Name is ' + name)
            println('INFO: Project Version is ' + version)

            println('INFO: Update Build ENVs...')
            env.PROJECT_NAME = name
            env.PROJECT_VERSION = version
            env.IMAGE_VERSION = env.PROJECT_VERSION.replace('+', '-')
            env.CHART_VERSION = env.PROJECT_VERSION
            env.DOCKER_REPO = env.JOB_TYPE == 'release' ? 'releases' : 'snapshots'
            env.IMAGE_NAME = env.PROJECT_NAME
            env.IMAGE_DEV_BACKEND_NAME = env.IMAGE_NAME + '-dev-backend'
            env.IMAGE_REPO = env.DOCKER_URL + '/' + env.DOCKER_REPO
            env.CHART_NAME = env.CHART_NAME ?: env.PROJECT_NAME
            env.HELM_URL = env.JOB_TYPE in ['release', 'pre-release', 'pointfix'] ? env.HELM_RELEASE_URL : env.HELM_SNAPSHOT_URL
            env.CHART_REPO = env.HELM_URL
            env.CBO_VERSION = getVersionCBO(this)
            env.GIT_COMMIT_SUMMARY = commit['summary']
            env.GIT_TAG = commit['tag']
            env.GIT_HASH = commit['hash']
            env.GIT_COMMIT_AUTHOR = commit['author']
            env.GIT_COMMIT_AUTHOR_EMAIL = commit['email']


            println('INFO: Set Deployment ENVs...')
            env.CLUSTER_BASE_URL = getClusterURL( cluster: env.CLUSTER,
                                                  iccr: parseBoolean(env.ICCR),
                                                  this)
            env.NAMESPACE_SUF = env.NAMESPACE_SUF ? env.NAMESPACE_SUF : 'evnfm'
            env.NAMESPACE = env.NAMESPACE_SUF + '-' + env.JOB_TYPE[0] + 'fm'
            env.NAMESPACE_URL = env.NAMESPACE + '.' + env.CLUSTER_BASE_URL
            env.HOST_DOCKER = 'docker.' + env.NAMESPACE_URL
            env.HOST_HELM = 'helm.' + env.NAMESPACE_URL
            env.HOST_IAM = 'iam.' + env.NAMESPACE_URL
            env.HOST_VNFM = 'vnfm.' + env.NAMESPACE_URL
            env.HOST_GAS = 'gas.' + env.NAMESPACE_URL
        }
    }
}


/* Stage for Update Helmfile ENVs. Use:
- Job's ENVs:
    DNS_NAME
*/
def UpdateHelmfile() {
    String stageName = 'UpdateHelmfile'


    stage('Update Job ENVs') {
        println('Set Helmfile Host ENVs...')
        env.HOST_DOCKER = 'docker.' + env.DNS_NAME
        env.HOST_DOCKER_REGISTRY = 'docker-registry.' + env.DNS_NAME
        env.HOST_GAS = 'gas.' + env.DNS_NAME
        env.HOST_GR = 'gr.' + env.DNS_NAME
        env.HOST_HELM = 'helm.' + env.DNS_NAME
        env.HOST_IAM = 'iam.' + env.DNS_NAME
        env.HOST_VNFM = 'vnfm.' + env.DNS_NAME
    }
}


/* Stage for Unlock Environment
*/
def UnlockEnv(String name) {
    stage('Unlock Environment') {
        if(checkNotBlank(name)) {
            println('Unlock resource with name: ' + name)
            currentBuild.description = 'Name: ' + name
            unlockResource(name)
        } else {
            println('Name of the resource is blank')
        }
    }
}


/* Stage for Lock Environment. Use:
- Job's ENVs:
    WAIT_TIME
    FLOW_URL_TAG
    FLOW_URL
- Args:
    name(require): type String; Label of the locked environment
    queue: type bool; Use a queue or not; default is true
*/
def LockEnv(Map Args) {
    String stageName = 'LockEnv'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    queue: [value: Args['queue'], type: 'bool', require: false]]
    String description = env.FLOW_URL_TAG + '::' + env.FLOW_URL


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['queue'] = Args.containsKey('queue') ? Args['queue'] : true


    stage('Lock Environment') {
        if(checkNotBlank(Args['name'])) {
            addShortText( background: '',
                          borderColor: 'white',
                          text: 'Label: ' + Args['name'])

            timeout(time: env.WAIT_TIME.toInteger(),
                    unit: 'MINUTES') {
                env.ENV_NAME = lockEnvironment( label: Args['name'],
                                                description: description,
                                                queue: Args['queue'],
                                                this)
            }

            addShortText( background: '',
                          borderColor: 'white',
                          text: 'ENV: ' + env.ENV_NAME)

            // Timeout for updating status of the lockable resource
            sleep(10)
        } else {
            println('INFO: Label of the resource is blank')
        }
    }
}


/* Stage for Get Dependency ENVs. Use:
- VARs:
    GERRIT_URL
- Args:
    project(require): type String; Gerrit project name
    branch: type String; branch of the umbrella project; default is 'master'
    stage: type String; Name of the stage; default is 'Get Dependency ENVs'
*/
def GetDependencyEnv(Map Args) {
    String stageName = 'GetDependencyEnv'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    branch: [value: Args['branch'], type: 'string', require: false],
                    stage: [value: Args['stage'], type: 'string', require: false]]
    String umbrella
    String chartPath
    String chartFile
    String fileContent
    String name
    String version
    Map chart


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['stage'] = Args.containsKey('stage') ? Args['stage'] : 'Get Dependency ENVs'
    Args['branch'] = Args.containsKey('branch') ? Args['branch'] : 'master'


    stage(Args['stage']) {
        umbrella = getUmbrellaProject(Args['project'])
        chartPath = getChartPath( project: Args['project'],
                                  url: GERRIT_HTTP_URL,
                                  branch: 'master',
                                  this)
        chartFile = chartPath + '/Chart.yaml'

        fileContent = getContent( project: Args['project'],
                                  url: env.GERRIT_URL,
                                  branch: 'master',
                                  file: chartFile,
                                  this)
        name = readYaml(text: fileContent)['name']

        switch(umbrella) {
            case 'eo-helmfile':
                dir(umbrella) {
                    chart = getHelmfileChart( name: name,
                                              this)
                }
                version = chart['version']
            break
            default:
                chartPath = getChartPath( project: umbrella,
                                          url: GERRIT_HTTP_URL,
                                          branch: Args['branch'],
                                          this)
                chartFile = chartPath + '/Chart.yaml'

                fileContent = getContent( project: umbrella,
                                          url: env.GERRIT_URL,
                                          branch: Args['branch'],
                                          file: chartFile,
                                          this)
                readYaml(text: fileContent)['dependencies'].each {
                    if(it['name'] == name) {
                        version = it['version']
                    }
                }
            break
        }

        println('Dependancy name: ' + name)
        println('Dependancy version: ' + version)

        println('INFO: Set dependency ENVs...')
        env.DEPENDENCY_NAME = name
        env.DEPENDENCY_VERSION = version
    }
}


/* Stage for Set Variable. Use:
- Args:
    name(require): type String; Name of the variable
    artifact: type String; Path to the artifact value; default is ''
    stage: type String; Name of the stage; default is 'Set Variable [name]'
    skip: type Boolean; if true to skip the current stage; default is false
*/
def SetVar(Map Args) {
    String stageName = 'SetVar'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    artifact: [value: Args['artifact'], type: 'string', require: false],
                    stage: [value: Args['stage'], type: 'string', require: false],
                    skip: [value: Args['skip'], type: 'bool', require: false]]


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['artifact'] = Args.containsKey('artifact') ? Args['artifact'] : ''
    Args['stage'] = Args.containsKey('stage') ? Args['stage'] : 'Set Variable ' + Args['name']
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false


    stage(Args['stage']) {
        if(Args['skip']) {
            markStageSkippedForConditional(STAGE_NAME)
        } else {
            switch(Args['name']) {
                case 'EO_HELMFILE_BASE_VERSION':
                    String value = getArtifactProperty( path: Args['artifact'],
                                                        property: 'CHART_VERSION',
                                                        this)

                    println('INFO: Set variable...')
                    env[Args['name']] = value
                break
            }

            println('INFO: Value of variable "' + Args['name'] + '"...')
            println(env[Args['name']])
        }
    }
}

return this