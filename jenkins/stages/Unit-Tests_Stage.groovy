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
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkArgs
import static com.ericsson.orchestration.mgmt.libs.VnfmJenkins.getCredential


/* Stage is for Run Maven Unit Tests. Use:
- Args:
    project(require): type String; Name of the project
    pom: type String; Path to the pom file; default is 'pom.xml'
*/
def MavenUnitTests(Map Args) {
    String stageName = 'MavenUnitTests'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    pom: [value: Args['pom'], type: 'list', require: false]]
    String comm
    String filePath


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['pom'] = Args.containsKey('pom') ? Args['pom'] : 'pom.xml'


    stage('Unit Tests') {
        dir(Args['project']) {
            try {
                switch(Args['project']) {
                    case 'am-package-manager':
                        println('INFO: Check path of the vnfsdk-pkgtools package file...')
                        comm = 'ls vnfsdk-*.whl'
                        filePath = sh(script: comm, returnStdout: true).trim()

                        println('INFO: Run Unit tests...')
                        comm = """mvn -ntp test \\
                                  | -Pjunit \\
                                  | -Dvnfsdk-path=./${filePath}""".stripMargin()
                        sh(comm)
                    break
                    case 'eric-eo-fh-event-to-alarm-adapter':
                    case 'eric-eo-lm-consumer':
                    case 'eric-eo-signature-validation-lib':
                    case 'eric-eo-batch-manager':
                        println('INFO: Run Unit tests...')
                        comm = 'mvn -ntp test -Pjunit'
                        sh(comm)
                    break
                    case 'vnfsdk-pkgtools':
                        println('INFO: Run Unit tests...')
                        comm = 'mvn -ntp test'
                        sh(comm)
                    break
                    case 'vnfm-orchestrator':
                        println('INFO: Run Unit tests...')
                        comm = 'mvn -ntp clean test -Punit-test'
                        sh(comm)
                    break
                    case 'gr-controller':
                        println('INFO: Run Unit tests...')
                        withCredentials([usernamePassword(credentialsId: getCredential('maven-gr'),
                                                          usernameVariable: 'MVN_USER',
                                                          passwordVariable: 'MVN_PASSWORD')]) {
                            comm = """mvn -B \\
                                      | --file ${env.POM_FILE} \\
                                      | -s settings.xml \\
                                      | test \\
                                      | -Dmvn.username=\$MVN_USER \\
                                      | -Dmvn.password=\$MVN_PASSWORD""".stripMargin()
                            sh(comm)
                        }
                    break
                    default:
                        println('INFO: Run Unit tests...')
                        comm = 'mvn -ntp surefire:test'
                        sh(comm)
                    break
                }
            } catch (err) {
                error(message: err)
            } finally {
                junit(testResults: '**/target/surefire-reports/*.xml',
                      allowEmptyResults: true)
            }
        }
    }
}


/* Stage is for Run Maven Contracts Tests. Use:
- Args:
    project(require): type String; Name of the project
*/
def MavenContractsTests(Map Args) {
    String stageName = 'MavenContractsTests'
    Map argsList = [project: [value: Args['project'], type: 'string']]
    String comm


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Contracts Tests') {
        dir(Args['project']) {
            try {
                switch(Args['project']) {
                    case 'eric-eo-batch-manager':
                        comm = 'mvn -ntp test -Pcontract'
                    break
                    case 'vnfm-orchestrator':
                        comm = 'mvn -ntp clean test -Pcontract-test'
                    break
                    default:
                        comm = '''mvn -ntp surefire:test \\
                                  | -Dtest=com.ericsson.vnfm.orchestrator.contracts.base.api.** \\
                                  | -DfailIfNoTests=false'''.stripMargin()
                    break
                }

                println('INFO: Run Contracts tests...')
                sh(comm)

            } catch (err) {
                error(message: err)
            } finally {
                junit(testResults: '**/target/surefire-reports/*.xml',
                      allowEmptyResults: true)
            }
        }
    }
}


/* Stage is for Run Maven End-to-End Tests. Use:
- Args:
    project(require): type String; Name of the project
*/
def MavenE2ETests(Map Args) {
    String stageName = 'MavenE2ETests'
    Map argsList = [project: [value: Args['project'], type: 'string']]
    String comm


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('End-to-End Tests') {
        dir(Args['project']) {
            try {
                switch(Args['project']) {
                    case 'eric-eo-batch-manager':
                        comm = 'mvn -ntp test -Pe2e'
                    break
                    case 'vnfm-orchestrator':
                        comm = 'mvn -ntp clean test -Pe2e-test'
                    break
                    default:
                        comm = '''mvn -ntp -e -U -V test \\
                                  | -T 1C \\
                                  | -Dsurefire.rerunFailingTestsCount=2'''.stripMargin()
                    break
                }

                println('INFO: Run End-to-End tests...')
                sh(comm)

            } catch (err) {
                error(message: err)
            } finally {
                junit(testResults: '**/target/surefire-reports/*.xml',
                      allowEmptyResults: true)
            }
        }
    }
}


