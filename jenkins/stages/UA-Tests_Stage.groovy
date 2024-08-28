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
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils
import static com.ericsson.orchestration.mgmt.libs.VnfmDocker.generateCSAR
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkArgs
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.getExternalIP
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.getNodesIPs
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.getResourceInfo
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.getServicePort


/* Stage is for Run Acceptance Maven Tests. Use:
- VARs:
    CLUSTER
    ICCR(for eric-eo-evnfm-crypto)
    IDAM_USERNAME(for am-common-wfs-ui)
    DEPLOY_PASSWORD(for am-common-wfs-ui)
- Job's ENVs:
    GERRIT_CHANGE_NUMBER
    PROJECT_NAME
    NAMESPACE
    HOST_IAM(for am-common-wfs-ui)
    HOST_VNFM(for am-common-wfs-ui)
- Args:
    name(require): type String; Name of the project
    skip: type Boolean; If true to skip the current stage; default is false
*/
def MavenTests(Map Args) {
    String stageName = 'MavenTests'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    skip: [value: Args['skip'], type: 'bool', require: false]]
    String comm
    String internalIP
    String port


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false


    stage('Acceptance Tests') {
        if(Args['skip']) {
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            dir(Args['name']) {
                switch(Args['name']) {
                    case 'am-common-wfs':
                        internalIP = getNodesIPs( cluster: env.CLUSTER,
                                                  type: 'InternalIP',
                                                  this)[0]
                        port = getServicePort(name: env.PROJECT_NAME,
                                              namespace: env.NAMESPACE,
                                              cluster: env.CLUSTER,
                                              type: 'nodePort',
                                              this)
                        comm = """mvn -ntp clean install \\
                                  | -Pacceptance -DskipAssembly=true \\
                                  | -Dnode.name=$NODE_NAME-$BUILD_NUMBER \\
                                  | -Dcontainer.host=http://${internalIP}:${port} \\
                                  | -DGERRIT_CHANGE_NUMBER=${env.NAMESPACE.replace('-', '')} \\
                                  | -Dnamespace=${env.NAMESPACE} \\
                                  | -Dcluster.config=\$KUBE_CONFIG_PATH""".stripMargin()
                    break
                    case 'am-common-wfs-ui':
                        comm = """mvn -ntp install \\
                                  | -Pacceptance \\
                                  | -DallBrowsers \\
                                  | -Dkeycloak.host=https://${env.HOST_IAM} \\
                                  | -Dcontainer.host=https://${env.HOST_VNFM}/vnfm \\
                                  | -Dkeycloak.user=${env.IDAM_USERNAME} \\
                                  | -Dkeycloak.password=${env.DEPLOY_PASSWORD}""".stripMargin()
                    break
                    case 'eric-eo-evnfm-crypto':
                        internalIP = getNodesIPs( cluster: env.CLUSTER,
                                                  type: 'InternalIP',
                                                  this)[0]
                        port = getServicePort(name: env.PROJECT_NAME,
                                              namespace: env.NAMESPACE,
                                              cluster: env.CLUSTER,
                                              type: 'nodePort',
                                              this)
                        comm = """mvn -ntp clean install \\
                                  | -Pacceptance -DskipAssembly=true \\
                                  | -Dnode.name=${NODE_NAME}-${BUILD_NUMBER} \\
                                  | -Dnamespace=${env.NAMESPACE} \\
                                  | -Dcontainer.host=http://${internalIP}:${port}""".stripMargin()
                        comm = env.GERRIT_CHANGE_NUMBER ? comm + " -DGERRIT_CHANGE_NUMBER=${env.GERRIT_CHANGE_NUMBER}" : comm
                        comm = env.ICCR.toBoolean() ? comm + ' -Dingress.class.name=iccr' : comm
                    break
                    case 'am-onboarding-service':
                        internalIP = getNodesIPs( cluster: env.CLUSTER,
                                                  type: 'InternalIP',
                                                  this)[0]
                        port = getServicePort(name: env.PROJECT_NAME,
                                              namespace: env.NAMESPACE,
                                              cluster: env.CLUSTER,
                                              type: 'nodePort',
                                              this)
                        comm = """JAVA_TOOL_OPTIONS="-Xmx8G" mvn -ntp clean install \\
                                  | -Pacceptance -DskipAssembly=true \\
                                  | -Dnode.name=$NODE_NAME-$BUILD_NUMBER \\
                                  | -Dnamespace=${env.NAMESPACE} \\
                                  | -Dcontainer.host=http://${internalIP}:${port}""".stripMargin()
                        comm = env.GERRIT_CHANGE_NUMBER ? comm + " -DGERRIT_CHANGE_NUMBER=${env.GERRIT_CHANGE_NUMBER}" : comm
                    break
                    case 'cvnfm-enm-cli-stub':
                        println('Install tests packages...')
                        comm = 'pip install -r requirements.txt'
                        sh(comm)

                        internalIP = getExternalIP( name: env.PROJECT_NAME,
                                                    namespace: env.NAMESPACE,
                                                    cluster: env.CLUSTER,
                                                    this)
                        comm = """mvn -DstubIP=${internalIP} \\
                                  | -P acceptance test""".stripMargin()
                    break
                    case 'eric-eo-batch-manager':
                        comm = 'mvn -ntp test -Pintegration'
                    break
                    default:
                        internalIP = getNodesIPs( cluster: env.CLUSTER,
                                                  type: 'InternalIP',
                                                  this)[0]
                        port = getServicePort(name: env.PROJECT_NAME,
                                              namespace: env.NAMESPACE,
                                              cluster: env.CLUSTER,
                                              type: 'nodePort',
                                              this)
                        comm = """mvn -ntp clean install \\
                                  | -Pacceptance -DskipAssembly=true \\
                                  | -Dnode.name=$NODE_NAME-$BUILD_NUMBER \\
                                  | -Dnamespace=${env.NAMESPACE} \\
                                  | -Dcontainer.host=http://${internalIP}:${port}""".stripMargin()
                        comm = env.GERRIT_CHANGE_NUMBER ? comm + " -DGERRIT_CHANGE_NUMBER=${env.GERRIT_CHANGE_NUMBER}" : comm
                    break
                }

                println('Run Acceptance Tests...')
                withCredentials([file(credentialsId: env.CLUSTER,
                                      variable: 'KUBE_CONFIG_PATH')]) {
                    sh('KUBECONFIG=$KUBE_CONFIG_PATH ' + comm)
                }
            }
        }
    }
}


