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
import static com.ericsson.orchestration.mgmt.libs.VnfmJenkins.getCredential
import static com.ericsson.orchestration.mgmt.libs.VnfmUtils.generateSSL


/* Stage for Preparing to Deploy. Use:
- VARs:
    CLUSTER
    CERT_DIR
    SSL_SAMPLE
    IAM_TLS_SECRET
    REGISTRY_TLS_SECRET
    VNFM_TLS_SECRET
    HELM_TLS_SECRET
    GAS_TLS_SECRET
    IAM_USERS_SECRET
    IDAM_USERNAME
    DEPLOY_USER
    DEPLOY_PASSWORD
    PG_USERNAME
    PG_PASSWORD
    DOCKER_REGISTRY_SECRET
    DOCKER_SERVER
    DOCKER_SERVER_SW
    IAM_CACERT_SECRET
    CONTAINER_REGISTRY_SECRET
    IAM_ONBOARDING_SECRET
    WFS_REGISTRY_SECRET
    CONTAINER_REGISTRY_CREDENTIALS_SECRET(for:  am-onboarding-service,
                                                am-common-wfs)
    HELM_REGISTRY_CREDENTIALS_SECRET(for: am-onboarding-service,
                                          am-common-wfs,
                                          vnfm-orchestrator)
    HELM_REGISTRY_USER(for: am-onboarding-service,
                            am-common-wfs,
                            vnfm-orchestrator)
    HELM_REGISTRY_PASSWORD(for: am-onboarding-service,
                                am-common-wfs,
                                vnfm-orchestrator)
- Job's ENVs:
    HOST_DOCKER
    HOST_HELM
    HOST_IAM
    HOST_VNFM
    HOST_GAS
    NAMESPACE
- Args:
    project(require): type String; Name of the project
    skip: type Boolean; If true to skip the current stage; default is false
*/
def Prepare(Map Args) {
    String stageName = 'Prepare'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    skip: [value: Args['skip'], type: 'bool', require: false]]
    String comm
    String sslSample = readFile(file: env.SSL_SAMPLE)


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false


    stage('Prepare Deploy') {
        if(Args['skip']) {
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            dir(env.CERT_DIR) {
                println('Generate SSL certificates...')
                generateSSL(name: env.HOST_DOCKER,
                            sample: sslSample,
                            this)
                generateSSL(name: env.HOST_HELM,
                            sample: sslSample,
                            this)
                generateSSL(name: env.HOST_IAM,
                            sample: sslSample,
                            this)
                generateSSL(name: env.HOST_VNFM,
                            sample: sslSample,
                            this)
                generateSSL(name: env.HOST_GAS,
                            sample: sslSample,
                            this)

                println('Check SSL cerificates...')
                comm = 'ls -ls'
                sh(comm)
            }

            withCredentials([file(credentialsId: env.CLUSTER,
                                  variable: 'KUBE_CONFIG_PATH')]) {
                println('Create namespace...')
                comm = """kubectl create namespace ${env.NAMESPACE} \\
                          | --kubeconfig=\$KUBE_CONFIG_PATH
                          |kubectl label namespace ${env.NAMESPACE} \\
                          | ${env.NAMESPACE_LABEL} \\
                          | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                sh(comm)

                println('Create IAM service secret...')
                comm = """kubectl create secret tls ${env.IAM_TLS_SECRET} \\
                          | --key ${env.CERT_DIR}/${env.HOST_IAM}.key \\
                          | --cert ${env.CERT_DIR}/${env.HOST_IAM}.crt \\
                          | --namespace ${env.NAMESPACE} \\
                          | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                sh(comm)

                println('Create Docker Registry service secret...')
                comm = """kubectl create secret tls ${env.REGISTRY_TLS_SECRET} \\
                          | --key ${env.CERT_DIR}/${env.HOST_DOCKER}.key \\
                          | --cert ${env.CERT_DIR}/${env.HOST_DOCKER}.crt \\
                          | --namespace ${env.NAMESPACE} \\
                          | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                sh(comm)

                println('Create EVNFM service secret...')
                comm = """kubectl create secret tls ${env.VNFM_TLS_SECRET} \\
                          | --key ${env.CERT_DIR}/${env.HOST_VNFM}.key \\
                          | --cert ${env.CERT_DIR}/${env.HOST_VNFM}.crt \\
                          | --namespace ${env.NAMESPACE} \\
                          | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                sh(comm)

                println('Create Helm service secret...')
                comm = """kubectl create secret tls ${env.HELM_TLS_SECRET} \\
                          | --key ${env.CERT_DIR}/${env.HOST_HELM}.key \\
                          | --cert ${env.CERT_DIR}/${env.HOST_HELM}.crt \\
                          | --namespace ${env.NAMESPACE} \\
                          | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                sh(comm)

                println('Create GAS service secret...')
                comm = """kubectl create secret tls ${env.GAS_TLS_SECRET} \\
                          | --key ${env.CERT_DIR}/${env.HOST_GAS}.key \\
                          | --cert ${env.CERT_DIR}/${env.HOST_GAS}.crt \\
                          | --namespace ${env.NAMESPACE} \\
                          | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                sh(comm)

                println('Create IAM CA Registry secret...')
                withCredentials([file(credentialsId: getCredential('ca-crt'),
                                      variable: 'CA_CRT')]) {
                    comm = """kubectl create secret generic ${env.IAM_CACERT_SECRET} \\
                              | --from-file=tls.crt=${env.CA_CRT} \\
                              | --namespace ${env.NAMESPACE} \\
                              | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                    sh(comm)
                }

                withCredentials([usernamePassword(credentialsId: getCredential('ldap'),
                                                  usernameVariable: 'USER_KUBE',
                                                  passwordVariable: 'PASSWORD_KUBE')]) {
                    println('Generate Docker config...')
                    comm = """docker login ${env.DOCKER_SERVER} \\
                              | --username \$USER_KUBE \\
                              | --password \$PASSWORD_KUBE
                              |docker login ${env.DOCKER_SERVER_SW} \\
                              | --username \$USER_KUBE \\
                              | --password \$PASSWORD_KUBE""".stripMargin()
                    sh(comm)

                    println('Create Docker Registry secret...')
                    comm = """kubectl create secret generic ${env.DOCKER_REGISTRY_SECRET} \\
                              | --from-file=.dockerconfigjson=${env.DOCKER_CONFIG}/config.json \\
                              | --type=kubernetes.io/dockerconfigjson \\
                              | --namespace ${env.NAMESPACE} \\
                              | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                    sh(comm)

                    println('Remove Docker config dir...')
                    comm = "rm -rf ${env.DOCKER_CONFIG}"
                    sh(comm)
                }
            }

            switch(Args['project']) {
                case 'am-onboarding-service':
                    withCredentials([file(credentialsId: env.CLUSTER,
                                          variable: 'KUBE_CONFIG_PATH')]) {
                        println('Create IAM USERS SECRET secret...')
                        comm = """kubectl create secret generic ${env.IAM_USERS_SECRET} \\
                                  | --from-literal=kcadminid=${env.IDAM_USERNAME} \\
                                  | --from-literal=kcpasswd='${env.DEPLOY_PASSWORD}' \\
                                  | --from-literal=pguserid=${PG_USERNAME} \\
                                  | --from-literal=pgpasswd=${PG_PASSWORD} \\
                                  | --namespace ${env.NAMESPACE} \\
                                  | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                        sh(comm)

                        println('Create Container Registry secret...')
                        comm = """htpasswd -cBb htpasswd ${env.DEPLOY_USER} '${env.DEPLOY_PASSWORD}'
                                  |kubectl create secret generic ${env.CONTAINER_REGISTRY_SECRET} \\
                                  | --from-file=htpasswd=./htpasswd \\
                                  | --namespace ${env.NAMESPACE} \\
                                  | --kubeconfig=\$KUBE_CONFIG_PATH
                                  | rm -f ./htpasswd""".stripMargin()
                        sh(comm)

                        // IAM_ONBOARDING_SECRET need to revers compatibility after EO-173143 will be implemeted
                        println('Create IAM Onboarding secret...')
                        comm = """kubectl create secret generic ${env.IAM_ONBOARDING_SECRET} \\
                                  | --from-literal=userid=${env.DEPLOY_USER} \\
                                  | --from-literal=userpasswd=${env.DEPLOY_PASSWORD} \\
                                  | --namespace ${env.NAMESPACE} \\
                                  | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                        sh(comm)

                        println('Create Container Registry Credentials secret...')
                        comm = """kubectl create secret generic ${env.CONTAINER_REGISTRY_CREDENTIALS_SECRET} \\
                                  | --from-literal=url=${env.HOST_DOCKER} \\
                                  | --from-literal=userid=${env.DEPLOY_USER} \\
                                  | --from-literal=userpasswd=${env.DEPLOY_PASSWORD} \\
                                  | --from-literal=tls-verify=false \\
                                  | --from-literal=read-only=false \\
                                  | --namespace ${env.NAMESPACE} \\
                                  | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                        sh(comm)

                        println('Create Helm Registry Credentials secret...')
                        comm = """kubectl create secret generic ${env.HELM_REGISTRY_CREDENTIALS_SECRET} \\
                                  | --from-literal=oci=false \\
                                  | --from-literal=url=http://eric-lcm-helm-chart-registry.${env.NAMESPACE}.svc.cluster.local:8080 \\
                                  | --from-literal=userid=${env.HELM_REGISTRY_USER} \\
                                  | --from-literal=userpasswd=${env.HELM_REGISTRY_PASSWORD} \\
                                  | --from-literal=tls-verify=false \\
                                  | --from-literal=read-only=false \\
                                  | --namespace ${env.NAMESPACE} \\
                                  | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                        sh(comm)
                    }
                break
                case 'am-common-wfs':
                    withCredentials([file(credentialsId: env.CLUSTER,
                                          variable: 'KUBE_CONFIG_PATH')]) {
                        println('Create IAM USERS SECRET secret...')
                        comm = """kubectl create secret generic ${env.IAM_USERS_SECRET} \\
                                  | --from-literal=kcadminid=${env.IDAM_USERNAME} \\
                                  | --from-literal=kcpasswd='${env.DEPLOY_PASSWORD}' \\
                                  | --from-literal=pguserid=${PG_USERNAME} \\
                                  | --from-literal=pgpasswd=${PG_PASSWORD} \\
                                  | --namespace ${env.NAMESPACE} \\
                                  | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                        sh(comm)

                        withCredentials([usernamePassword(credentialsId: getCredential('ldap'),
                                                          usernameVariable: 'USER_KUBE',
                                                          passwordVariable: 'PASSWORD_KUBE')]) {
                            println('Create WFS Docker Registry secret...')
                            comm = """kubectl create secret generic ${env.WFS_REGISTRY_SECRET} \\
                                      | --from-literal=url=${env.DOCKER_SERVER} \\
                                      | --from-literal=userid=\$USER_KUBE \\
                                      | --from-literal=userpasswd=\$PASSWORD_KUBE \\
                                      | --namespace ${env.NAMESPACE} \\
                                      | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                            sh(comm)
                        }

                        // IAM_ONBOARDING_SECRET need to revers compatibility after EO-173143 will be implemeted
                        println('Create IAM Onboarding secret...')
                        comm = """kubectl create secret generic ${env.IAM_ONBOARDING_SECRET} \\
                                  | --from-literal=userid=${env.DEPLOY_USER} \\
                                  | --from-literal=userpasswd=${env.DEPLOY_PASSWORD} \\
                                  | --namespace ${env.NAMESPACE} \\
                                  | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                        sh(comm)

                        println('Create Container Registry Credentials secret...')
                        comm = """kubectl create secret generic ${env.CONTAINER_REGISTRY_CREDENTIALS_SECRET} \\
                                  | --from-literal=url=${env.HOST_DOCKER} \\
                                  | --from-literal=userid=${env.DEPLOY_USER} \\
                                  | --from-literal=userpasswd=${env.DEPLOY_PASSWORD} \\
                                  | --from-literal=tls-verify=false \\
                                  | --from-literal=read-only=false \\
                                  | --namespace ${env.NAMESPACE} \\
                                  | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                        sh(comm)

                        println('Create Helm Registry Credentials secret...')
                        comm = """kubectl create secret generic ${env.HELM_REGISTRY_CREDENTIALS_SECRET} \\
                                  | --from-literal=oci=false \\
                                  | --from-literal=url=http://eric-lcm-helm-chart-registry.${env.NAMESPACE}.svc.cluster.local:8080 \\
                                  | --from-literal=userid=${env.HELM_REGISTRY_USER} \\
                                  | --from-literal=userpasswd=${env.HELM_REGISTRY_PASSWORD} \\
                                  | --from-literal=tls-verify=false \\
                                  | --from-literal=read-only=false \\
                                  | --namespace ${env.NAMESPACE} \\
                                  | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                        sh(comm)
                    }
                break
                case 'am-common-wfs-ui':
                    withCredentials([file(credentialsId: env.CLUSTER,
                                          variable: 'KUBE_CONFIG_PATH')]) {
                        println('Create IAM USERS SECRET secret...')
                        comm = """kubectl create secret generic ${env.IAM_USERS_SECRET} \\
                                  | --from-literal=kcadminid=${env.IDAM_USERNAME} \\
                                  | --from-literal=kcpasswd='${env.DEPLOY_PASSWORD}' \\
                                  | --from-literal=pguserid=${PG_USERNAME} \\
                                  | --from-literal=pgpasswd=${PG_PASSWORD} \\
                                  | --namespace ${env.NAMESPACE} \\
                                  | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                        sh(comm)
                    }
                break
                case 'vnfm-orchestrator':
                    withCredentials([file(credentialsId: env.CLUSTER,
                                          variable: 'KUBE_CONFIG_PATH')]) {
                        println('Create IAM USERS SECRET secret...')
                        comm = """kubectl create secret generic ${env.IAM_USERS_SECRET} \\
                                  | --from-literal=kcadminid=${env.IDAM_USERNAME} \\
                                  | --from-literal=kcpasswd='${env.DEPLOY_PASSWORD}' \\
                                  | --from-literal=pguserid=${PG_USERNAME} \\
                                  | --from-literal=pgpasswd=${PG_PASSWORD} \\
                                  | --namespace ${env.NAMESPACE} \\
                                  | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                        sh(comm)

                        println('Create Container Registry secret...')
                        comm = """htpasswd -cBb htpasswd ${env.DEPLOY_USER} '${env.DEPLOY_PASSWORD}'
                                  |kubectl create secret generic ${env.CONTAINER_REGISTRY_SECRET} \\
                                  | --from-file=htpasswd=./htpasswd \\
                                  | --namespace ${env.NAMESPACE} \\
                                  | --kubeconfig=\$KUBE_CONFIG_PATH
                                  | rm -f ./htpasswd""".stripMargin()
                        sh(comm)

                        println('Create Helm Registry Credentials secret...')
                        comm = """kubectl create secret generic ${env.HELM_REGISTRY_CREDENTIALS_SECRET} \\
                                  | --from-literal=oci=false \\
                                  | --from-literal=url=http://eric-lcm-helm-chart-registry.${env.NAMESPACE}.svc.cluster.local:8080 \\
                                  | --from-literal=userid=${env.HELM_REGISTRY_USER} \\
                                  | --from-literal=userpasswd=${env.HELM_REGISTRY_PASSWORD} \\
                                  | --from-literal=tls-verify=false \\
                                  | --from-literal=read-only=false \\
                                  | --namespace ${env.NAMESPACE} \\
                                  | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                        sh(comm)
                    }
                break
                default:
                    withCredentials([file(credentialsId: env.CLUSTER,
                                          variable: 'KUBE_CONFIG_PATH')]) {
                        println('Create IAM USERS SECRET secret...')
                        comm = """kubectl create secret generic ${env.IAM_USERS_SECRET} \\
                                  | --from-literal=kcadminid=${env.IDAM_USERNAME} \\
                                  | --from-literal=kcpasswd='${env.DEPLOY_PASSWORD}' \\
                                  | --from-literal=pguserid=${PG_USERNAME} \\
                                  | --from-literal=pgpasswd=${PG_PASSWORD} \\
                                  | --namespace ${env.NAMESPACE} \\
                                  | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                        sh(comm)

                        println('Create Container Registry secret...')
                        comm = """htpasswd -cBb htpasswd ${env.DEPLOY_USER} '${env.DEPLOY_PASSWORD}'
                                  |kubectl create secret generic ${env.CONTAINER_REGISTRY_SECRET} \\
                                  | --from-file=htpasswd=./htpasswd \\
                                  | --namespace ${env.NAMESPACE} \\
                                  | --kubeconfig=\$KUBE_CONFIG_PATH
                                  | rm -f ./htpasswd""".stripMargin()
                        sh(comm)

                        println('Create IAM Onboarding secret...')
                        comm = """kubectl create secret generic ${env.IAM_ONBOARDING_SECRET} \\
                                  | --from-literal=userid=${env.DEPLOY_USER} \\
                                  | --from-literal=userpasswd=${env.DEPLOY_PASSWORD} \\
                                  | --namespace ${env.NAMESPACE} \\
                                  | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                        sh(comm)

                        withCredentials([usernamePassword(credentialsId: getCredential('ldap'),
                                                          usernameVariable: 'USER_KUBE',
                                                          passwordVariable: 'PASSWORD_KUBE')]) {
                            println('Create WFS Docker Registry secret...')
                            comm = """kubectl create secret generic ${env.WFS_REGISTRY_SECRET} \\
                                      | --from-literal=url=${env.DOCKER_SERVER} \\
                                      | --from-literal=userid=\$USER_KUBE \\
                                      | --from-literal=userpasswd=\$PASSWORD_KUBE \\
                                      | --namespace ${env.NAMESPACE} \\
                                      | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                            sh(comm)
                        }
                    }
                break
            }
        }
    }
}


/* Stage for Deploy Application. Use:
- VARs:
    GLOBAL_HELM_FOLDER
    IAM_USERS_SECRET
    DOCKER_REGISTRY_SECRET
    CLUSTER
    HELM_REGISTRY_USER(for: am-onboarding-service)
    HELM_REGISTRY_PASSWORD(for: am-onboarding-service)
- Job's ENVs:
    PROJECT_NAME
    NAMESPACE
    ICCR
    INGRESS_TYPE
- Args:
    name(require): type String; Name of the project
    chart: type String; Name of the Helm Chart; default is env.CHART_NAME
    skip: type Boolean; If true to skip the current stage; default is false
*/
def Deploy(Map Args) {
    String stageName = 'Deploy'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    chart: [value: Args['chart'], type: 'string', require: false],
                    skip: [value: Args['skip'], type: 'bool', require: false]]
    String comm
    String helmFolder
    String args = """ --set global.pullSecret=${env.DOCKER_REGISTRY_SECRET} \\
                    | --set global.registry.imagePullPolicy=Always""".stripMargin()
    String helmArgs = ''
    String filePath


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['chart'] = Args.containsKey('chart') ? Args['chart'] : env.CHART_NAME
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false


    stage('Deploy') {
        if(Args['skip']) {
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            dir(Args['name']) {
                filePath = 'charts/' + Args['chart'] + '.yaml'

                println('Check Deployment config file...')
                if(env.HELM_ARGS_FILE && fileExists(env.HELM_ARGS_FILE)) {
                    helmArgs = readFile(env.HELM_ARGS_FILE)
                                                          .trim()
                                                          .split('\n')
                                                          .join(' ')
                }

                println('Check chart values file...')
                if(fileExists(filePath)) {
                    args += " -f ${filePath}"
                }

                switch(Args['name']) {
                    case 'am-common-wfs':
                        helmFolder = 'am-integration-charts/charts'
                        args += """ --set eric-am-common-wfs.dockerRegistry.secret=${env.WFS_REGISTRY_SECRET} \\
                                  | --set eric-am-common-wfs.service.type=NodePort \\
                                  | --set global.hosts.vnfm=default \\
                                  | --set global.iccrAppIngresses=${env.ICCR} \\
                                  | --set global.ingressClass=${env.INGRESS_TYPE} \\
                                  | --set global.geo-redundancy.enabled=false \\
                                  | --set global.postgresCredentials.secret=${env.IAM_USERS_SECRET} \\
                                  | --set global.security.tls.enabled=false \\
                                  | --set eric-am-onboarding-service.enabled=false \\
                                  | --set eric-lcm-container-registry.enabled=false \\
                                  | --set eric-lcm-helm-chart-registry.enabled=false \\
                                  | --set evnfm-toscao.enabled=false \\
                                  | --set eric-am-common-wfs.camunda.historyLevel=audit \\
                                  | -f am-integration-charts/charts/deploy-without-gateway-values.yaml \\
                                  | -f am-integration-charts/charts/eric-am-common-wfs.yaml""".stripMargin()
                    break
                    case 'am-common-wfs-ui':
                        helmFolder = "${env.GLOBAL_HELM_FOLDER}/${env.PROJECT_NAME}"
                        args += ' --set global.hosts.vnfm=default'
                    break
                    case 'am-onboarding-service':
                        helmFolder = 'am-integration-charts/charts'
                        args += """ --set eric-am-onboarding-service.enabled=true  \\
                                  | --set eric-am-onboarding-service.ingress.enabled=false \\
                                  | --set eric-am-onboarding-service.service.type=NodePort \\
                                  | --set eric-am-onboarding-service.onboarding.skipToscaoValidation=true \\
                                  | --set eric-am-onboarding-service.onboarding.skipCertificateValidation=false \\
                                  | --set eric-am-onboarding-service.objectStorage.enabled=true \\
                                  | --set eric-am-onboarding-service.objectStorage.host=eric-data-object-storage-mn \\
                                  | --set eric-am-onboarding-service.objectStorage.port=9000 \\
                                  | --set eric-am-onboarding-service.objectStorage.kubernetesSecretName=minio-secret \\
                                  | --set eric-am-onboarding-service.registry-tls-secret=${env.REGISTRY_TLS_SECRET} \\
                                  | --set evnfm-toscao.enabled=true \\
                                  | --set evnfm-toscao.resources.requests.memory=500Mi \\
                                  | --set evnfm-toscao.resources.requests.cpu=100m \\
                                  | --set evnfm-toscao.resources.limits.memory=600Mi \\
                                  | --set evnfm-toscao.resources.limits.cpu=250m \\
                                  | --set evnfm-toscao.postgresql.enabled=false \\
                                  | --set evnfm-toscao.postgresql.postgresDatabase=toscao \\
                                  | --set evnfm-toscao.postgresql.existingSecret.name=eric-sec-access-mgmt-creds \\
                                  | --set evnfm-toscao.postgresql.existingSecret.userKey=pguserid \\
                                  | --set evnfm-toscao.postgresql.existingSecret.pwdKey=pgpasswd \\
                                  | --set evnfm-toscao.postgresql.persistentVolumeClaim.housekeeping_threshold=90 \\
                                  | --set evnfm-toscao.existingDatabase=application-manager-postgres \\
                                  | --set credentials.kubernetesSecretName=eric-eo-database-pg-secret \\
                                  | --set global.hosts.vnfm=default \\
                                  | --set global.iccrAppIngresses=${env.ICCR} \\
                                  | --set global.ingressClass=${env.INGRESS_TYPE} \\
                                  | --set global.geo-redundancy.enabled=false \\
                                  | --set global.postgresCredentials.secret=${env.IAM_USERS_SECRET} \\
                                  | --set global.security.tls.enabled=false \\
                                  | --set application-manager-postgres.highAvailability.replicaCount=1 \\
                                  | --set application-manager-postgres.highAvailability.synchronousModeEnabled=false \\
                                  | --set eric-data-message-bus-rmq.persistence.enabled=false \\
                                  | --set eric-lcm-container-registry.ingress.ingressClass=${env.INGRESS_TYPE} \\
                                  | --set eric-lcm-container-registry.ingress.hostname=${env.HOST_DOCKER} \\
                                  | --set eric-lcm-container-registry.ingress.tls.secretName=${env.REGISTRY_TLS_SECRET} \\
                                  | --set eric-lcm-container-registry.registry.users.secret=${env.CONTAINER_REGISTRY_SECRET} \\
                                  | --set eric-lcm-helm-chart-registry.ingress.hostname=${env.HOST_HELM} \\
                                  | --set eric-lcm-helm-chart-registry.env.secret.BASIC_AUTH_USER=${env.HELM_REGISTRY_USER} \\
                                  | --set eric-lcm-helm-chart-registry.env.secret.BASIC_AUTH_PASS=${env.HELM_REGISTRY_PASSWORD} \\
                                  | --set eric-tm-ingress-controller-cr.enabled=false \\
                                  | --set eric-vnfm-orchestrator-service.toscao.enabled=false \\
                                  | --set iam.cacert.secretName=${env.IAM_CACERT_SECRET} \\
                                  | -f ${helmFolder}/deploy-without-gateway-values.yaml \\
                                  | -f ${helmFolder}/eric-am-onboarding-service.yaml""".stripMargin()
                    break
                    case 'eric-eo-lm-consumer':
                        helmFolder = "${env.GLOBAL_HELM_FOLDER}/${env.PROJECT_NAME}"
                        args += " --set database.service='eric-data-document-database-pg'"
                    break
                    case 'eric-eo-evnfm-crypto':
                        helmFolder = "${env.GLOBAL_HELM_FOLDER}/${env.PROJECT_NAME}"
                        args += ''' --set service.type=NodePort \\
                                  | --set ADDITIONAL_DEPLOY_COMMANDS=false \\
                                  | --set kms.enabled=false'''.stripMargin()
                    break
                    case 'eric-eo-evnfm-sol-agent':
                        helmFolder = "${env.GLOBAL_HELM_FOLDER}/${env.PROJECT_NAME}"
                        args += ''' --set service.type=NodePort \\
                                  | --set eric-evnfm-rbac.enabled=false \\
                                  | --set ingress.enabled=false \\
                                  | --set global.hosts.vnfm=default \\
                                  | --set ADDITIONAL_DEPLOY_COMMANDS=false'''.stripMargin()
                    break
                    case 'vnfm-orchestrator':
                        helmFolder = 'am-integration-charts/charts'
                        args += """ --set eric-vnfm-orchestrator-service.type=NodePort \\
                                  | --set eric-vnfm-orchestrator-service.toscao.enabled=false \\
                                  | --set global.hosts.vnfm=default \\
                                  | --set global.iccrAppIngresses=${env.ICCR} \\
                                  | --set global.ingressClass=${env.INGRESS_TYPE} \\
                                  | --set global.geo-redundancy.enabled=false \\
                                  | --set global.postgresCredentials.secret=${env.IAM_USERS_SECRET} \\
                                  | --set global.security.tls.enabled=false \\
                                  | --set application-manager-postgres.highAvailability.replicaCount=1 \\
                                  | --set application-manager-postgres.highAvailability.synchronousModeEnabled=false \\
                                  | --set eric-data-message-bus-rmq.persistence.enabled=false \\
                                  | --set eric-lcm-container-registry.ingress.ingressClass=${env.INGRESS_TYPE} \\
                                  | --set eric-lcm-container-registry.ingress.hostname=${env.HOST_DOCKER} \\
                                  | --set eric-lcm-container-registry.ingress.tls.secretName=${env.REGISTRY_TLS_SECRET} \\
                                  | --set eric-lcm-container-registry.registry.users.secret=${env.CONTAINER_REGISTRY_SECRET} \\
                                  | --set eric-lcm-helm-chart-registry.ingress.hostname=${env.HOST_HELM} \\
                                  | --set eric-tm-ingress-controller-cr.enabled=false \\
                                  | --set iam.cacert.secretName=${env.IAM_CACERT_SECRET} \\
                                  | --set eric-am-onboarding-service.enabled=false \\
                                  | --set evnfm-toscao.enabled=false \\
                                  | -f ${helmFolder}/deploy-without-gateway-values.yaml \\
                                  | -f ${helmFolder}/eric-vnfm-orchestrator-service.yaml""".stripMargin()
                    break
                    case 'eric-eo-batch-manager':
                        helmFolder = "${env.GLOBAL_HELM_FOLDER}/${env.PROJECT_NAME}"
                        args += """ --set apiGatewayRoute.enabled=false""".stripMargin()
                    break
                    default:
                        helmFolder = "${env.GLOBAL_HELM_FOLDER}/${env.PROJECT_NAME}"
                    break
                }

                println('Deploy ' + Args['name'] + '...')
                withCredentials([file(credentialsId: env.CLUSTER,
                                      variable: 'KUBE_CONFIG_PATH')]) {
                    comm = """helm3 install ${env.PROJECT_NAME} \\
                              | \$(ls ${helmFolder}/*.tgz) \\
                              | --wait \\
                              | --timeout 420s \\
                              | --debug \\
                              | --namespace ${env.NAMESPACE} \\
                              | --kubeconfig \$KUBE_CONFIG_PATH \\
                              | ${args} \\
                              | ${helmArgs}""".stripMargin()
                    sh(comm)
                }
            }
        }
    }
}


/* Stage for Deploy Dependency. Use:
- VARs:
    GLOBAL_HELM_FOLDER
    CLUSTER
    DEPLOY_PASSWORD(only for am-common-wfs-ui)
    DEPLOY_USER(only for am-common-wfs-ui)
    INGRESS_TYPE(only for am-common-wfs-ui)
    ICCR(only for am-common-wfs-ui)
- Job's ENVs:
    CHART_NAME
    NAMESPACE
    HOST_IAM(only for am-common-wfs-ui)
    HOST_VNFM(only for am-common-wfs-ui)
    IMAGE_VERSION(only for am-common-wfs-ui)
- Args:
    name(require): type String; Name of the project
    chart: type String; Name of the Helm Chart; default is env.CHART_NAME
    skip: type Boolean; If true to skip the current stage; default is false
*/
def DeployDependency(Map Args) {
    String stageName = 'DeployDependency'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    chart: [value: Args['chart'], type: 'string', require: false],
                    skip: [value: Args['skip'], type: 'bool', require: false]]
    String comm
    String chartName
    String args = """ --set global.pullSecret=${env.DOCKER_REGISTRY_SECRET} \\
                    | --set global.registry.imagePullPolicy=Always""".stripMargin()


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['chart'] = Args.containsKey('chart') ? Args['chart'] : env.CHART_NAME
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false


    stage('Deploy Dependency') {
        if(Args['skip']) {
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            dir(Args['name']) {
                chartName = Args['chart'] + '-dependency'

                switch(Args['name']) {
                    case 'am-common-wfs-ui':
                        args += """ --set global.iccrAppIngresses=false \\
                                  | --set global.hosts.iam=${env.HOST_IAM} \\
                                  | --set global.hosts.vnfm=${env.HOST_VNFM} \\
                                  | --set global.security.tls.enabled=false \\
                                  | --set eric-eo-api-gateway.iam.uri="https://${env.HOST_IAM}/auth/realms/master" \\
                                  | --set eric-evnfm-rbac.defaultUser.password=${env.DEPLOY_PASSWORD} \\
                                  | --set eric-evnfm-rbac.defaultUser.username=${env.DEPLOY_USER} \\
                                  | --set eric-sec-access-mgmt.ingress.hostname=${env.HOST_IAM} \\
                                  | --set eric-sec-access-mgmt.ingress.ingressClass=${env.INGRESS_TYPE} \\
                                  | --set eric-tm-ingress-controller-cr.enabled=false \\
                                  | --set idam-database-pg.highAvailability.replicaCount=1 \\
                                  | --set idam-database-pg.highAvailability.synchronousModeEnabled=false \\
                                  | --set images.${Args['chart']}-dev-backend.tag=${env.IMAGE_VERSION} \\
                                  | --set ingress.iccr=${env.ICCR}""".stripMargin()
                    break
                }

                println('Deploy dependent Helm chart for ' + Args['chart'] + '...')
                withCredentials([file(credentialsId: env.CLUSTER,
                                      variable: 'KUBE_CONFIG_PATH')]) {
                    comm = """helm3 install ${chartName} \\
                              | ${env.GLOBAL_HELM_FOLDER}/${chartName}/*.tgz \\
                              | --wait \\
                              | --timeout 420s \\
                              | --debug \\
                              | --namespace ${env.NAMESPACE} \\
                              | --kubeconfig \$KUBE_CONFIG_PATH \\
                              | ${args}""".stripMargin()
                    sh(comm)
                }
            }
        }
    }
}


/* Stage for ENM stub. Use:
- VARs:
    CLUSTER
    ENM_STUB_NAME
- Job's ENVs:
    NAMESPACE
    ENM_STUB_IP
- Args:
    name(require): type String; Name of the helm release
    helmRepo(require): type String; Helm repo URL
    skip: type Boolean; If true to skip the current stage; default is false
*/
def DeployEnmStub(Map Args) {
    String stageName = 'DeployEnmStub'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    helmRepo: [value: Args['helmRepo'], type: 'string'],
                    skip: [value: Args['skip'], type: 'bool', require: false]]
    String comm


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false


    stage('Install ENM Stub') {
        if(Args['skip']) {
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            withCredentials([ file( credentialsId: env.CLUSTER,
                                    variable: 'KUBE_CONFIG_PATH')]) {
                println('Get current context...')
                comm = """kubectl config current-context \\
                          | --kubeconfig=\$KUBE_CONFIG_PATH""".stripMargin()
                sh(comm)

                withEnv(['HELM_CACHE_HOME=./.cache/helm',
                         'HELM_CONFIG_HOME=./.config/helm']){
                    println('Add helm repo...')
                    comm = "helm3 repo add ${Args['name']} ${Args['helmRepo']}"
                    sh(comm)

                    println('Helm fetch latest...')
                    comm = "helm3 fetch ${Args['name']}/${Args['name']} --devel"
                    sh(comm)
                }

                println('Deploy ENM stub...')
                comm = """helm3 install ${Args['name']} \\
                          | ./${Args['name']}*.tgz \\
                          | --wait \\
                          | --timeout 420s \\
                          | --debug \\
                          | --namespace ${env.NAMESPACE} \\
                          | --set service.loadBalancerIP=${env.ENM_STUB_IP} \\
                          | --kubeconfig \$KUBE_CONFIG_PATH""".stripMargin()
                sh(comm)

                println('Create enm-secret...')
                comm = """kubectl create secret generic enm-secret \\
                          | --from-literal=enm-scripting-ip=${env.ENM_STUB_NAME} \\
                          | --from-literal=enm-scripting-username=enm \\
                          | --from-literal=enm-scripting-password='enm123!' \\
                          | --from-literal=enm-scripting-connection-timeout=20000 \\
                          | --from-literal=enm-scripting-ssh-port=22 \\
                          | --namespace ${env.NAMESPACE} \\
                          | --kubeconfig \$KUBE_CONFIG_PATH""".stripMargin()
                sh(comm)
            }
        }
    }
}


/* Stage for Genarate SSL Certificates. Use:
- Args:
    name(require): type String; Root name of the generated certificates
    type(require); type String; Type of the generated certificates
*/
def GenerateSSL(Map Args) {
    String stageName = 'GenerateSSL'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    type: [value: Args['type'], type: 'string']]
    String sslSample = readFile(file: env.SSL_SAMPLE)
    String comm


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Genarate SSL Certificates') {
        dir(env.CERT_DIR) {
            switch(Args['type']) {
                case 'self-signed':
                    println('Generate SSL certificates...')
                    generateSSL(name: env.HOST_DOCKER,
                                sample: sslSample,
                                this)
                    generateSSL(name: env.HOST_DOCKER_REGISTRY,
                                sample: sslSample,
                                this)
                    generateSSL(name: env.HOST_GAS,
                                sample: sslSample,
                                this)
                    generateSSL(name: env.HOST_GR,
                                sample: sslSample,
                                this)
                    generateSSL(name: env.HOST_HELM,
                                sample: sslSample,
                                this)
                    generateSSL(name: env.HOST_IAM,
                                sample: sslSample,
                                this)
                    generateSSL(name: env.HOST_VNFM,
                                sample: sslSample,
                                this)

                    println('Clean certificate directory...')
                    comm = '''  rm -f *.conf
                              | rm -f *.csr'''.stripMargin()
                    sh(comm)

                    println('Prepare Intermediate CA...')
                    withCredentials([ file( credentialsId: getCredential('ca-crt'),
                                            variable: 'CRT'),
                                      file( credentialsId: getCredential('ca-key'),
                                            variable: 'KEY')]) {
                        comm = '''  cat \$CRT > intermediate-ca.crt
                                  | cat \$KEY >> intermediate-ca.crt'''.stripMargin()
                        sh(comm)
                    }

                    println('Prepare archive with certificates...')
                    comm = """zip ${Args['name']}.zip \\
                              | *""".stripMargin()
                    sh(comm)

                    println('Check SSL cerificates...')
                    comm = 'ls -lsh'
                    sh(comm)

                    println('Save certificates artifact...')
                    archiveArtifacts( artifacts: Args['name'] + '.zip',
                                      allowEmptyArchive: true)

                break
            }
        }
    }
}

return this