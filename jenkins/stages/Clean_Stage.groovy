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
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkArgs
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkNotBlank
import static com.ericsson.orchestration.mgmt.libs.VnfmDocker.deleteImages
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.deleteNamespace
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.deletePartNamespaces
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.deleteResources
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.getResources


/* Stage for Clean EVNFM environment. Use:
- Job's ENVs:
    JOB_TYPE
    CLUSTER
    NAMESPACE
    CISM_CLUSTER
*/
def RunCleanEVNFM() {
    stage('Clean') {
        String comm
        Integer commStatus


        switch(env.JOB_TYPE) {
            case 'unlock-env':
                if(checkNotBlank(env.NAMESPACE)) {
                    println('INFO: Delete namespace...')
                    deleteNamespace(namespace: env.NAMESPACE,
                                    cluster: env.CLUSTER,
                                    this)
                }
            break
            case 'testng':
                println('INFO: Delete part namespaces...')
                deletePartNamespaces( name: '.*' + env.TEST_FLOW + BUILD_NUMBER + 'b',
                                      cluster: env.CLUSTER,
                                      this)
                deletePartNamespaces( name: '.*' + env.TEST_FLOW + BUILD_NUMBER + 'b',
                                      cluster: env.CLUSTER,
                                      this)
            break
        }

        println('Fix repository permissions...')
        comm = 'sudo chown -R $(id -u):$(id -g) .'
        sh(comm)

        comm = 'docker ps'
        commStatus = sh(script: comm, returnStatus: true).toInteger()
        if(commStatus == 0) {
            println('Cleanup Docker environments...')
            comm = '''  docker rm -f $(docker ps -aq) || echo "INFO: Docker containers are absent"
                      | docker image prune -f
                      | docker volume prune -f'''.stripMargin()
            sh(comm)
            deleteImages(name: 'armdocker', this)
        }

        println('Cleanup tempory directories...')
        comm = '''  sudo rm -rf ~/.m2/repository/* || echo "INFO: Could not cleanup .m2 directory"
                  | sudo rm -rf ~/.cache/* || echo "INFO: Could not cleanup .cache directory"
                  | sudo rm -f /tmp/*.yaml || echo "INFO: Could not delete tempory yaml files"
                  | rm -rf /tmp/* || echo "INFO: Could not delete all files in /tmp directory"
                  | sudo rm -rf ~/.sonar/cache/* || echo "INFO: Could not cleanup Sonar Cache directory"
                  | rm -rf ~/release-testing-csars || echo "INFO: Could not delete CSAR directory"
                  | helm3 repo remove test-scale-chart || echo "INFO: Helm repository test-scale-chartund is not found"'''.stripMargin()
        sh(comm)
    }
}


/* Stage for Clean Build environment. Use:
- VARs:
    GERRIT_PROJECT
    CLUSTER
- Job's ENVs:
    NAMESPACE
- Args:
    deleteDeploy: type Boolean; If true to run delete deploy step; default false
    skip: type Boolean; If true to skip the current stage; default false
    stage: type String; Stage custom name; default is 'Clean'
*/
def CleanBuild(Map Args = [:]) {
    String stageName = 'CleanBuild'
    Map argsList = [deleteDeploy: [value: Args['deleteDeploy'], type: 'bool', require: false],
                    skip: [value: Args['skip'], type: 'bool', require: false],
                    stage: [value: Args['stage'], type: 'string', require: false]]
    String comm
    Integer commStatus
    List<String> resources = new ArrayList<String>()


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['deleteDeploy'] = Args.containsKey('deleteDeploy') ? Args['deleteDeploy'] : false
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false
    Args['stage'] = Args.containsKey('stage') ? Args['stage'] : 'Clean'


    stage(Args['stage']) {
        if(Args['skip']) {
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            try {
                println('Fix repository permissions...')
                comm = 'sudo chown -R $(id -u):$(id -g) .'
                sh(comm)

                comm = 'docker ps'
                commStatus = sh(script: comm, returnStatus: true).toInteger()
                if(commStatus == 0) {
                    println('Cleanup Docker environments...')
                    comm = '''  docker rm -f $(docker ps -aq) || echo "INFO: Docker containers are absent"
                              | docker image prune -f
                              | docker volume prune -f'''.stripMargin()
                    sh(comm)
                    deleteImages(name: 'armdocker', this)
                }

                println('Cleanup tempory directories...')
                comm = '''  sudo rm -rf ~/.m2/repository/* || echo "INFO: Could not cleanup .m2 directory"
                          | sudo rm -rf ~/.cache/* || echo "INFO: Could not cleanup .cache directory"
                          | sudo rm -f /tmp/*.yaml || echo "INFO: Could not delete tempory yaml files"
                          | rm -rf /tmp/* || echo "INFO: Could not delete all files in /tmp directory"
                          | sudo rm -rf ~/.sonar/cache/* || echo "INFO: Could not cleanup Sonar Cache directory"'''.stripMargin()
                sh(comm)


                if(Args['deleteDeploy']) {
                    withCredentials([file(credentialsId: env.CLUSTER,
                                          variable: 'KUBE_CONFIG_PATH')]) {
                        println('Delete ClusterRoleBinding...')
                        resources = "clusterrolebinding/cluster-evnfm-${env.NAMESPACE}".split()
                        deleteResources(resources: resources,
                                        cluster: '$KUBE_CONFIG_PATH',
                                        this)


                        if(env.NAMESPACE) {
                            println('Delete namespace...')
                            deleteNamespace(namespace: env.NAMESPACE,
                                            cluster: env.CLUSTER,
                                            this)
                        }

                        switch(env.GERRIT_PROJECT) {
                            case 'am-common-wfs':
                                println('Delete ClusterRoles...')
                                resources = getResources( type: 'ClusterRole',
                                                          cluster: env.CLUSTER,
                                                          partName: 'wfs-accept',
                                                          this)
                                deleteResources(resources: resources,
                                                cluster: '$KUBE_CONFIG_PATH',
                                                this)

                                println('Delete ClusterRoleBinding...')
                                resources = getResources( type: 'ClusterRoleBinding',
                                                          cluster: env.CLUSTER,
                                                          partName: 'wfs-accept',
                                                          this)
                                deleteResources(resources: resources,
                                                cluster: '$KUBE_CONFIG_PATH',
                                                this)

                                println('Delete testing namespaces...')
                                deletePartNamespaces( name: 'wfs-acceptance.*' + BUILD_NUMBER,
                                                      cluster: env.CLUSTER,
                                                      this)
                            break
                        }
                    }
                }
            } catch (err) {
                currentBuild.result = 'FAILURE'
                throw err
            } finally {
                cleanWs()
            }
        }
    }
}