/* Stage for Acceptance Maven Tests with CSARs. Use:
- Job's ENVs:
    IMAGE_NAME
    IMAGE_VERSION
    IMAGE_REPO
- Args:
    name(require): type String; Name of the project
*/
def MavenCSAR(Map Args) {
    String stageName = 'MavenCSAR'
    Map argsList = [name: [value: Args['name'], type: 'string']]
    String image = "${env.IMAGE_REPO}/${env.IMAGE_NAME}:${env.IMAGE_VERSION}"
    String comm
    String filePath


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Acceptance Tests') {
        dir(Args['name']) {
            println('Generate basic CSAR...')
            generateCSAR( project: Args['name'],
                          image: image,
                          type: 'basic',
                          this)

            println('Generate Helm3 CSAR...')
            generateCSAR( project: Args['name'],
                          image: image,
                          type: 'helm3',
                          this)

            println('Generate Multi Chart CSAR...')
            generateCSAR( project: Args['name'],
                          image: image,
                          type: 'multi-chart',
                          this)

            println('Generate Signed CSAR...')
            generateCSAR( project: Args['name'],
                          image: image,
                          type: 'signed',
                          this)

            println('Generate Light Weight CSAR...')
            generateCSAR( project: Args['name'],
                          image: image,
                          type: 'lightweight',
                          this)

            println('Generate Helmfile CSAR...')
            generateCSAR( project: Args['name'],
                          image: image,
                          type: 'helmfile-csar',
                          this)

            println('Generate Light Weight Helmfile CSAR...')
            generateCSAR( project: Args['name'],
                          image: image,
                          type: 'lightweight-helmfile',
                          this)

            switch(Args['name']) {
                case 'am-package-manager':
                    println('Check path of the vnfsdk-pkgtools package file...')
                    comm = 'ls vnfsdk-*.whl'
                    filePath = sh(script: comm, returnStdout: true).trim()

                    comm = """mvn -ntp clean package \\
                              | -DskipTests \\
                              | -Pacceptance \\
                              | -Dvnfsdk-path=./${filePath}""".stripMargin()
                break
                default:
                    comm = 'mvn -ntp clean install -DskipTests -Pacceptance'
                break
            }

            println ('Run Acceptance tests...')
            sh(comm)
        }
    }
}


