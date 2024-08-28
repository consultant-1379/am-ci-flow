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
import static com.ericsson.orchestration.mgmt.libs.VnfmJira.addTicketComment


/* Function for Add Comment to the Jira ticket(s). Use:
- Job's ENVs:
    JOB_NAME
    BUILD_NUMBER
    CHART_NAME(if exist)
    CHART_VERSION(if exist)
    CHART_REPO(if exist)
    GERRIT_CHANGE_ID(if exist)
    GERRIT_CHANGE_SUBJECT(if exist)
    GERRIT_CHANGE_OWNER(if exist)
    GERRIT_CHANGE_URL(if exist)
- Args:
    url(require): type String; URL of the Jira host
    subject(require): type String; Subject of the change
*/
def AddJiraTicketComment(Map Args) {
    String stageName = 'addJiraTicketComment'
    Map argsList = [url: [value: Args['url'], type: 'string'],
                    subject: [value: Args['subject'], type: 'string']]


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Add Comment to Jira Ticket') {
        println('INFO: Set BUILD_STATUS env...')
        env.BUILD_STATUS = currentBuild.result ?: 'SUCCESS'

        println('INFO: Add comment to ticket')
        addTicketComment( url: Args['url'],
                          subject: Args['subject'],
                          this)
    }
}

return this