/* Stage for Cleanup Cluster. Use:
- Args:
    cluster(require): type String; Kubernetes Cluster name
    fileName(require): type String; Name of file with namespace parameters
    time: type Integer; Timeout in minutes of waiting for delete processes; default is 10
*/
def CleanupCluster(Map Args) {
    String stageName = 'CleanupCluster'
    Map argsList = [cluster: [value: Args['cluster'], type: 'string'],
                    fileName: [value: Args['fileName'], type: 'string'],
                    time: [value: Args['time'], type: 'integer', require: false]]


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['time'] = Args.containsKey('time') ? Args['time'] : 10


    stage(Args['cluster'] + ': Cleanup') {
        for(def ns in readYaml(file: Args['fileName'])) {
            println('Namespace: ' + ns['Namespace'])
            println('Delete status: ' + ns['Will Be Deleted'])

            if(ns['Will Be Deleted']) {
                try {
                    timeout(time: Args['time'], unit: 'MINUTES') {
                        deleteNamespace(namespace: ns['Namespace'],
                                        cluster: Args['cluster'],
                                        this)
                    }
                } catch (Exception errClean) {
                    println(errClean)
                }
            }
        }
    }
}


/* Stage for Clean Agent. Use:
- Args:
    name(require): type String; Name of the Jenkins agent
    skip: type Boolean; If true to skip the current stage; default is false
*/
def CleanAgent(Map Args) {
    String stageName = 'CleanAgent'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    skip: [value: Args['skip'], type: 'bool', require: false]]
    String comm
    String commStatus


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false


    stage('Clean Agent: ' + Args['name']) {
        if(Args['skip']) {
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            node(Args['name']) {
                comm = 'docker ps'
                commStatus = sh(script: comm, returnStatus: true).toInteger()
                if(commStatus == 0) {
                    println('Cleanup Docker environments...')
                    comm = '''  docker rm -f $(docker ps -aq) || echo "INFO: Docker containers are absent"
                              | docker image prune -f
                              | docker volume prune -f'''.stripMargin()
                    sh(comm)
                    deleteImages(name: 'armdocker', this)
                }

                println('Cleanup tempory directories...')
                comm = '''  sudo rm -rf ~/.m2/repository/* || echo "INFO: Could not cleanup .m2 directory"
                          | sudo rm -rf ~/.cache/* || echo "INFO: Could not cleanup .cache directory"
                          | sudo rm -f /tmp/*.yaml || echo "INFO: Could not delete tempory yaml files"
                          | rm -rf /tmp/* || echo "INFO: Could not delete all files in /tmp directory"
                          | sudo rm -rf ~/.sonar/cache/* || echo "INFO: Could not cleanup Sonar Cache directory"'''.stripMargin()
                sh(comm)
            }
        }
    }
}

return this