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
import static com.ericsson.orchestration.mgmt.libs.VnfmArtifact.setArtifact
import static com.ericsson.orchestration.mgmt.libs.VnfmArtifact.uploadToNexus
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkArgs
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.pathExists
import static com.ericsson.orchestration.mgmt.libs.VnfmJira.createTicket
import static com.ericsson.orchestration.mgmt.libs.VnfmJira.getWikiPageConfig
import static com.ericsson.orchestration.mgmt.libs.VnfmJira.prepareContent
import static com.ericsson.orchestration.mgmt.libs.VnfmJira.uploadJira
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.getPodsLogs
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.getCNFLogs
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.getDataCollectorLogs
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.getPgDump
import static com.ericsson.orchestration.mgmt.libs.VnfmNotify.sendMail


/* Stage is for TestNG post actions. Use:
- Job's ENVs:
    ARTIFACTS_DIR
    CISM_CLUSTER
    CLUSTER
    GERRIT_PROJECT
    JIRA_TESTNG_COMPONENT
    JIRA_TESTNG_DESCR_TEMPLATE
    JIRA_TESTNG_LABELS
    JIRA_TESTNG_PROJECT
    JIRA_TESTNG_REPORTER
    JIRA_TESTNG_TYPE
    JIRA_TESTNG_WATCHERS
    JIRA_URL
    NEXUS_TESTNG_NAME
    NEXUS_TESTNG_PATH
    NAMESPACE
    NEXUS_TESTNG_NAME
    NEXUS_TESTNG_PATH
    NEXUS_URL
    TEST_FLOW
- Args:
    status(require): type String; Value of the build status
*/
def PostTestNG(Map Args) {
    String stageName = 'PostTestNG'
    Map argsList = [status: [value: Args['status'], type: 'string']]
    String buildText = '<p>Link to ticket: <a href="[JIRA_URL]">[TICKET_KEY]</a></p>'
    String ticketDesc
    String ticketKey
    String ticketUrl
    def status


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Post Actions') {
        if(Args['status'] == 'FAILURE') {
            // Get logs
            getPodsLogs(namespace: env.NAMESPACE,
                        cluster: env.CLUSTER,
                        dir: env.ARTIFACTS_DIR,
                        this)
            getCNFLogs( suffix: "-" + env.TEST_FLOW + BUILD_NUMBER + 'b',
                        cluster: env.CLUSTER,
                        dir: env.ARTIFACTS_DIR,
                        this)
            getCNFLogs( suffix: "-" + env.TEST_FLOW + BUILD_NUMBER + 'b',
                        cluster: env.CISM_CLUSTER,
                        dir: env.ARTIFACTS_DIR,
                        this)
            getDataCollectorLogs( namespace: env.NAMESPACE,
                                  cluster: env.CLUSTER,
                                  dir: env.ARTIFACTS_DIR,
                                  version: '1.4.0',
                                  this)

            // Get Database dumps
            getPgDump(name: 'orchestrator',
                      namespace: env.NAMESPACE,
                      cluster: env.CLUSTER,
                      dir: env.ARTIFACTS_DIR,
                      this)
            getPgDump(name: 'onboarding',
                      namespace: env.NAMESPACE,
                      cluster: env.CLUSTER,
                      dir: env.ARTIFACTS_DIR,
                      this)

            // Upload TestNG logs to Nexus
            uploadToNexus(name: env.NEXUS_TESTNG_NAME,
                          dir: env.ARTIFACTS_DIR,
                          url: env.NEXUS_URL,
                          path: env.NEXUS_TESTNG_PATH,
                          this)

            buildText = currentBuild.description + buildText
            if(buildText && buildText.contains('in Spinnaker')) {
                // Create Jira ticket
                ticketDesc = readFile(file: env.JIRA_TESTNG_DESCR_TEMPLATE)
                ticketDesc = ticketDesc.replace('[JOB_NAME]', JOB_NAME)
                ticketDesc = ticketDesc.replace('[BUILD_NUMBER]', BUILD_NUMBER)
                ticketDesc = ticketDesc.replace('[BUILD_URL]', BUILD_URL)
                ticketDesc = ticketDesc.replace('[NEXUS_TESTNG_NAME]', env.NEXUS_TESTNG_NAME + '.zip')
                ticketDesc = ticketDesc.replace('[NEXUS_URL]', env.NEXUS_URL)
                ticketDesc = ticketDesc.replace('[NEXUS_TESTNG_PATH]', env.NEXUS_TESTNG_PATH)

                status = createTicket(url: env.JIRA_URL,
                                      summary: 'Investigate TestNG failure #' + BUILD_NUMBER,
                                      project: env.JIRA_TESTNG_PROJECT ,
                                      reporter: env.JIRA_TESTNG_REPORTER,
                                      description: ticketDesc,
                                      type: env.JIRA_TESTNG_TYPE,
                                      labels: readYaml(text: env.JIRA_TESTNG_LABELS),
                                      component: env.JIRA_TESTNG_COMPONENT,
                                      watchers: readYaml(text: env.JIRA_TESTNG_WATCHERS),
                                      this)
                ticketKey = readJSON(text: status['log'])['key']
                ticketUrl = env.JIRA_URL + '/browse/' + ticketKey

                println('Update build description...')
                buildText = buildText.replace('[JIRA_URL]', ticketUrl)
                buildText = buildText.replace('[TICKET_KEY]', ticketKey)
                currentBuild.description = buildText
            }
        }

        // Save tests' results
        archiveArtifacts( artifacts: env.GERRIT_PROJECT + '/**/target/surefire-reports/**/*.* ',
                          allowEmptyArchive: true)

        allure([reportBuildPolicy: 'ALWAYS',
                results: [[path: env.GERRIT_PROJECT + '/**/allure-results']]])
    }
}


