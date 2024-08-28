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
import static com.ericsson.orchestration.mgmt.libs.VnfmDocker.getDockerImagePath
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkArgs
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.tableHTML
import static com.ericsson.orchestration.mgmt.libs.VnfmHelm.checkHelmDeploy
import static com.ericsson.orchestration.mgmt.libs.VnfmJira.wikiUpload
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.checkNamespace
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.countCPU
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.countMemory
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.getNamespaces
import static com.ericsson.orchestration.mgmt.libs.VnfmKubernetes.getResourceAge
import static com.ericsson.orchestration.mgmt.libs.VnfmLDAP.nameConvenient
import static com.ericsson.orchestration.mgmt.libs.VnfmLDAP.getDisplayName


/* Stage for Discover Kubernetes Cluster. Use:
- VARs:
    WHITE_NAMESPACE_LIST
    WHITE_NAMESPACE_LABEL
    SKIP_NAMESPACE_LABEL
    LDAP_HOST
- Args:
    cluster(require): type String; Name of the Kubernetes Cluster
*/
def ClusterDiscover(Map Args) {
    String stageName = 'ClusterDiscover'
    Map argsList = [cluster: [value: Args['cluster'], type: 'string']]
    ArrayList namespaces
    List<Map> result = new ArrayList<Map>()


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage(Args['cluster'] + ': Discover') {
        withCredentials([file(credentialsId: Args['cluster'],
                              variable: 'KUBE_CONFIG_PATH')]) {
            println('INFO: Get namespaces...')
            namespaces = getNamespaces( cluster: KUBE_CONFIG_PATH,
                                        this)

            println('INFO: Remove white list namespaces...')
            namespaces.removeAll(readYaml(text: env.WHITE_NAMESPACE_LIST))

            println('INFO: Remove namespace with White labels...')
            for(String label in readYaml(text: env.WHITE_NAMESPACE_LABEL)) {
                ArrayList whiteNamespaces = getNamespaces(cluster: KUBE_CONFIG_PATH,
                                                          label: label,
                                                          this)
                namespaces.removeAll(whiteNamespaces)
            }

            println('INFO: Prepare namespace list...')
            for(String ns in namespaces) {
                Boolean nameConvenient = nameConvenient(name: ns,
                                                        host: env.LDAP_HOST,
                                                        this)
                Boolean skipLabelExist = checkNamespace(namespace: ns,
                                                        cluster: KUBE_CONFIG_PATH,
                                                        label: env.SKIP_NAMESPACE_LABEL,
                                                        this)
                Boolean willBeDelete = (nameConvenient != true || skipLabelExist != true) ? true : false
                ArrayList deploysList = checkHelmDeploy(namespace: ns,
                                                        cluster: KUBE_CONFIG_PATH,
                                                        this)
                String age = getResourceAge(name: ns,
                                            cluster: Args['cluster'],
                                            this)

                // Skip to tag namespace to delete, when age < 1 day(24h)
                String day = age.contains('d') ? age.split('d')[0] : '0'
                String hour = age.contains('h') ? age.minus(day + 'd').split('h')[0] : '0'
                if(day == '0' && hour.toInteger() < 24) {
                    willBeDelete = false
                }

                Map nsInfo = [:]
                nsInfo["Namespace"] = ns
                nsInfo["Namespace's owner"] = getDisplayName( name: ns,
                                                              host: env.LDAP_HOST,
                                                              this)
                nsInfo["Count of Requests CPU"] = countCPU( namespace: ns,
                                                            cluster: KUBE_CONFIG_PATH,
                                                            type: 'requests',
                                                            this)
                nsInfo["Count of Limits CPU"] = countCPU( namespace: ns,
                                                          cluster: KUBE_CONFIG_PATH,
                                                          type: 'limits',
                                                          this)
                nsInfo["Count of Requests Memory(Gi)"] = countMemory( namespace: ns,
                                                                      cluster: KUBE_CONFIG_PATH,
                                                                      type: 'requests',
                                                                      this)
                nsInfo["Count of Limits Memory(Gi)"] = countMemory( namespace: ns,
                                                                    cluster: KUBE_CONFIG_PATH,
                                                                    type: 'limits',
                                                                    this)
                nsInfo["List of Helm deploys"] = deploysList ? deploysList : '-'
                nsInfo["Age"] = age
                nsInfo["Naming Conventions Followed"] = nameConvenient
                nsInfo["Skip Label Exist"] = skipLabelExist
                nsInfo["Will Be Deleted"] = willBeDelete

                result.add(nsInfo)
            }
        }

        println('INFO: Prepare and archive result...')
        writeYaml(file: Args['cluster'] + '.yaml',
                  data: result,
                  overwrite: true)
        archiveArtifacts( artifacts: cluster + '.yaml',
                          allowEmptyArchive: true)
    }
}


/* Stage for Upload Cluster content to Wiki. Use:
- VARs:
    WIKI_URL
- Args:
    cluster(require): type String; Name of the Kubernetes Cluster
    fileName(require): type String; Name of the file with upload content
*/
def ClusterWikiUpload(Map Args) {
    String stageName = 'ClusterWikiUpload'
    Map argsList = [cluster: [value: Args['cluster'], type: 'string'],
                    fileName: [value: Args['fileName'], type: 'string']]
    def content


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage(Args['cluster'] + ': Upload to Confluence') {
        content = readYaml(file: Args['fileName'])

        if(content.size() > 0) {
            println('Upload to Confluence...')
            wikiUpload( url: env.WIKI_URL,
                        name: Args['cluster'],
                        body: tableHTML(content),
                        this)
        }
    }
}


