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


/* Stage for Run Sonar Analysis. Use:
- VARs:
    SONAR_PROJECT
    SONAR_SERVER
- Job's ENVs:
    JOB_TYPE
    GERRIT_BRANCH
    GERRIT_REFSPEC
    GERRIT_CHANGE_SUBJECT
- Args:
    project(require): type String; Name of the project
    abort: type Boolean; If true to fail the stage if sonar check doesn't pass; default is true
    pom: type String; Path to the pom file; default is 'pom.xml'
*/
def Analysis(Map Args) {
    String stageName = 'Analysis'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    abort: [value: Args['abort'], type: 'bool', require: false],
                    pom: [value: Args['pom'], type: 'string', require: false]]
    String comm
    String sonarBranch
    def sonarExclusionsFile
    def sonarExclusionsContent


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['abort'] = Args.containsKey('abort') ? Args['abort'] : true
    Args['pom'] = Args.containsKey('pom') ? Args['pom'] : 'pom.xml'


    stage('Sonar Analysis') {
        dir(Args['project']) {
            println('INFO: Prepare for testing...')
            comm = '''  sudo chown -R $(id -u):$(id -g) .
                      | mkdir -p $HOME/.sonar'''.stripMargin()
            sh(comm)

            switch(env.JOB_TYPE) {
                case 'post-merge':
                case 'pre-release':
                case 'release':
                    comm = """mvn sonar:sonar \\
                              | --file ${Args['pom']} \\
                              | -Dsonar.projectKey=${env.SONAR_PROJECT}""".stripMargin()
                break
                default:
                    sonarBranch = env.GERRIT_REFSPEC.contains('refs/changes') ? env.GERRIT_REFSPEC.split('/')[3] : env.GERRIT_BRANCH
                    comm = """mvn sonar:sonar \\
                              | --file ${Args['pom']} \\
                              | -Dsonar.projectKey=${env.SONAR_PROJECT} \\
                              | -Dsonar.branch.name=change-${sonarBranch} \\
                              | -Dsonar.branch.target=${env.GERRIT_BRANCH} \\
                              | -Dsonar.analysis.mode=publish \\
                              | -Dsonar.projectVersion='Build #${BUILD_NUMBER}'""".stripMargin()
                break
            }

            if(Args['project'] == 'eric-eo-vnfm-helm-executor') {
                comm += """ -Dsonar.sources=. \\
                           | -Dsonar.language=go \\
                           | -Dsonar.tests=. \\
                           | -Dsonar.test.inclusions=**/*_test.go \\
                           | -Dsonar.go.tests.reportPaths=coverage.out \\
                           | -Dsonar.go.coverage.reportPaths=coverage.out""".stripMargin()
            }

            println('INFO: Check the sonar-exclusions.properties file...')
            if(fileExists('sonar-exclusions.properties')) {

                println('INFO: Read the sonar-exclusions.properties file...')
                sonarExclusionsFile = readFile('sonar-exclusions.properties')

                println('INFO: Check if sonar-exclusions.properties not empty...')
                if(sonarExclusionsFile.isEmpty()) {
                    println('WARNING: sonar-exclusions.properties file is empty or contains no exclusions...')
                }

                sonarExclusionsContent = sonarExclusionsFile.readLines().findAll { !it.trim().startsWith('#') }.join(',')
                comm += " -Dsonar.exclusions=${sonarExclusionsContent}"
            }

            println('INFO: Run SonarQube tests...')
            withSonarQubeEnv(env.SONAR_SERVER) {
                sh(comm)
            }

            println('INFO: Sleeping for 2 minutes...')
            sleep(120)

            // Check commit message
            switch(env.GERRIT_CHANGE_SUBJECT) {
                case ~/(.*)[b, B]locker(.*)/:
                case ~/(.*)[r, R]evert(.*)/:
                    Args['abort'] = false
                break
            }

            println('INFO: Check Quality Gate...')
            timeout(time: 10, unit: 'MINUTES') {
                waitForQualityGate(abortPipeline: Args['abort'])
            }
        }
    }
}

return this