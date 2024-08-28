#!/usr/bin/env bash

# set eo-helmfile version example "2.18.0-66"
EO_HELMFILE_VERSION="$2"
# do not change this
SIGNUM=$(whoami)
# change this for alternative deployment e.g. "$SIGNUM"-<text>-ns
NAMESPACE="$SIGNUM"-ns
CLUSTER_ROLE_HELMFILE=2.7.0-153
NELS_HELMFILE=2.11.0-138
SM_HELMFILE=2.19.0-165
ICCR_NAME="iccr"
ICCR_GLOBAL="ingressClass: $ICCR_NAME"
########################################
#   No edits below this line
########################################
if kubectl get ns | grep $NAMESPACE  > /dev/null; then
  ENV_NAME=$(kubectl get ns $NAMESPACE --no-headers -o custom-columns=":metadata.labels.envName")
fi

case $(kubectl config current-context) in
*haber002)
  DOMAIN="ews.gic.ericsson.se"
  CLUSTER="$(kubectl config current-context)-$ENV_NAME"
  ;;
*hart066)
  DOMAIN="ews.gic.ericsson.se"
  CLUSTER="$(kubectl config current-context)-iccr"
  ;;
*hart070)
  DOMAIN="ews.gic.ericsson.se"
  CLUSTER="$ENV_NAME-$(kubectl config current-context)"
  ;;
*)
  DOMAIN="rnd.gic.ericsson.se"
  CLUSTER="$(kubectl config current-context)"
  ;;
esac