/* Stage is for Run NodeJS Unit Tests. Use:
- Job's ENVs:
    PROJECT_NAME
- Args:
    project(require): type String; Name of the project
*/
def NodeJsTests(Map Args) {
    String stageName = 'MavenE2ETests'
    Map argsList = [project: [value: Args['project'], type: 'string']]
    String dockerImage = getDockerImagePath('node-chrome')
    String comm


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Unit Tests') {
        dir(Args['project']) {
            println('Run lint tests...')
            comm = """docker run --init --rm \\
                      | --volume ${WORKSPACE}/${Args['project']}:/app \\
                      | --workdir /app/${env.PROJECT_NAME}-gui \\
                      | --user root:\$(id -g) \\
                      | ${dockerImage} \\
                      | npm run lint""".stripMargin()
            sh(comm)

            println('Run unit tests...')
            comm = """docker run --init --rm \\
                      | --volume ${WORKSPACE}/${Args['project']}:/app \\
                      | --workdir /app/${env.PROJECT_NAME}-gui \\
                      | --user root:\$(id -g) \\
                      | -e CHROME_BIN=/usr/bin/google-chrome \\
                      | ${dockerImage} \\
                      | npm test""".stripMargin()
            sh(comm)
        }
    }
}


/* Stage is for Run Golang Unit Tests
*/
def GolangTests(String project) {
    stage('Unit Tests') {
        dir(project) {
            String dockerImage = getDockerImagePath('sles-golang')
            String comm


            println('Run unit tests...')
            comm = """docker run --init --rm \\
                      | --volume ${WORKSPACE}/${project}:/app \\
                      | --workdir /app/common \\
                      | --user :\$(id -g) \\
                      | ${dockerImage} \\
                      | sh -c \\
                      | 'go test \$(go list ./... | grep -v /vendor/) -v'""".stripMargin()
            sh(comm)
        }
    }
}


/* Stage is for Run Bash Shell Check Tests. Use:
- VARs:
    TEST_DIR
    TESTED_SCRIPT
- Job's ENVs:
    PROJECT_NAME
- Args:
    project(require): type String; Name of the project
*/
def BashShellCheckTests(Map Args) {
    String stageName = 'BashShellCheckTests'
    Map argsList = [project: [value: Args['project'], type: 'string']]
    String dockerImage = getDockerImagePath('shellcheck')
    String comm


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Bash Shell Check Tests') {
        dir(Args['project']) {
            println('INFO: Run unit tests...')
            comm = """docker run --init --rm \\
                      | --volume ${WORKSPACE}/${Args['project']}:/app \\
                      | --user :\$(id -g) \\
                      | ${dockerImage} \\
                      | /app/${env.TEST_DIR}/${env.TESTED_SCRIPT}""".stripMargin()
            sh(comm)
        }
    }
}


/* Stage is for Run Bash Call Grapg Tests. Use:
- VARs:
    TEST_DIR
    TESTED_SCRIPT
    NEXUS_URL
- Job's ENVs:
    PROJECT_NAME
    TESTED_SCRIPT
- Args:
    project(require): type String; Name of the project
*/
def BashCallGrapgTests(Map Args) {
    String stageName = 'BashCallGrapgTests'
    Map argsList = [project: [value: Args['project'], type: 'string']]
    String dockerImage = getDockerImagePath('callgraph')
    String comm


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Bash Call Grapg Tests') {
        dir(Args['project']) {
            println('INFO: Run unit tests...')
            comm = """docker run --init --rm \\
                      | --volume ${WORKSPACE}/${Args['project']}:/app \\
                      | --user :\$(id -g) \\
                      | ${dockerImage} \\
                      | /app/${env.TEST_DIR}/${env.TESTED_SCRIPT} \\
                      | -output /app/callGraph.png""".stripMargin()
            sh(comm)

            println('INFO: Upload Design rule check report to Nexus...')
            uploadToNexus(name: 'callgraph',
                          dir: '.',
                          url: env.NEXUS_URL,
                          path: 'evnfm_storage/Jenkins/callGraph',
                          type: 'png',
                          artifact: 'callGraph.png',
                          this)
        }
    }
}


/* Stage is for Run Bash Unit Tests. Use:
- VARs:
    TEST_DIR
    TESTS_FILE
- Job's ENVs:
    CLUSTER
- Args:
    project(require): type String; Name of the project
 */
def BashUnitTests(Map Args) {
    String stageName = 'BashUnitTests'
    Map argsList = [project: [value: Args['project'], type: 'string']]
    String dockerImage = getDockerImagePath('bats')
    String comm


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Bash Unit Tests') {
        dir(Args['project']) {
            withCredentials([file(credentialsId: env.CLUSTER,
                                  variable: 'KUBE_CONFIG_PATH')]) {
                println('INFO: Run unit tests...')
                comm = """docker run --init --rm \\
                          | --volume  ${WORKSPACE}/${Args['project']}/${env.TEST_DIR}:/code \\
                          | --volume \$KUBE_CONFIG_PATH:/root/.kube/config \\
                          | --user :\$(id -g) \\
                          | ${dockerImage} \\
                          | /code/test/${env.TESTS_FILE}""".stripMargin()
                sh(comm)
            }
        }
    }
}

return this