/* Stage is for Run TestNG Acceptance Tests. Use:
- Job's ENVs:
    CLUSTER
    CISM_CLUSTER
    NAMESPACE
    TEST_FLOW
    TEST_SUITES_FLOW
    TEST_THREAD_COUNT
    BUILD_NUMBER
    HOST_VNFM
    HOST_IAM
    IDAM_USERNAME
    IDAM_PASSWORD
    HOST_HELM
    ARTIFACTS_DIR
*/
def TestngTests(String project) {
    stage('TestNG Acceptance Tests') {
        dir(project) {
            String comm
            Integer status


            withCredentials([ file( credentialsId: env.CLUSTER,
                                    variable: 'KUBE_CONFIG_PATH'),
                              file( credentialsId: env.CISM_CLUSTER,
                                    variable: 'EXTERNAL_KUBE_CONFIG_PATH')]) {
                println('Run TestNG Acceptance Tests...')
                comm = """#!/usr/bin/env  bash
                          |set -o pipefail && mvn clean install -P acceptance-testng \\
                          |-DidamClientId=eo \\
                          |-DidamRealm=master \\
                          |-Dhelm=helm3 \\
                          |-DcsarDownloadPath=\$HOME/release-testing-csars \\
                          |-Dnamespace=${env.NAMESPACE} \\
                          |-DglobalFlowSuffix=${env.TEST_FLOW}${env.BUILD_NUMBER}b \\
                          |-Dtestng.suit=${env.TEST_SUITES_FLOW}.xml \\
                          |-Dtestng.${env.TEST_SUITES_FLOW}.suit.threadcount=${env.TEST_THREAD_COUNT} \\
                          |-DevnfmUrl=https://${env.HOST_VNFM} \\
                          |-DidamUrl=https://${env.HOST_IAM} \\
                          |-DidamAdminUser=${env.IDAM_USERNAME} \\
                          |-DidamAdminPassword=${env.IDAM_PASSWORD} \\
                          |-DhelmRegistryUrl=https://${env.HOST_HELM}""".stripMargin()
                if(env.TEST_SUITES_FLOW == 'long') {
                    comm += " -DdracEnabled=true"
                }
                comm += " | tee ${env.TESTNG_LOG_NAME}"
                status = sh(script: comm, returnStatus: true).toInteger()
            }

            println('Archive TestNG logs...')
            archiveArtifacts( artifacts: env.TESTNG_LOG_NAME,
                              allowEmptyArchive: true)

            if(status != 0) {
                println('Copy logs to artifact directory...')
                comm = """mkdir -p ../${env.ARTIFACTS_DIR}
                          |cp ${env.TESTNG_LOG_NAME} ../${env.ARTIFACTS_DIR}/""".stripMargin()
                sh(comm)

                println('ERROR: Test is FAILED')
                comm = "exit 1"
                sh(comm)
            }
        }
    }
}


