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
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkArgs
import static com.ericsson.orchestration.mgmt.libs.VnfmDocker.getDockerImagePath

/* Stage for Run Coverage Test. Use:
- Args:
    project(require): type String; Name of the project
*/
def CoverageTest(Map Args) {
      String stageName = 'CoverageTest'
      Map argsList = [project: [value: Args['project'], type: 'string']]
      String comm
      String dockerImage = getDockerImagePath('bob-gobuilder.adp-base-os')


      // Checking Arguments
      checkArgs(argsList, stageName, this)


      stage('Coverage Test') {
          dir(Args['project']) {
              println('Run Coverage test...')
              switch(Args['project']) {
                  case 'eric-eo-fh-event-to-alarm-adapter':
                  case 'eric-eo-lm-consumer':
                  case 'vnfm-orchestrator':
                      comm = 'mvn jacoco:report-aggregate'
                      sh(comm)
                  break
                  case 'eric-eo-signature-validation-lib':
                      comm = 'mvn jacoco:report'
                      sh(comm)
                  break
                  case 'gr-controller':
                      comm = """mvn clean install \\
                                | --file ${env.POM_FILE}""".stripMargin()
                      sh(comm)
                  break
                  case 'eric-eo-vnfm-helm-executor':
                      comm = """docker run --init --rm \\
                                | --volume ${WORKSPACE}/${Args['project']}:/app \\
                                | --workdir /app \\
                                | --user :\$(id -g) \\
                                | ${dockerImage} \\
                                | sh -c '\\
                                | go work init; \\
                                | go work use common; \\
                                | go work use helm-executor; \\
                                | go work use helm; \\
                                | go test ./common/... ./helm/... ./helm-executor/... \\
                                | -coverprofile=coverage.out;  \\
                                | go tool cover -func=coverage.out'""".stripMargin()
                      sh(script: comm, returnStdout: true)
                  break
                  default:
                      comm = 'mvn clean install'
                      sh(comm)
                  break
              }

              jacoco( execPattern: '**/target/**/*.exec',
                      classPattern: '**/target/classes',
                      sourcePattern: '**/src/main/java')
          }
      }
}


/* Stage for Run Checkstyle Test. Use:
- Args:
    project(require): type String; Name of the project
    pom: type String; Path to the pom file; default is 'pom.xml'
*/
def CheckstyleTest(Map Args) {
    String stageName = 'CheckstyleTest'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    pom: [value: Args['pom'], type: 'string', require: false]]
    String comm


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['pom'] = Args.containsKey('pom') ? Args['pom'] : 'pom.xml'


    stage('Checkstyle Test') {
        dir(Args['project']) {
            println('Copy checkstyle.xml...')
            comm = "cp -r ${WORKSPACE}/checkstyle.xml ."
            sh(comm)

            println('Run test...')
            comm = """mvn -ntp -fae -U \\
                      | --file ${Args['pom']} \\
                      | -V validate \\
                      | -Pcheckstyle \\
                      | -DskipTests \\
                      | -T 1C""".stripMargin()
            sh(comm)

            println('Remove checkstyle.xml...')
            comm = 'rm -rf checkstyle.xml'
            sh(comm)
        }
    }
}


/* Stage for Run Copyright Check. Use:
- Args:
    project(require): type String; Name of the project
    pom: type String; Path to the pom file; default is 'pom.xml'
*/
def CopyrightCheck(Map Args) {
    String stageName = 'CopyrightCheck'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    pom: [value: Args['pom'], type: 'string', require: false]]
    String comm


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['pom'] = Args.containsKey('pom') ? Args['pom'] : 'pom.xml'


    stage('Copyright Check') {
        dir(Args['project']) {
            println('Copy license directory...')
            comm = "cp -r ${WORKSPACE}/license ."
            sh(comm)

            println('Run Copyright codes check...')
            comm = """mvn -ntp \\
                      | --file ${Args['pom']} \\
                      | -Plicense \\
                      | license:check""".stripMargin()
            sh(comm)

            println('Remove license directory...')
            comm = 'rm -rf license'
            sh(comm)
        }
    }
}

return this