/* Stage for Test Kubernetes Compatibility. Use:
- VARs:
    KUBE_VERSION_FILE_PATH
    GLOBAL_HELM_FOLDER
    PROJECT_NAME
- Args:
    dir(require): type String; Directory of the project code
    tests(require): type String; Path to the tests scipts
    templates(require): type String; Path to the templates
*/
def CompatibilityTests(Map Args) {
    stage('Kubernetes Compatibility Tests') {
        String stageName = 'CompatibilityTests'
        Map argsList = [dir: [value: Args['dir'], type: 'string'],
                        tests: [value: Args['tests'], type: 'string'],
                        templates: [value: Args['templates'], type: 'string']]
        String comm
        String dockerImage
        String k8sVersions
        String versionsFile = 'k8s-versions.list'
        String helmFolders = 'helm-scans'


        // Checking Arguments
        checkArgs(argsList, stageName, this)


        dir(Args['dir']) {
            comm = """  mkdir scripts
                      | cp ${Args['tests']} scripts/
                      | mkdir -p templates/precode
                      | cp ${Args['templates']} templates/precode/""".stripMargin()
            sh(comm)

            println('Get supported versions...')
            comm = "scripts/print_supported_k8s_versions.sh ${env.KUBE_VERSION_FILE_PATH}"
            k8sVersions = sh(script: comm, returnStdout: true).trim()
            writeFile(text: k8sVersions, file: versionsFile)

            println('Prepare Helm template for scan...')
            dockerImage = getDockerImagePath('kubesec')
            comm = """docker run --init --rm \\
                      | --volume ${pwd()}:/app:rw \\
                      | --workdir /app \\
                      | --user \$(id -u):\$(id -g) \\
                      | ${dockerImage} \\
                      | ./scripts/generate_helm_templates_for_supported_k8s.sh \\
                      | \$(ls ${env.GLOBAL_HELM_FOLDER}/${env.PROJECT_NAME}/*.tgz) \\
                      | templates/precode/site_values.yaml \\
                      | ${versionsFile} \\
                      | ${helmFolders}""".stripMargin()
            sh(comm)

            println('Run deprecation test...')
            dockerImage = getDockerImagePath('deprek8ion')
            comm = """docker run --init --rm \\
                      | --volume ${pwd()}:/app:rw \\
                      | --workdir /app \\
                      | --user \$(id -u):\$(id -g) \\
                      | --entrypoint ./scripts/deprek8ion.sh \\
                      | ${dockerImage} \\
                      | ${versionsFile} \\
                      | ${helmFolders}""".stripMargin()
            sh(comm)
        }
    }
}


/* Stage for Pod Security Standard enforcement. Use:
- Args:
    cluster(require): type String; Cluster name from jenkins secrets
    namespace(require): type String; Namespace for pod security profile enforcement
    profile(require): type String; Pod security standard [privileged, baseline, restricted]
    mode(require): type String; Pod security standard mode [audit, warn, enforce]
*/
def EnforcePSS(Map Args) {
    stage('Enforce Pod Security standard') {
        String stageName = 'Enforce Pod Security Standard'
        Map argsList = [cluster: [value: Args['cluster'], type: 'string'],
                        namespace: [value: Args['namespace'], type: 'string'],
                        profile: [value: Args['profile'], type: 'string'],
                        mode: [value: Args['mode'], type: 'string']]
        String comm


        // Checking Arguments
        checkArgs(argsList, stageName, this)


        println("Setting ${profile} Pod Security Standard with mode ${Args['mode']} for ns ${Args['namespace']}, cluster ${Args['cluster']}")
        withCredentials([file(credentialsId: Args['cluster'],
                              variable: 'KUBE_CONFIG_PATH')]) {
            comm = """kubectl \\
                      | --kubeconfig=\$KUBE_CONFIG_PATH \\
                      | label \\
                      | --overwrite ns ${Args['namespace']} \\
                      | pod-security.kubernetes.io/${Args['mode']}=${Args['profile']}""".stripMargin()
            sh(comm)
        }
    }
}


/* Stage to enable logLevel debug in image registry. Use:
- Args:
    cluster(require): type String; Cluster name
    namespaces(require): type String; Namespaces

*/
def RegistryDebug(Map Args) {
    String stageName = 'RegistryDebug'
    Map argsList = [cluster: [value: Args['cluster'], type: 'string'],
                    namespace: [value: Args['namespace'], type: 'string']]
    String comm

    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Enable registry debug') {
        withCredentials([file(credentialsId: Args['cluster'],
                              variable: 'KUBE_CONFIG_PATH')]) {
            println('Update lcm-container-registry config file')
            comm = """kubectl \\
                      | --kubeconfig \$KUBE_CONFIG_PATH \\
                      | --namespace ${Args['namespace']} \\
                      | get cm eric-lcm-container-registry-registry -o yaml | \\
                      | sed 's/level: .*/level: debug/; s/formatter: .*/formatter: text/' | \\
                      | kubectl --kubeconfig \$KUBE_CONFIG_PATH --namespace ${Args['namespace']} apply -f -""".stripMargin()
            sh(comm)

            println('Change loging vars in lcm-container-registry')
            comm = """kubectl \\
                      | --kubeconfig \$KUBE_CONFIG_PATH \\
                      | --namespace ${Args['namespace']} \\
                      | set env deployment eric-lcm-container-registry-registry \\
                      | -e LOG_LEVEL=debug \\
                      | -e LOG_SCHEMA=none""".stripMargin()
            sh(comm)
        }
    }
}

return this