/* Stage is for Run Internal Acceptance Tests. Use:
- VARs:
    INTERNAL_TESTS_DIR
    INTERNAL_TESTS_FILE
    INTERNAL_TESTS_JAR
- Job's ENVs:
    HOST_VNFM
    HOST_IAM
    IDAM_USERNAME
    IDAM_PASSWORD
    CLUSTER
    NAMESPACE
*/
def InternalTests(String project) {
    stage('Internal Acceptance Tests') {
        dir(project) {
            String comm
            String configContent
            String configFile = 'internal-tests.json'


            configContent = readFile(file: env.INTERNAL_TESTS_FILE)
            dir(env.INTERNAL_TESTS_DIR) {
                println('Prepare config file...')
                configContent = configContent.replace('<GATEWAY_HOST>', env.HOST_VNFM)
                configContent = configContent.replace('<KEYCLOAK_HOST>', env.HOST_IAM)
                configContent = configContent.replace('<IDAM_ADMIN_USERNAME>', env.IDAM_USERNAME)
                configContent = configContent.replace('<IDAM_ADMIN_PASSWORD>', env.IDAM_PASSWORD)

                withCredentials([file(credentialsId: env.CLUSTER, variable: 'KUBE_CONFIG_PATH')]) {
                    comm = """kubectl get secret eric-cncs-oss-config-iam-client-secret \\
                              | -o jsonpath='{.data.clientSecret}' \\
                              | --namespace ${env.NAMESPACE} \\
                              | --kubeconfig=\$KUBE_CONFIG_PATH | \\
                              |base64 --decode""".stripMargin()
                               // | sed -e 's`[][\\/.*&^$]`\\&`g')
                    env.KEYCLOAK_CLIENT_SECRET = sh(script: comm, returnStdout: true).trim()
                    configContent = configContent.replace('<KEYCLOAK_CLIENT_SECRET>', env.KEYCLOAK_CLIENT_SECRET)
                    writeFile(file: configFile, text: configContent)

                    println('Run E2E internal acceptance tests...')
                    comm = """java -jar ${env.INTERNAL_TESTS_JAR} \\
                              | -f ${configFile}""".stripMargin()
                    sh(comm)
                }
            }
        }
    }
}


/* Stage is for Run Containers Restart Tests. Use:
- Job's ENVs:
    CLUSTER
    NAMESPACE
- Args:
    name(require): type String; Name of the project
*/
def ContainersRestartTests(Map Args) {
    String stageName = 'ContainersRestartTests'
    Map argsList = [name: [value: Args['name'], type: 'string']]
    String comm
    String podName
    String nodeIP
    String nodePodId
    Integer podCount
    def count = 0


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Containers Restart Tests') {
        withCredentials([file(credentialsId: env.CLUSTER,
                              variable: 'KUBE_CONFIG_PATH')]) {
            println('Get a pod name...')
            comm = """kubectl --namespace ${env.NAMESPACE} \\
                     |--kubeconfig=\$KUBE_CONFIG_PATH \\
                     |get pods \\
                     |-o name | \\
                     |grep ${Args['name']}""".stripMargin()
            podName = sh(script: comm, returnStdout: true).trim()
            podName = podName.split('\n')[0].replace('pod/', '')

            println('Get a node IP...')
            comm = """kubectl --namespace ${env.NAMESPACE} \\
                      |--kubeconfig=\$KUBE_CONFIG_PATH \\
                      |get pods ${podName} \\
                      |-o jsonpath='{.status.hostIP}'""".stripMargin()
            nodeIP = sh(script: comm, returnStdout: true).trim()
        }

        println('Set podCount variable...')
        podCount = getResourceInfo( name: env.PROJECT_NAME,
                                    type: 'deployment',
                                    namespace: env.NAMESPACE,
                                    cluster: env.CLUSTER,
                                    field: 'availableReplicas',
                                    this).toInteger()
        println('Default deployed pods count: ' + podCount)

        withCredentials([sshUserPrivateKey( credentialsId: 'amadm100_key',
                                            usernameVariable: 'SSH_USERNAME',
                                            keyFileVariable: 'SSH_PRIVATE_KEY')]) {
            sshagent(credentials: ['amadm100_key']) {
                println('Get Pod ID on the node...')
                comm = """ssh -q \\
                          |\$SSH_USERNAME@${nodeIP} \\
                          |'sudo crictl pods \\
                          |--namespace ${env.NAMESPACE} \\
                          |--name ${podName} \\
                          |--quiet'""".stripMargin()
                nodePodId = sh(script: comm, returnStdout: true).trim()
                nodePodId = nodePodId[0..12]

                println('Stop the pod on node...')
                comm = """ssh -q \\
                          |\$SSH_USERNAME@${nodeIP} \\
                          |'sudo crictl stopp ${nodePodId}'""".stripMargin()
                sh(comm)

                println('Delete the pod on the node...')
                comm = """ssh -q \\
                          |\$SSH_USERNAME@${nodeIP} \\
                          |'sudo crictl rmp ${nodePodId}'""".stripMargin()
                sh(comm)
            }
        }

        timeout(time: 5, unit: 'MINUTES') {
            while(count < podCount) {
                sleep(10)

                count = getResourceInfo(name: env.PROJECT_NAME,
                                        type: 'deployment',
                                        namespace: env.NAMESPACE,
                                        cluster: env.CLUSTER,
                                        field: 'availableReplicas',
                                        this)
                count = count ? count.toInteger() : 0

                println('Count of running pods: ' + count)
            }

            println('INFO: TEST PASSED')
        }
    }
}