CLUSTER=${CLUSTER#*@}
LB_IP=$(curl test.$CLUSTER.$DOMAIN -vs 2>&1 | grep -Eom1 "([0-9]{1,3}[\.]){3}[0-9]{1,3}")
HELMFILE_REPOSITORIES_FILE="eric-eo-helmfile/repositories.yaml"
REGISTRY_CREDENTIALS_USER="usertest"
REGISTRY_CREDENTIALS_PASS="passtest"
WORKING_DIR="./certificates"
USER="vnfm"
PASSWORD="ciTesting123!"
ERIC_PASSWORD="Ericsson123!"
CRD_NAMESPACE="eric-crd-ns"
SFTP_PASSWORD="C.d5[j8Z,g-h#i]Y1!"
export GERRIT_USERNAME="cvnfmadm10"
export GERRIT_PASSWORD="SNWVb8WyASUmhQ7SEx?hVpHR"
GLOBAL_REGISTRY_URL="armdocker.rnd.ericsson.se"
HOST_DOC_REG="docker-registry.$SIGNUM.$CLUSTER.$DOMAIN"
HOST_HELM_REG="helm-chart-registry.$SIGNUM.$CLUSTER.$DOMAIN"
HOST_IDAM="iam-service.$SIGNUM.$CLUSTER.$DOMAIN"
HOST_VNFM="vnfm.$SIGNUM.$CLUSTER.$DOMAIN"
HOST_GAS="gas.$SIGNUM.$CLUSTER.$DOMAIN"
HOST_GR="gr.$SIGNUM.$CLUSTER.$DOMAIN"
HOST_NELS="nelsaas-vnf2-thrift.sero.gic.ericsson.se"
WORKING_DIR="./certificates"
VALUES_FILE="site_values.yaml"
HELM_RELEASENAME="$SIGNUM-eric-eo-evnfm"
SELF_SIGN_KEY="$WORKING_DIR/intermediate-ca.key"
SELF_SIGN_CRT="$WORKING_DIR/intermediate-ca.crt"
CI_SCRIPT_TOOL_IMAGE="armdocker.rnd.ericsson.se/proj-eric-oss-drop/eric-oss-ci-scripts:latest"
DEPLOYMENT_MANAGER_IMAGE="armdocker.rnd.ericsson.se/proj-eric-oss-drop/eric-oss-deployment-manager"
CTRL_BRO_PVC_SIZE="20Gi"
CONTAINER_REGISTRY_PVC_SIZE="50Gi"
PG_BRA_STORAGE_REQUESTS=
PG_BRA_STORAGE_LIMITS="12Gi" # must be not less as APPLICATION_MANAGER_POSTGRES_PVC_SIZE * 1.5
PG_BRA_STORAGE_REQUESTS="10Gi" # must be not less as APPLICATION_MANAGER_POSTGRES_PVC_SIZE * 1.2
NELS_ENABLED="true"
ARTIFACTORY_PATH="https://arm.seli.gic.ericsson.se/artifactory"
EO_HELMFILE_REPO="$ARTIFACTORY_PATH/proj-eo-helm"
RUN_DEPLOYMENT_MANAGER="docker run --init --rm  --user $(id -u):$(id -g) --volume $(pwd):/workdir --volume /etc/hosts:/etc/hosts --group-add 512 --volume /var/run/docker.sock:/var/run/docker.sock --workdir $(pwd) $DEPLOYMENT_MANAGER_IMAGE"
CI_TOOL_SCRIPT="docker run --init --rm --user $(id -u):$(id -g) --volume $(pwd):/ci-scripts/output-files --volume $(pwd):$(pwd) --workdir $(pwd) -e $GERRIT_USERNAME -e $GERRIT_PASSWORD $CI_SCRIPT_TOOL_IMAGE script_executor"
AM_PACKEGE_MANAGER="armdocker.rnd.ericsson.se/proj-am/releases/eric-am-package-manager:latest"

# Templates for intermediate-ca certificate
SELF_SIGN_CRT_TLP=$(cat <<-END
-----BEGIN CERTIFICATE-----
MIIDXjCCAkYCCQD0PDbGCtaOyzANBgkqhkiG9w0BAQsFADBwMRkwFwYDVQQDDBBT
ZWxmU2lnbmVkUm9vdENBMREwDwYDVQQKDAhFcmljc3NvbjELMAkGA1UECwwCSVQx
EjAQBgNVBAcMCVN0b2NraG9sbTESMBAGA1UECAwJU3RvY2tob2xtMQswCQYDVQQG
EwJTRTAgFw0yMTExMTkxNzM5MzFaGA8yMDUxMTExMjE3MzkzMVowcDEZMBcGA1UE
AwwQU2VsZlNpZ25lZFJvb3RDQTERMA8GA1UECgwIRXJpY3Nzb24xCzAJBgNVBAsM
AklUMRIwEAYDVQQHDAlTdG9ja2hvbG0xEjAQBgNVBAgMCVN0b2NraG9sbTELMAkG
A1UEBhMCU0UwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDMJBdwN4ED
vF8CWPzgvjabZqVS3ftaUzolSqEGD7yvlY0j2ai7w/5+Y+YOJ5rgpHqDyDZ+ntvS
bGdjHrpnE9Tx0CcPXmHe+MSxqrOEqx1cbLLi80MxPjujdId2cup22aPdcXhh71+i
sVuYzHpzdRE4/LKabN8AnPfv1bruRax31O49dIbAq+RUj4cYMEZCcZNid316rwsv
ymzwNpbFRlsABfimwZV/2PPhBoWxGqk5mCz93pYnronQKLar9FtttRUQAxI5R5y2
wLfLWfrgpwGwd6XEN1aZJX/CYx/3QHZ+t1oe/fBcHpnzEVtdMMM3filgIJSeSVO0
tIq8/nzAAiLdAgMBAAEwDQYJKoZIhvcNAQELBQADggEBALvoW28xdNnNleuApIlg
d+lXKYnavVY/tBHg8q3n+0OQc4wJ7BoMIbJhvts2zwxXWwkLquvKnc/NfapPSEEC
8Mly2bmgmF2RdAc5s/Ojz/lzdXEvJFa93/0KTlTlO4Of/6kv7XJ1Q3rA2ybLGBSi
SZVTYl65sEdlGj1DGTwTBOG49TupFcJ6rK/BtyWFOnEAFpnPXYh2GlB+WTsUVFqn
RZ5lLK+EtncTu3HE5WBSkKHhFxbV48dTOP2SSaDTVjjLlbzTEWvj5BOlFYTxPsTB
pWvyN2AUEWG4b+Ez7vxz7ccRnHebjl5uJrztQpXS1TSG0mFV2q7+eQcqsSCLyORv
a0g=
-----END CERTIFICATE-----
END
)

SELF_SIGN_KEY_TLP=$(cat <<-END
-----BEGIN PRIVATE KEY-----
MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDMJBdwN4EDvF8C
WPzgvjabZqVS3ftaUzolSqEGD7yvlY0j2ai7w/5+Y+YOJ5rgpHqDyDZ+ntvSbGdj
HrpnE9Tx0CcPXmHe+MSxqrOEqx1cbLLi80MxPjujdId2cup22aPdcXhh71+isVuY
zHpzdRE4/LKabN8AnPfv1bruRax31O49dIbAq+RUj4cYMEZCcZNid316rwsvymzw
NpbFRlsABfimwZV/2PPhBoWxGqk5mCz93pYnronQKLar9FtttRUQAxI5R5y2wLfL
WfrgpwGwd6XEN1aZJX/CYx/3QHZ+t1oe/fBcHpnzEVtdMMM3filgIJSeSVO0tIq8
/nzAAiLdAgMBAAECggEBAIcgmNcyFldXuHhAWVuW7WSeZG7e+4OFteZ7aO0vO5Hq
Z5vEdxmbGfmlvOG/u5hZp7NVsyTLmOzHzwPgkjiq+vj59PEKY7SJbQHB4cS+09eb
KCpsJh0Reb6v4v84ABWd6QcrFimVnvN9fQk+yQtmAXl8Y+kuicrJHKGIE42nVwuW
GxKpB3Ys7h881uGT/YAZc01UNVoGtBtE/VnHfILEFcFHZbuCnz899sI6p+yiXho8
Gv7WjW73zNxBo0J1syF8B5Kt37OTBe2z29jZ7BPPhxJBIDF7RkROcXB4YZmRB35H
qkD4LC3XO92S7tccJvXgvp4KOfCnZItuWuSAUkRfu80CgYEA7sVl58NVc1rbvrBd
kx1mlGzatam5rJjQCw5aqQOH6pWfk4gxKxlBZnH0cz0yzRvyx2ApERtuyrW7aV5k
dL2UA/P/b/u3nnUokSHfiICyaokEc2eqm9E7ijw6XEt7MEcOg8vxbBbumHcRNzcx
bzY2canjsOBbyOx+y1iuZuZaxYsCgYEA2t8AwidVjet4A8fuRo/j8H2w7FfiGe3w
BadciCqIrpgsuPVTDm5r8U397i0mCVPGzWIS/j5fV7C0ObdgdTf3zKU57a9Hvee+
cM/0m6M5vc1nA5XD16hwSKGVSvxfE7IGoLPNbpVRR0v1xrhpgIwN9at7GykpnXvo
c6gcR7MPVjcCgYEAsJoOQnqGhFiqeYMG4x321kchCQZtD4zDK7pFMgcri0V5juxH
uaHnbndQn7+fCHfofLDSDxYkPwhlgozPbk0d4kKhJtmeOTRceeP86oCN9iA7y4Pc
e30pNZhQbh1iExYrVS4N9a2McfZ3JEjNZn1JjY5jm1qGaLkLGyoPbIpqjvsCgYEA
uPQbuvXsSTqDN4a65uvvLam5WW9GhKzZ2J0+B18SE6BKop3E6vwKwWYrwBps+xLN
e392F1zzyrFrCx7YJxX9k/THyAAHuwXbm49P4DmFsMujUpc7YMFY6TeKZkxvt8AH
88MdRWZuwbYB4kSx+svffAvFwwT8wrUTkLCt/TTmL+8CgYBUaIDeYqiP3W9tfU2U
Lir2WLe/cgAxvVBL212g3e31BcKrxHBExeuHuOEy4f7Ql/dzbQ3n4ucSQqSf0Z15
6ZwRPl1tbfuxyD/Bv3ziBRi3V+BECwYy765N7ZcK8o88e1vHjFNRi/8jPIoHabc7
G42PodwfD3xUQKONThkgCHvF4A==
-----END PRIVATE KEY-----
END
)

# Templates for signed packages certificates
PKG_CRT_ROOT=$(cat <<-END
-----BEGIN CERTIFICATE-----
MIICxzCCAa+gAwIBAgIJAI18aq9lz7jtMA0GCSqGSIb3DQEBCwUAMBIxEDAOBgNV
BAMMB1Jvb3QgQ0EwHhcNMjIwODEyMDkyNzE2WhcNMzIwODA5MDkyNzE2WjASMRAw
DgYDVQQDDAdSb290IENBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA
rnDw/WuelZObMnayatETWPlDgAZI932oqFjjeGhOn6+vQowojtsg8gXHqhlXFklc
RxswcW03LwQDyZ45Ez730vi0TcdpccYhynTfB632POtcLPfHYkvm8XzyvKuyuUVC
Ch6P5DblAScGdEtt2dkC/IHLBMHzYa1PSHEMotL3SsFwH92XC2BnCXdMpoiKYz/L
l92gCoW/74JIHCg3fK2xkEd/Pe4+5zaaJFsRCT5Qj47J2hmxc94SQSQwlKgsrU47
DPVBfZg/JEyiAeZoAGGvXvB1XagFnOUe56sXLrSOe7lCVRrS+CfQ7a+WGjtfLXHS
e4UDlYIhWHrtItRlf0ga4QIDAQABoyAwHjALBgNVHQ8EBAMCAQYwDwYDVR0TAQH/
BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAJ4+ykcB+86aVrAabnECuffG33EFw
vIjmPkBrcC9a0nbXPT8jG69zpM1jTM1nXa8nGzFGhpjxjl0WjCbynL8oQ8oTDqBK
ddHEVQZzsLh0XruhZJ4Hx/ZdGpZqksBeyFitQAI7bgJsA9sfYFjjH4EICSiUAKkz
R4cVajWLngboACxfNOCDmBz6aRDjW4aLUmMoH0DQAeUK5cxfFHKoUFUK8Fzd7OgO
pRTHjasfsyhbYMbO+LLhpWmL+wGLCn8TBjdJ/x7t1BOiLxHglYWI/8rykUKh5Gts
j0WSFX4mcmDfxpzUSkRxIMd0K0DDkguO4ez60rpBKgu2n3IS6FxXORESzw==
-----END CERTIFICATE-----
END
)

PKG_CRT_INTERMEDIATE=$(cat <<-END
-----BEGIN CERTIFICATE-----
MIIC1DCCAbygAwIBAgIJAJwdpR47bVsGMA0GCSqGSIb3DQEBCwUAMBIxEDAOBgNV
BAMMB1Jvb3QgQ0EwHhcNMjIwODEyMDkyNzU5WhcNMzIwODA5MDkyNzU5WjAcMRow
GAYDVQQDDBFJbnRlcm1lZGlhdGUgQ0EgMTCCASIwDQYJKoZIhvcNAQEBBQADggEP
ADCCAQoCggEBAMGos+JYOYUfVvagOJ0A1mz/IeYHaEfzBoSRo78sTGV5AHsq9bQ8
BqLmeGDj7XzxoXfakXnN7Mg9SkJ6D6caSI+6TcBU2t2grGEu3VBZnn5mWTg8Xw7X
ySh0nd6hyfpqHDsUcUwi37OvXQSu7cMmxABUYTZ3FZdEpHewLh6sBi5CGvvSESZV
1XV5MGdbbLK1FpMpOng3SGXKKAvQ13aDpixbhh5Ei58fYIcy/RgPbSykl/zJgWcK
7Y3V/yX18eWxDfKAPM52WjC98+2yMYtVXRWNJp5MDCRL9VL2ADJMLR7NWv+HtPFf
/QZjxwMDqNpeUA7Gm0nw5/grbeIey5ZcGB0CAwEAAaMjMCEwCwYDVR0PBAQDAgIE
MBIGA1UdEwEB/wQIMAYBAf8CAQowDQYJKoZIhvcNAQELBQADggEBAI7ysApv0l1S
dplz2IJkJjuHl/8OMHf3GlMmkac7NEjThpYApNGVLi0Ko2GiT1iVzTarVc8Gq2nc
7F5HKUa9Kz90t717N5lEsN2WMA6ijT0o2xVeKe0kgmkElNFVVPTful6WaWiLBIhM
2MIvI1yXa5IaVVMynFinytJAHgrt1jLXe7NGaejL6Bn1YFXM5nIy7JkwhS9lDeYD
Fa2y0K2kMm0dIdGePh1T7fBox1Icpj+YQjctVy6wgrTrtKRHJFreyi1bwfm+z7dE
lzsnIqMdR6itwJiSeiDoIzKXXBqacODfssm3luh586Gp+DmX9haPgO6Swk230WFI
j7mrCqJNW+Y=
-----END CERTIFICATE-----
END
)

confFileTemplate="[ req ]
default_bits = 2048
distinguished_name = dn
req_extensions = req_ext

[ dn ]
CN = HOSTNAME (HOSTNAME)
CN_default = HOSTNAME
O = Example Company (Ericsson AB)
O_default = Ericsson AB
OU = Example Unit (IT)
OU_default = IT
L = City (Stockholm)
L_default = Stockholm
ST = State (Stockholm)
ST_default = Stockholm
C = Country (SE)
C_default = SE

[ req_ext ]
subjectAltName = DNS: HOSTNAME"


# values for Helmfile deployment
function renderValues() {
values_yaml=$(cat <<-END > $(pwd)/site_values.yaml
global:
  createClusterRoles: true
  hosts:
    gas: $HOST_GAS
    gr: $HOST_GR
    iam: $HOST_IDAM
    vnfm: $HOST_VNFM
$ICCR_GLOBAL_TEMPLATE
  registry:
    password: $GERRIT_PASSWORD
    url: $GLOBAL_REGISTRY_URL
    username: $GERRIT_USERNAME
  serviceMesh:
    enabled: false
  meshConfig:
    enableTracing: false
    defaultConfig:
      tracing:
        sampling: 10
  proxy:
    tracer: zipkin
  tracer:
    zipkin:
      address: localhost:9411
  support:
    ipv6:
      enabled: false
  timezone: UTC
  ericsson:
    licensing:
      licenseDomains:
      - productType: Ericsson_Orchestrator
        swltId: STB-EVNFM-1
        customerId: 800141
        applicationId: 800141_STB-EVNFM-1_Ericsson_Orchestrator
      nelsConfiguration:
        primary:
          hostname: $HOST_NELS
tags:
  eoCm: false
  eoEvnfm: true
  eoVmvnfm: true
eric-cloud-native-base:
  eric-ctrl-bro:
    persistence:
      persistentVolumeClaim:
        size: $CTRL_BRO_PVC_SIZE
  eric-data-search-engine:
    service:
      network:
        protocol:
          IPv6: false
  eric-sec-access-mgmt:
    accountManager:
      enabled: false
      inactivityThreshold: 9999
    replicaCount: 2
  eric-tm-ingress-controller-cr:
    enabled: true
    service:
      loadBalancerIP: "$LB_IP"
      annotations:
        cloudProviderLB: {}
      externalTrafficPolicy: Cluster
  eric-log-transformer:
    egress:
      syslog:
        enabled: false
        tls:
          enabled: true
        remoteHosts: []
  eric-data-object-storage-mn:
    persistentVolumeClaim:
      size: 10Gi
    affinity:
      podAntiAffinity: soft
  eric-fh-snmp-alarm-provider:
    sendAlarm: false
    service:
      annotations:
        sharedVIPLabel: shared-vip
    imageCredentials:
      pullSecret: k8s-registry-secret
  eric-lm-combined-server:
    licenseServerClient:
      licenseServer:
        thrift:
          host: $HOST_NELS
          port: 9095
      asih:
        port: 8080
    labels:
      eric-si-application-sys-info-handler-access: 'true'
  eric-si-application-sys-info-handler:
    asih:
      uploadSwimInformation: false
    applicationInfoService:
      port: 9095
  eric-fh-alarm-handler:
    imageCredentials:
      pullSecret: k8s-registry-secret
  eric-cloud-native-kvdb-rd-operand:
    imageCredentials:
      pullSecret: k8s-registry-secret
  eric-data-key-value-database-rd:
    resources:
      kvdbOperator:
        limits:
          cpu: 100m
          memory: 200Mi
          ephemeral-storage: 2Gi
    imageCredentials:
      pullSecret: k8s-registry-secret
geo-redundancy:
  enabled: false
eric-oss-common-base:
$SM_CNCS_TEMPLATE
  sessionTokens:
    maxSessionDurationSecs: 36000
    maxIdleTimeSecs: 1800
  eric-gr-bur-orchestrator:
    credentials:
      username: dummy
      password: dummy
    gr:
      bro:
        autoDelete:
          backupsLimit: 10
      sftp:
        url: vnfm:22/path/
        username: '$USER'
        password: '$SFTP_PASSWORD'
      cluster:
        role: PRIMARY
        secondary_hostnames:
        - dummy
      registry:
        secondarySiteContainerRegistryHostname: fakehostname.com
  system-user:
    credentials:
      username: system-user
      password: '$ERIC_PASSWORD'
  gas:
    defaultUser:
      username: gas-user
      password: '$ERIC_PASSWORD'
  eric-eo-usermgmt:
    replicaCount: 2
  eric-eo-usermgmt-ui:
    replicaCount: 2
  eric-cnom-server:
    imageCredentials:
      pullSecret: k8s-registry-secret
eric-oss-function-orchestration-common:
  eric-eo-evnfm-nbi:
    eric-evnfm-rbac:
      defaultUser:
        username: '$USER'
        password: '$ERIC_PASSWORD'
      eric-eo-evnfm-drac:
        enabled: false
        domainRoles:
        - name: Staging Role
          nodeTypes:
          - spider-app-multi-a-tosca
          - spider-app-multi-b-tosca
          - spider-app-multi-a-v2
          - spider-app-multi-b-v2
          - CCRC
        - name: Multi A Domain Role
          nodeTypes:
          - spider-app-multi-a-v2
          - spider-app-multi-a-tosca
          - spider-app-multi-a-etsi-tosca-rel4
        - name: Multi B Domain Role
          nodeTypes:
          - spider-app-multi-b-v2
          - spider-app-multi-b-tosca
          - spider-app-multi-b-etsi-tosca-rel4
  eric-am-onboarding-service:
    container:
      registry:
        enabled: true
    onboarding:
      skipCertificateValidation: false
eric-oss-eo-clm:
  enabled: false
eric-eo-evnfm:
  services:
    onboarding:
      enabled: true
  eric-am-common-wfs:
    helm:
      url: ''
    dockerRegistry:
      secret:
  eric-lcm-container-registry:
    enabled: true
    highAvailability: false
    ingress:
      hostname: $HOST_DOC_REG
    persistence:
      persistentVolumeClaim:
        size: $CONTAINER_REGISTRY_PVC_SIZE
    resources:
      brAgent:
        limits:
          memory: 1500Mi
          cpu: 1500m
          ephemeral-storage: 20Gi
  eric-global-lcm-container-registry:
    hostname: dummy
    username: dummy
    password: dummy
  eric-lcm-helm-chart-registry:
    ingress:
      enabled: true
      hostname: $HOST_HELM_REG
    env:
      secret:
        BASIC_AUTH_USER: '$USER'
        BASIC_AUTH_PASS: '$USER'
  eric-vnfm-orchestrator-service:
    oss:
      topology:
        secretName:
    smallstack:
      application: true
  application-manager-postgres:
    resources:
      bra:
        requests:
          ephemeral-storage: $PG_BRA_STORAGE_REQUESTS
        limits:
          ephemeral-storage: $PG_BRA_STORAGE_LIMITS
    probes:
      logshipper:
        livenessProbe:
          initialDelaySeconds: 300
eric-eo-evnfm-vm:
  eric-vnflcm-service:
    oss:
      secretName:
    persistentVolumeClaim:
      size: 20Gi
    service:
      enabled: false
      loadBalancerIP: 0.0.0.0
      externalTrafficPolicy: Local
END
)
}
# Enabling/disabling services in helmfile. For lightweight deployment - disable log-shipper/transformer, data-search-engine/curator etc.
renderOptionality() {
optionality_yaml=$(cat <<-END > $(pwd)/eric-eo-helmfile/optionality.yaml
optionality:
  eric-cloud-native-base:
    eric-cm-mediator:
      enabled: true
    eric-fh-snmp-alarm-provider:
      enabled: false
    eric-data-document-database-pg:
      enabled: false
    eric-fh-alarm-handler-db-pg:
      enabled: false
    eric-sec-access-mgmt-db-pg:
      enabled: true
    eric-lm-combined-server-db-pg:
      enabled: $NELS_ENABLED
    eric-cm-mediator-db-pg:
      enabled: true
    eric-pm-server:
      enabled: false
    eric-data-message-bus-kf:
      enabled: false
    eric-data-coordinator-zk:
      enabled: false
    eric-sec-key-management:
      enabled: true
    eric-fh-alarm-handler:
      enabled: false
    eric-sec-access-mgmt:
      enabled: true
    eric-sec-sip-tls:
      enabled: true
    eric-odca-diagnostic-data-collector:
      enabled: false
    eric-data-distributed-coordinator-ed:
      enabled: true
    eric-sec-certm:
      enabled: true
    eric-ctrl-bro:
      enabled: false
    eric-lm-combined-server:
      enabled: $NELS_ENABLED
    eric-data-search-engine:
      enabled: false
    eric-data-search-engine-curator:
      enabled: false
    eric-log-transformer:
      enabled: false
    eric-log-shipper:
      enabled: false
    eric-data-object-storage-mn:
      enabled: false
    eric-dst-agent:
      enabled: false
    eric-dst-collector:
      enabled: false
    eric-tm-ingress-controller-cr:
      enabled: false
    eric-si-application-sys-info-handler:
      enabled: false
    eric-data-key-value-database-rd:
      enabled: true
    eric-cloud-native-base-rd-operand:
      enabled: false
    eric-cloud-native-kvdb-rd-operand:
      enabled: true
  eric-oss-common-base:
    eric-eo-api-gateway:
      enabled: true
    eric-data-visualizer-kb:
      enabled: false
    eric-eo-usermgmt:
      enabled: true
    eric-eo-usermgmt-ui:
      enabled: true
    eric-eo-common-br-agent:
      enabled: false
    eric-oss-common-postgres:
      enabled: true
    eric-adp-gui-aggregator-service:
      enabled: true
    eric-oss-notification-service:
      enabled: false
    eric-oss-notification-service-database-pg:
      enabled: false
    eric-eo-eai:
      enabled: false
    eric-eo-eai-database-pg:
      enabled: false
    eric-oss-dmaap:
      enabled: false
    eric-eo-subsystem-management:
      enabled: false
    eric-eo-subsystem-management-database-pg:
      enabled: false
    eric-eo-subsystemsmgmt-ui:
      enabled: false
    eric-eo-credential-manager:
      enabled: false
    eric-eo-onboarding:
      enabled: false
    eric-eo-ecmsol005-adapter:
      enabled: false
    eric-eo-ecmsol005-stub:
      enabled: false
    eric-gr-bur-orchestrator:
      enabled: false
    eric-gr-bur-database-pg:
      enabled: false
    eric-oss-metrics-stager:
      enabled: false
    eric-oss-ddc:
      enabled: false
    eric-pm-kube-state-metrics:
      enabled: false
    eric-cnom-server:
      enabled: false
    service-mesh-ingress-gateway:
      enabled: false
    service-mesh-egress-gateway:
      enabled: false
    eric-oss-key-management-agent:
      enabled: false
    eric-dst-query:
      enabled: false
    eric-oss-help-aggregator:
      enabled: false
  eric-oss-function-orchestration-common:
    eric-eo-lm-consumer:
      enabled: true
    eric-eo-fh-event-to-alarm-adapter:
      enabled: false
    eric-eo-evnfm-nbi:
      enabled: true
    eric-eo-evnfm-crypto:
      enabled: true
    evnfm-toscao:
      enabled: true
    eric-am-onboarding-service:
      enabled: true
END
)
}

ICCR_GLOBAL_TEMPLATE=$(cat << END
  ingressClass: "$ENV_NAME"
END
)

ICCR_SEC_ACCESS_TEMPLATE=$(cat << END
    ingress:
      hostname: $HOST_IDAM
      ingressClass: "$ENV_NAME"
END
)

ICCR_CNCS_TEMPLATE=$(cat << END
  eric-tm-ingress-controller-cr:
    service:
      loadBalancerIP: "$LB_IP"
      annotations:
        cloudProviderLB: {}
      externalTrafficPolicy: "Local"
END
)

SM_CNCS_TEMPLATE=$(cat << END
  service-mesh-ingress-gateway:
    service:
      loadBalancerIP: "$LB_IP"
      annotations:
        cloudProviderLB: {}
END
)

function INFO() {
  echo "[$(date +%Y-%m-%d' '%T,%3N)] [$0] [$FUNCNAME]: $1"
}

function ERROR() {
  echo "[$(date +%Y-%m-%d' '%T,%3N)] [$0] [$FUNCNAME]: $1"
  exit 1
}

declare -a hosts=("$HOST_DOC_REG" "$HOST_HELM_REG" "$HOST_IDAM" "$HOST_VNFM" "$HOST_GAS")

function selfSignedCerts() {
  set +x
  INFO "Creating self-signed certificates"
  mkdir -p $WORKING_DIR
  echo "$SELF_SIGN_KEY_TLP" > $SELF_SIGN_KEY
  echo "$SELF_SIGN_CRT_TLP" > $SELF_SIGN_CRT
  createConfFilesForEGAD
  for each in ${hosts[@]}; do
    INFO "Creating self-signed certificates for $each"
    set +x
    openssl x509 -req -in $WORKING_DIR/$each.csr -out $WORKING_DIR/$each.crt -CA $SELF_SIGN_CRT -CAkey $SELF_SIGN_KEY -CAcreateserial -extfile $WORKING_DIR/$each.conf -extensions req_ext -days 10950
    rm -f $WORKING_DIR/$each.csr
    rm -f $WORKING_DIR/$each.conf
  done
}

function createConfFilesForEGAD() {
  mkdir -p $WORKING_DIR
  echo "$confFileTemplate" > $WORKING_DIR/confFileTemplate.conf
  for each in ${hosts[@]}; do
    INFO "Creating config file for $each"
    sed "s/HOSTNAME/${each}/g" $WORKING_DIR/confFileTemplate.conf >$WORKING_DIR/$each.conf
    createConfFile $each
  done
  rm -f $WORKING_DIR/confFileTemplate.conf
}

function createConfFile() {
  mkdir -p $WORKING_DIR
  openssl req -new -out $WORKING_DIR/$1.csr -keyout $WORKING_DIR/$1.key -config $WORKING_DIR/$1.conf -batch -nodes
}

function createNamespace() {
  if ! (kubectl get namespace --no-headers -o custom-columns=":metadata.labels.envOwner" | grep $SIGNUM > /dev/null); then
    i=0
    while ! (kubectl get namespace --no-headers -o custom-columns=":metadata.labels.envOwner" | grep $SIGNUM > /dev/null)
      do
        if ! (kubectl get namespace --no-headers -o custom-columns=":metadata.labels.envName" | grep vnfm$i > /dev/null); then
          kubectl create namespace $NAMESPACE
          kubectl label namespace $NAMESPACE envOwner=$SIGNUM envName=vnfm$i
        else
          ((i=i+1))
        fi
      done
  else
    echo "There is already created namespace labeled $SIGNUM"
  fi
}

function createDockerRegSecret() {
  INFO "Checking if container-registry-users-secret secret exists"
  kubectl get secret -n $NAMESPACE | grep "container-registry-users-secret"
  if [[ $? == 1 ]]; then
    INFO "Creating container-registry-users-secret secret"
    htpasswd -cBb htpasswd "$USER" "$ERIC_PASSWORD"
    kubectl create secret generic container-registry-users-secret --from-file=htpasswd=./htpasswd --namespace $NAMESPACE
    rm -f ./htpasswd
  else
    INFO "container-registry-users-secret secret already exists"
  fi
}

function createDockerRegistryAccessSecret() {
  INFO "Logging to dockerhubs"
  docker --config $WORKING_DIR login selndocker.mo.sw.ericsson.se -u "$GERRIT_USERNAME" -p "$GERRIT_PASSWORD"
  docker --config $WORKING_DIR login armdocker.rnd.ericsson.se -u "$GERRIT_USERNAME" -p "$GERRIT_PASSWORD"
  docker --config $WORKING_DIR login serodocker.sero.gic.ericsson.se -u "$GERRIT_USERNAME" -p "$GERRIT_PASSWORD"
  INFO "Creating k8s-registry-secret secret"
  kubectl create secret generic k8s-registry-secret \
    --from-file=.dockerconfigjson=$WORKING_DIR/config.json \
    --type=kubernetes.io/dockerconfigjson \
    --namespace $NAMESPACE
  rm -rf $WORKING_DIR/config.json
}

function createDockerRegCredentialsSecret() {
  INFO "Checking if container-credentials secret exists"
  kubectl get secret -n $NAMESPACE | grep "container-credentials"
  if [[ $? == 1 ]]; then
    INFO "Creating container-credentials secret"
    kubectl create secret generic container-credentials --from-literal=url=$HOST_DOC_REG --from-literal=userid=$REGISTRY_CREDENTIALS_USER \
      --from-literal=userpasswd=$REGISTRY_CREDENTIALS_PASS --namespace $NAMESPACE
  else
    INFO "container-credentials secret already exists"
  fi
}

function createSecAccessMgmtCredsSecret() {
  INFO "Checking if eric-sec-access-mgmt-creds secret exists"
  kubectl get secret -n $NAMESPACE | grep "eric-sec-access-mgmt-creds"
  if [[ $? == 1 ]]; then
    INFO "Creating eric-sec-access-mgmt-creds secret"
    kubectl create secret generic eric-sec-access-mgmt-creds --from-literal=kcadminid=admin --from-literal=kcpasswd=$PASSWORD \
      --from-literal=pguserid=admin --from-literal=pgpasswd=test-pw --namespace $NAMESPACE
  else
    INFO "eric-sec-access-mgmt-creds secret already exists"
  fi
}

function createPostgressDbSecret() {
  INFO "Checking if eric-eo-database-pg-secret secret exists"
  kubectl get secret -n $NAMESPACE | grep "eric-eo-database-pg-secret"
  if [[ $? == 1 ]]; then
    INFO "Creating eric-eo-database-pg-secret secret"
    kubectl create secret generic eric-eo-database-pg-secret --from-literal=custom-user=eo_user --from-literal=custom-pwd=postgres \
      --from-literal=super-user=postgres --from-literal=super-pwd=postgres --from-literal=metrics-user=exporter --from-literal=metrics-pwd=postgres \
      --from-literal=replica-user=replica --from-literal=replica-pwd=postgres --namespace $NAMESPACE
  else
    INFO "eric-eo-database-pg-secret secret already exists"
  fi
}

function createTlsSecret() {
  INFO "Checking if $1 secret exists"
  kubectl get secret $1 -n $NAMESPACE
  if [[ $? == 1 ]]; then
    INFO "Creating $1 secret"
    kubectl create secret tls $1 --key $WORKING_DIR/$2.key --cert $WORKING_DIR/$2.crt -n $NAMESPACE
  else
    INFO "$1 secret already exists"
  fi
}

function createAllTlsSecrets() {
  createTlsSecret registry-tls-secret $HOST_DOC_REG
  createTlsSecret helm-registry-tls-secret $HOST_HELM_REG
  createTlsSecret iam-tls-secret $HOST_IDAM
  createTlsSecret vnfm-tls-secret $HOST_VNFM
  createTlsSecret gas-tls-secret $HOST_GAS
}

function createCaCertsSecret() {
  INFO "Checking if iam-cacert-secret secret exists"
  kubectl get secret -n $NAMESPACE | grep "iam-cacert-secret"
  if [[ $? == 1 ]]; then
    INFO "Creating iam-cacert-secret secret"
    cp $SELF_SIGN_CRT $WORKING_DIR/tls.crt
    cp $SELF_SIGN_CRT $WORKING_DIR/cacertbundle.pem
    kubectl create secret generic iam-cacert-secret --from-file=$WORKING_DIR/tls.crt --from-file=$WORKING_DIR/cacertbundle.pem --namespace $NAMESPACE
    ls -la $WORKING_DIR
    rm -f $WORKING_DIR/tls.crt
    rm -f $WORKING_DIR/cacertbundle.pem
  else
    INFO "iam-cacert-secret secret already exists"
  fi
}

function makeKubeConfigFile() {
    INFO "Make kube config file $(pwd)/kube_config/config"
    if [ -f "$(pwd)/kube_config/config" ]; then
        INFO "Kube config file exist in the directory $(pwd)/kube_config/config"
    else
        INFO "Kube config file does not exist in the directory $(pwd)/kube_config/config "
        if [ -f "/home/$SIGNUM/.kube/config" ]; then
            INFO "Kube config file exist in the directory /home/$SIGNUM/.kube/config"
            INFO "Copy kube config file from the directory /home/$SIGNUM/.kube/config to $(pwd)/kube_config/config"
            mkdir $(pwd)/kube_config
            cp ~/.kube/config $(pwd)/kube_config/config
            chmod 600 $(pwd)/kube_config/config
        else
            ERROR "Kube config file does not exist in the directory $(pwd)/kube_config/config, please make kube config file"
        fi
    fi
}

function getHelmfileLatestVersion() {
  INFO "Get helmfile version"
  latestVersion=$(curl -u "${GERRIT_USERNAME}:${GERRIT_PASSWORD}" -X POST $ARTIFACTORY_PATH/api/search/aql \
    -H "content-type: text/plain" \
    -d 'items.find({ "repo": {"$eq":"proj-eo-helm"}, "path": {"$match" : "eric-eo-helmfile"}}).sort({"$desc": ["created"]}).limit(1)' \
      2>/dev/null | grep name | sed 's/.*eric-eo-helmfile-\(.*\).tgz.*/\1/')
  echo "$latestVersion"
}

function setHelmFileVersion() {
  INFO "Set helmfile version"
  getHelmfileLatestVersion
  if [ -z $EO_HELMFILE_VERSION ]
  then
      EO_HELMFILE_VERSION="$latestVersion"
      echo "eric-eo-helmfile version: $EO_HELMFILE_VERSION"
  else
      EO_HELMFILE_VERSION="$EO_HELMFILE_VERSION"
      echo "eric-eo-helmfile version: $EO_HELMFILE_VERSION"
  fi
}

function downloadHelmfile() {
    INFO "Download eo-helmfile "
    setHelmFileVersion
    $CI_TOOL_SCRIPT download-helmfile --chart-name eric-eo-helmfile --chart-version "$EO_HELMFILE_VERSION" --chart-repo "$EO_HELMFILE_REPO" --username "$GERRIT_USERNAME" --user-password "$GERRIT_PASSWORD"
}

function setDeploementManagerVersion() {
    INFO "Make kube config file $(pwd)/kube_config/config"
    DEPLOYMENT_MANAGER_VERSION=$(grep "tag:" $(pwd)/eric-eo-helmfile/dm_version.yaml | cut -f2 -d":" | tr -d \" | tr -d ' ')
    DEPLOYMENT_MANAGER_IMAGE="armdocker.rnd.ericsson.se/proj-eric-oss-drop/eric-oss-deployment-manager:$DEPLOYMENT_MANAGER_VERSION"
}

function unpackingArchive(){
    INFO "Unpacking eo-helmfile "
    tar -xf eric-eo-helmfile-$EO_HELMFILE_VERSION.tgz
    ls -la | grep --colour eo-helmfile
}

function replace_username() {
    INFO 'replace {{ env "GERRIT_USERNAME" }} in eric-eo-helmfile/repositories.yaml'
    sed -i """s# {{ env \"GERRIT_USERNAME\" }}# $GERRIT_USERNAME #""" $HELMFILE_REPOSITORIES_FILE
    cat $HELMFILE_REPOSITORIES_FILE
}

function replace_password() {
    INFO 'replace {{ env "GERRIT_PASSWORD" }} in eric-eo-helmfile/repositories.yaml'
    sed -i "s# {{ env \"GERRIT_PASSWORD\" }}# $GERRIT_PASSWORD #" $HELMFILE_REPOSITORIES_FILE
    cat $HELMFILE_REPOSITORIES_FILE
}

function getReleaseDetails() {
    INFO 'Get release details from eric-eo-helmfile'
    $CI_TOOL_SCRIPT get-release-details-from-helmfile --state-values-file $(pwd)/site_values.yaml --path-to-helmfile $(pwd)/eric-eo-helmfile/helmfile.yaml --get-all-images "false" --fetch-charts "true"
}

function build_csar() {
  INFO "Build csar (arguments were passed): "
  INFO "- File: $(pwd)/am_package_manager.properties"
  INFO "- Image: $AM_PACKEGE_MANAGER"
  INFO "- Include image: false"
  image="$AM_PACKEGE_MANAGER"
  include_image="false"
  while IFS= read -r prop; do
    echo "Found property entry: $prop"
    IFS='=' read -ra property_entry <<< "$prop"
    if [ "${#property_entry[@]}" -le 1 ]; then
      echo "Invalid property found: $prop"
      exit 1
    fi
    csar_name_version="${property_entry[0]}"
    csar_chart_content_list="$(echo "${property_entry[1]}" | tr -d '[:space:]' | tr ',' ' ')"
    echo -e "\n---------- Building imageless CSAR $csar_name_version ----------\n"
    for count in {1..5}; do
      echo -e "\n---------- Attempt ${count} of 5 for ${csar_name_version} ----------\n"
      if [ "$include_image" = "true" ]; then
        echo "Executing :: docker run --rm --volume $(pwd):$(pwd) -w $(pwd) ${image} generate --helm ${csar_chart_content_list} --name $csar_name_version"
        docker run --user $(id -u):$(id -g) --rm --volume $(pwd):$(pwd) -w $(pwd) ${image} generate --helm ${csar_chart_content_list} --name $csar_name_version
      else
        echo "Executing :: docker run --rm --volume $(pwd):$(pwd) -w $(pwd) ${image} generate --helm ${csar_chart_content_list} --name $csar_name_version --no-images"
        docker run --user $(id -u):$(id -g) --rm --volume $(pwd):$(pwd) -w $(pwd) ${image} generate --helm ${csar_chart_content_list} --name $csar_name_version --no-images
      fi
      if [ "$?" -eq 0 ]; then
        break
      fi
    done
  done < "$(pwd)/am_package_manager.properties"
}

function printDeploymentManagerVersion() {
    INFO 'Print Deployment Manager Version'
    setDeploementManagerVersion
    $RUN_DEPLOYMENT_MANAGER version | (echo -n 'DEPLOYMENT_MANAGER_VERSION=' && cat) >> artifact.properties
}

function deploymentManagerprepareNamespace() {
    INFO 'Prepare namespace by deploymen manager'
    $RUN_DEPLOYMENT_MANAGER prepare --namespace $NAMESPACE
}

function mergeSiteValuesFiles(){
    INFO "Merge site_values.yaml and site_values_$EO_HELMFILE_VERSION.yaml Files"
    $CI_TOOL_SCRIPT merge-yaml-files --path-base-yaml $(pwd)/site_values_$EO_HELMFILE_VERSION.yaml --path-override-yaml $(pwd)/site_values.yaml --path-output-yaml $(pwd)/site_values_$EO_HELMFILE_VERSION.yaml --check-values-only true
}

function updateCrdsHelmfile() {
    INFO 'Update crds Helmfile'
    $CI_TOOL_SCRIPT update-crds-helmfile --path-to-helmfile $(pwd)/eric-eo-helmfile/crds-helmfile.yaml
}

function runInstall() {
    INFO 'Run install...'
    $RUN_DEPLOYMENT_MANAGER install --namespace $NAMESPACE --crd-namespace eric-crd-ns --helm-timeout 3600
}

function addLabelForNoCleanup() {
  INFO "Checking if namespace label exists"
  kubectl get namespace --show-labels $NAMESPACE | grep doNotCleanup=true
  if [[ $? == 0 ]]; then
    INFO "Namespace is already labeled"
  else
    INFO "Adding namespace label"
    kubectl label namespace $NAMESPACE doNotCleanup=true
  fi
}

function runCleanUp() {
  INFO "Checking if namespace $NAMESPACE exists"
  kubectl get namespaces | grep $NAMESPACE
  if [[ $? == 0 ]]; then
    INFO "Running uninstall command"
    for each in $(helm ls -qn $NAMESPACE); do
      INFO "Uninstall release: $each";
      helm uninstall $each -n $NAMESPACE;
    done
    INFO "Deleting namespace $NAMESPACE"
    kubectl delete ns $NAMESPACE
  else
    INFO "Namespace $NAMESPACE not found, nothing to delete"
  fi
  INFO "Checking if ClusterRoleBinding exists"
  kubectl get ClusterRoleBinding -n $NAMESPACE | grep "evnfm-${NAMESPACE}"
  if [[ $? == 0 ]]; then
    kubectl delete clusterrolebinding evnfm-$NAMESPACE
  else
    INFO "ClusterRoleBinding doesn't exist"
  fi
  INFO "Checking if ClusterRole exists"
  kubectl get clusterrole | grep $SIGNUM
  if [[ $? == 0 ]]; then
    for each in $(kubectl get clusterrole -l app.kubernetes.io/instance=$SIGNUM-eric-eo-evnfm -o name); do
      kubectl delete $each;
    done
  else
    INFO "ClusterRole doesn't exist"
  fi
}

function createClusterRoleBinding() {
  INFO "Checking if ClusterRoleBinding exists"
  kubectl get clusterRoleBinding | grep "evnfm-${NAMESPACE}"
  if [[ $? == 1 ]]; then
    INFO "Creating ClusterRoleBinding evnfm-${NAMESPACE}"
    cat <<EOF | kubectl apply -n $NAMESPACE -f -
      apiVersion: rbac.authorization.k8s.io/v1
      kind: ClusterRoleBinding
      metadata:
        name: evnfm-$NAMESPACE
      subjects:
        - kind: ServiceAccount
          name: evnfm
          namespace: $NAMESPACE
      roleRef:
        kind: ClusterRole
        name: cluster-admin
        apiGroup: rbac.authorization.k8s.io
EOF
  else
    INFO "ClusterRoleBinding already exists"
  fi
}

function prepareNamespace() {
  INFO 'Prepare namespace '
  createNamespace
  createClusterRoleBinding
  createDockerRegSecret
  createDockerRegistryAccessSecret
  createDockerRegCredentialsSecret
  createSecAccessMgmtCredsSecret
  createPostgressDbSecret
  createAllTlsSecrets
  createCaCertsSecret
  deploymentManagerprepareNamespace
}

function prepareForInstall() {
  INFO "Prepare for Install"
  downloadHelmfile
  unpackingArchive
  replace_username
  replace_password
  renderValues
  renderOptionality
  getReleaseDetails
  build_csar
  makeKubeConfigFile
  prepareNamespace
  printDeploymentManagerVersion
  mergeSiteValuesFiles
  updateCrdsHelmfile
}

function showHelp() {
  echo "Usage: $0 [option...]" >&2
  echo """
  ############################################################################################################################################

     -s    | --selfSignedCerts,         Create self-signed certs to use in EVNFM installation
     -p    | --prepare,                 Prepare for install i.e. create namespace, service account, clusterRoleBindings
     -i    | --install,                 Run install (upgrade) command for helmfile chart
     -c    | --cleanup,                 Delete namespace and ClusterRoles/ClusterRoleBindings
     -t    | --no-cleanup,              Label namespace to skip night clean up. Only for important work - cluster resources are limited!
     -h    | --help                     Show help message

     Note: Script handles arguments from left to right (e.g. 'sh deploy-evnfm-csar.sh -cpti' will clean, prepare, label
             and deploy latest helmfile version), so arrange them correctly

  ############################################################################################################################################
  """
  exit 1
}

if ! TEMP=$(getopt -o e,m,d,b,s,p,o,i,t,u,c,g,n,m:,l,w,h,* \
  -l cu,co,egad,namespace:,rundos2unixCommand,bundleEgadCerts,selfSignedCerts,prepare,installold,installhelmfile,upgrade,cleanup,certoutput,chart:,no-cleanup,localca,cleandb,ipc,help,makeKubeConfigFile,* -q -- "$@"); then
  showHelp
  exit 1
fi

eval set -- "$TEMP"
while true; do
  case "$1" in
  --chart)
    HELM_CHART="$EO_HELMFILE_VERSION"
    shift 2
    ;;
  -s | --selfSignedCerts)
    selfSignedCerts
    shift 1
    ;;
  -p | --prepare)
    prepareForInstall
    shift 1
    ;;
  -i | --installhelmfile)
    runInstall
    shift 1
    ;;
  -c | --cleanup)
    runCleanUp
    shift 1
    ;;
  -t | --no-cleanup)
    addLabelForNoCleanup
    shift 1
    ;;
  -h | --help)
    showHelp
    shift 1
    ;;
  *)
    showHelp
    break
    ;;
  esac
done