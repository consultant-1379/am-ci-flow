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


/* Stage to set max pod limit. Use:
- Args:
    cluster(require): type String; Cluster name

*/
def SetMaxPods(Map Args) {
    String stageName = 'SetMaxPods'
    Map argsList = [cluster: [value: Args['cluster'], type: 'string']]
    String comm


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Set max pod limit per worker') {
        withCredentials([sshUserPrivateKey( credentialsId: 'amadm100_key',
                                            usernameVariable: 'SSH_USERNAME',
                                            keyFileVariable: 'SSH_PRIVATE_KEY')]) {
            comm = """ansible-playbook \\
                      | infra/ansible/kubernetes.yml \\
                      | -i infra/ansible/hosts/${Args['cluster']} \\
                      | --tags pods_capacity \\
                      | --tags restart \\
                      | -u $SSH_USERNAME \\
                      | --private-key $SSH_PRIVATE_KEY""".stripMargin()
            sh(comm)
        }
    }
}

return this