/* Stage is for Run HA Robustness Tests. Use:
- Job's ENVs:
    ALLURE_DIR
    CLUSTER
    NAMESPACE
    VNFM_USER
    VNFM_PASSWORD
- Args:
    project(require): type String; Name of the project
    host(require): type String; Name of the testing host
    path(require): type String; Path to the testing directory
*/
def RobustnessTests(Map Args) {
    String stageName = 'RobustnessTests'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    host: [value: Args['host'], type: 'string'],
                    path: [value: Args['path'], type: 'string']]
    String comm


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('HA Robustness Tests') {
        dir(Args['project']) {
            println('Install required packages...')
            comm = """sudo apt update
                      |sudo apt install -y libpq-dev
                      |pip --version
                      |python -m pip install virtualenv
                      |pip install -r requirements.txt
                      |mkdir ${env.ALLURE_DIR}""".stripMargin()
            sh(comm)

            println('Start HA robustness tests...')
            withCredentials([file(credentialsId: env.CLUSTER,
                                  variable: 'KUBE_CONFIG_PATH')]) {
                comm = """  HOSTNAME=${Args['host']} \\
                          | NAMESPACE=${env.NAMESPACE} \\
                          | VNFM_USER=${env.VNFM_USER} \\
                          | VNFM_PASSWORD=${env.VNFM_PASSWORD} \\
                          | python \\
                          | -m pytest \\
                          | ${Args['path']} \\
                          | --alluredir ${env.ALLURE_DIR}""".stripMargin()
                sh(comm)
            }
        }
    }
}


/* Stage is for Run GR Internal Tests. Use:
- Job's ENVs:
    CLUSTER
    NAMESPACE
- Args:
    name(require): type String; Name of the project
    skip: type Boolean; If true to skip the current stage; default is false
*/
def InternalTestsGR(Map Args) {
    String stageName = 'InternalTestsGR'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    skip: [value: Args['skip'], type: 'bool', require: false]]
    ArrayList<String> secrets = new ArrayList<String>()
    String comm
    def resComm


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false


    stage('GR Internal Tests') {
        if(Args['skip']) {
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            dir(Args['name']) {
                println('INFO: Run Check Empty Secret tests...')
                withCredentials([ file( credentialsId: env.CLUSTER,
                                        variable: 'KUBE_CONFIG_PATH')]) {
                    comm = """kubectl get secrets \\
                              | --kubeconfig=\$KUBE_CONFIG_PATH \\
                              | --namespace ${env.NAMESPACE} \\
                              | --no-headers \\
                              | -o yaml""".stripMargin()
                    resComm = sh(script: comm, returnStdout: true).trim()
                }

                readYaml(text: resComm)['items'].each {
                    if(!it['data']) {
                        secrets.add(it['metadata']['name'])
                    }
                }

                if(secrets.size() > 0) {
                    secrets.each {
                        unstable(message: 'WARNING: Secret "' + it + '" is empty')
                    }
                }
            }
        }
    }
}

return this