#!/usr/bin/env bash
user=""
password=""

mydir="${0%/*}"

openssl req -new -key "${mydir}"/../openssl/server.key -out "${mydir}"/../openssl/server.csr -config "${mydir}"/../openssl/ssl.conf
openssl x509 -req -in "${mydir}"/../openssl/server.csr -CA "${mydir}"/../openssl/rootCA.crt -CAkey "${mydir}"/../openssl/rootCA.key -CAcreateserial -out "${mydir}"/../openssl/server.crt -days 30065 -extensions v3_ext -extfile "${mydir}"/../openssl/ssl.conf
cat "${mydir}"/../openssl/rootCA.crt >> "${mydir}"/../openssl/server.crt

# CHECK IF IAM SERVICE SECRET EXIST, IF NOT CREATE
SECRET_EXIST=`kubectl get secret iam-tls-secret -n ${NAMESPACE} 2>&1`
echo $SECRET_EXIST
if [[ $SECRET_EXIST  == *"(NotFound): secrets"* ]]; then
  kubectl create secret tls iam-tls-secret --namespace ${NAMESPACE} --key "${mydir}"/../openssl/server.key --cert "${mydir}"/../openssl/server.crt
  echo "iam service secret created"
fi

# CHECK IF DOCKER REGISTRY SERVICE SECRET EXIST, IF NOT CREATE
SECRET_EXIST=`kubectl get secret registry-tls-secret -n ${NAMESPACE} 2>&1`
echo $SECRET_EXIST
if [[ $SECRET_EXIST  == *"(NotFound): secrets"* ]]; then
  kubectl create secret tls registry-tls-secret --namespace ${NAMESPACE} --key "${mydir}"/../openssl/server.key --cert "${mydir}"/../openssl/server.crt
  echo "docker registry service secret created"
fi

# CHECK IF EVNFM SERVICE SECRET EXIST, IF NOT CREATE
SECRET_EXIST=`kubectl get secret vnfm-tls-secret -n ${NAMESPACE} 2>&1`
echo $SECRET_EXIST
if [[ $SECRET_EXIST  == *"(NotFound): secrets"* ]]; then
  kubectl create secret tls vnfm-tls-secret --namespace ${NAMESPACE} --key "${mydir}"/../openssl/server.key --cert "${mydir}"/../openssl/server.crt
  echo "evnfm service secret created"
fi

# CHECK IF ONBOARDING SERVICE SECRET EXIST, IF NOT CREATE
SECRET_EXIST=`kubectl get secret am-container-registry-release-flow-tls -n ${NAMESPACE} 2>&1`
echo $SECRET_EXIST
if [[ $SECRET_EXIST  == *"(NotFound): secrets"* ]]; then
  kubectl create secret tls am-container-registry-release-flow-tls --namespace ${NAMESPACE} --key "${mydir}"/../openssl/server.key --cert "${mydir}"/../openssl/server.crt
  echo "onboarding service secret created"
fi

# CHECK IF IAM USERS SECRET EXIST, IF NOT CREATE
SECRET_EXIST=`kubectl get secret eric-sec-access-mgmt-creds -n ${NAMESPACE} 2>&1`
if [[ $SECRET_EXIST  == *"(NotFound): secrets"* ]]; then
  kubectl create -f "${mydir}"/passwords.yaml --namespace ${NAMESPACE}
  echo "iam users secret created"
fi

# CHECK IF IAM USERS SECRET EXIST, IF NOT CREATE
if [[ ${NAMESPACE} == *"onboarding"* ]]; then
  SECRET_EXIST=`kubectl get secret eric-evnfm-rbac-default-user -n ${NAMESPACE} 2>&1`
  if [[ $SECRET_EXIST  == *"(NotFound): secrets"* ]]; then
    kubectl create -f "${mydir}"/passwords-onboarding.yaml --namespace ${NAMESPACE}
    echo "iam onboarding users secret created"
  fi
fi

# CHECK IF VNFM USERS SECRET EXIST, IF NOT CREATE
SECRET_EXIST=`kubectl get secret vnfm-user-credentials -n ${NAMESPACE} 2>&1`
if [[ $SECRET_EXIST  == *"(NotFound): secrets"* ]]; then
  kubectl create -f "${mydir}"/usercredentials.yaml --namespace ${NAMESPACE}
  echo "vnfm user secret created"
fi

# CHECK IF CONTAINER REGISTRY SECRET EXIST, IF NOT CREATE
SECRET_EXIST=`kubectl get secret container-registry-users-secret -n ${NAMESPACE} 2>&1`
if [[ $SECRET_EXIST  == *"(NotFound): secrets"* ]]; then
  kubectl create secret generic container-registry-users-secret --from-file=htpasswd="${mydir}"/htpasswd --namespace ${NAMESPACE}
  echo "container registry user secret created"
fi

# CHECK IF IAM_CA REGISTRY SECRET EXIST, IF NOT CREATE
SECRET_EXIST=`kubectl get secret  iam-cacert-secret  -n ${NAMESPACE} 2>&1`
if [[ $SECRET_EXIST  == *"(NotFound): secrets"* ]]; then
   kubectl create secret generic iam-cacert-secret --from-file="${mydir}"/../openssl/tls.crt --namespace ${NAMESPACE}
  echo "CA cert secret created"
fi

# CHECK IF DOCKER REGISTRY SECRET EXIST, IF NOT CREATE
SECRET_EXIST=`kubectl get secret  k8s-registry-secret  -n ${NAMESPACE} 2>&1`
if [[ $SECRET_EXIST  == *"(NotFound): secrets"* ]]; then
   kubectl create secret docker-registry k8s-registry-secret --docker-server=armdocker.rnd.ericsson.se --docker-username=$user --docker-password=$password -n ${NAMESPACE}
  echo "CA cert secret created"
fi