/* Stage is for Gerrit Deploy job post actions. Use:
- Job's ENVs:
    NAMESPACE
    CLUSTER
    GERRIT_PROJECT
- Args:
    status(require): type String; Value of the build status
*/
def GerritDeploy(Map Args) {
    String stageName = 'GerritDeploy'
    Map argsList = [status: [value: Args['status'], type: 'string']]


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Post Actions') {
        if(Args['status'] == 'FAILURE') {
            // Get Pods' Logs
            getPodsLogs(namespace: env.NAMESPACE,
                        cluster: env.CLUSTER,
                        this)
        }

        // Create and Archive Build Artifacts
        setArtifact(file: 'artifact.properties',
                    archive: true,
                    this)

        // Save tests' results
        archiveArtifacts( artifacts: env.GERRIT_PROJECT + '/**/target/screenshots/**/*.* ',
                          allowEmptyArchive: true)
        allure([reportBuildPolicy: 'ALWAYS',
                results: [[path: env.GERRIT_PROJECT + '/**/allure-results']]])
    }
}


/* Stage is for HA Deploy job post actions. Use:
- Job's ENVs:
    RESERVE_ART
    ARTIFACTS_DIR
    INSTALL_ART
- Args:
    status(require): type String; Value of the build status
    artifact: type String; Name of the archived artifact; default is 'artifact.properties'
*/
def HaDeploy(Map Args) {
    String stageName = 'HaDeploy'
    Map argsList = [status: [value: Args['status'], type: 'string'],
                    artifact: [value: Args['artifact'], type: 'string', require: false]]
    String content = ''
    String filePath


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['artifact'] = Args.containsKey('artifact') ? Args['artifact'] : 'artifact.properties'


    stage('Post Actions') {
        println('INFO: Get content for the artifact...')
        // Get content from reserve env artifact
        filePath = env.RESERVE_ART
        if(pathExists(filePath, this)) {
            content += readFile(file: filePath).trim()
            content += '\n'
        }

        // Get content from Install artifact
        filePath = env.ARTIFACTS_DIR + '/' + env.INSTALL_ART
        if(pathExists(filePath, this)) {
            content += readFile(file: filePath).trim()
        }

        println('INFO: Save the artifact...')
        writeFile(text: content,
                  file: Args['artifact'])

        println('INFO: Archive artifact...')
        archiveArtifacts( artifacts: Args['artifact'],
                          allowEmptyArchive: true)

        cleanWs()
    }
}


/* Stage is for GR Deploy job post actions. Use:
- VARs:
    ARTIFACTS_DIR
- Args:
    status(require): type String; Value of the build status
*/
def GRDeploy(Map Args) {
    String stageName = 'GRDeploy'
    Map argsList = [status: [value: Args['status'], type: 'string']]
    String content = ''
    String filePath


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Post Actions') {
        println('INFO: Get content for the artifact...')
        // Get content from primary install artifact
        filePath = env.ARTIFACTS_DIR + '/Install Primary Site.artifact'
        if(pathExists(filePath, this)) {
            content += readFile(file: filePath).trim()
            content += '\n'
        }

        println('INFO: Save the artifact...')
        writeFile(text: content,
                  file: 'artifact.properties')

        println('INFO: Archive artifact...')
        archiveArtifacts( artifacts: 'artifact.properties',
                          allowEmptyArchive: true)

        cleanWs()
    }
}


/* Stage is for GR Deploy job post actions. Use:
- VARs:
    ARTIFACTS_DIR
- Args:
    project(require): type String; Gerrit project name
    status(require): type String; Value of the build status
*/
def PostMerge(Map Args) {
    String stageName = 'PostMerge'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    status: [value: Args['status'], type: 'string']]
    def files
    ArrayList<Map> result = new ArrayList<Map>()


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Post Actions') {
        switch(Args['project']) {
            case 'eric-eo-evnfm-library-chart':
                println('INFO: Check the Design Rules tests result...')
                // List the DR reports
                files = findFiles(glob: env.ARTIFACTS_DIR + '/*.html')
                for(def drFile in files) {
                    String content
                    String name
                    String status = 'FAILED'

                    println('INFO: Parse DR report for ' + drFile['name'])
                    content = readFile(drFile.toString())

                    for(String line in content.readLines()) {
                        if(line.startsWith('<h3>')) {
                            name = line.split(':')[0]
                                        .minus('<h3>')
                            break
                        }
                    }

                    if(!content.contains('tr class="FAILED"')) {
                        status = 'PASS'
                    }

                    result.add( name: name,
                                link: drFile['name'],
                                status: status)
                }
            break
        }

        if(!result.isEmpty()) {
            println('INFO: Save the artifact...')
            writeYaml(file: 'artifact.properties',
                      data: result)

            println('INFO: Archive artifact...')
            archiveArtifacts( artifacts: 'artifact.properties',
                              allowEmptyArchive: true)
        }
    }
}


/* Stage is for Base job post actions. Use:
- VARs:
    CC_EMAIL
- Args:
    status(require): type String; Value of the build status
*/
def Base(Map Args) {
    String stageName = 'Base'
    Map argsList = [status: [value: Args['status'], type: 'string']]


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Post Actions') {
        // Send mail notification
        sendMail( email: env.CC_EMAIL,
                  subject: JOB_NAME + ' was ' + Args['status'],
                  body: BUILD_URL,
                  this)

        // Clean workspace
        cleanWs()
    }
}


/* Stage is for Helmfile job post actions
*/
def Helmfile() {
    stage('Post Actions') {
        // Create and Archive Build Artifacts
        setArtifact(file: 'artifact.properties',
                    archive: true,
                    source: 'helmfile',
                    this)

        // Clean workspace
        cleanWs()
    }
}


/* Stage is for PreRelease job post actions
*/
def PreRelease() {
    stage('Post Actions') {
        // Create and Archive Build Artifacts
        setArtifact(file: 'artifact.properties',
                    archive: true,
                    this)

        // Clean workspace
        cleanWs()
    }
}


/* Stage is for Release job post actions. Use:
- VARs:
    CC_EMAIL
- Args:
    status(require): type String; Value of the build status
*/
def Release(Map Args) {
    String stageName = 'Release'
    Map argsList = [status: [value: Args['status'], type: 'string']]


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Post Actions') {
        // Create and Archive Build Artifacts
        setArtifact(file: 'artifact.properties',
                    archive: true,
                    this)

        // Send mail notification
        sendMail( email: env.CC_EMAIL,
                  subject: JOB_NAME + ' was ' + Args['status'],
                  body: BUILD_URL,
                  this)
    }
}


/* Stage is for Service Job post actions. Use:
- Args:
    name(require): type String; Name of the job
    status: type String; Value of the build status; default is 'SUCCESS'
*/
def ServiceJob(Map Args) {
    String stageName = 'ServiceJob'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    status: [value: Args['status'], type: 'string', require: false]]


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['status'] = Args.containsKey('status') ? Args['status'] : 'SUCCESS'


    stage('Post Actions') {
        switch(true) {
            case Args['name'] == 'jobs-discover' && Args['status'] != 'FAILURE':
                /*- Use VARs:
                      WIKI_URL
                  - Job's ENVs:
                      CONTENT_FILE
                */
                Map page = getWikiPageConfig(Args['name'])
                Map content = prepareContent( type: 'wiki-page',
                                              url: env.WIKI_URL,
                                              id: page['id'],
                                              title: page['title'],
                                              body: readFile(file: env.CONTENT_FILE).trim(),
                                              this)
                uploadJira( content: content,
                            url: env.WIKI_URL + '/rest/api/content/' + page['id'],
                            method: 'PUT',
                            this)
            break
            case Args['name'] == 'lock-resource':
                /*- Use Job's ENVs:
                      ENV_NAME
                */
                println('INFO: Create and Archive Build Artifacts...')
                writeFile(file: 'artifact.properties',
                          text: 'RESOURCE_NAME=' + env.ENV_NAME)
                archiveArtifacts(artifacts: 'artifact.properties')

                println('INFO: Cleanup workspace...')
                cleanWs()
            break
            case Args['name'] == 'get-chart-version':
                /*- Use Job's ENVs:
                    CHART_NAME
                    CHART_VERSION
                */
                String result = 'CHART_NAME=' + env.CHART_NAME
                result += '\n' + 'CHART_VERSION=' + env.CHART_VERSION

                println('INFO: Create and Archive Build Artifacts...')
                writeFile(file: 'artifact.properties',
                          text: result)
                archiveArtifacts(artifacts: 'artifact.properties')

                println('INFO: Cleanup workspace...')
                cleanWs()
            break
            case Args['name'] == 'ha-robustness-tests':
                /*- Use Job's ENVs:
                      GERRIT_PROJECT
                      ALLURE_DIR
                */
                println('INFO: Save tests results...')
                allure([reportBuildPolicy: 'ALWAYS',
                        results: [[path: env.GERRIT_PROJECT + '/' + env.ALLURE_DIR]]])
            break
        }
    }
}

return this