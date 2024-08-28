#!/usr/bin/env bash

mydir="${0%/*}"

# CHECK IF SERVICE ACCOUNT EXIST, IF NOT CREATE
SERVICE_ACCOUNT_EXIST=`kubectl get serviceaccount evnfm -n ${NAMESPACE} 2>&1`
echo $SERVICE_ACCOUNT_EXIST
if [[ $SERVICE_ACCOUNT_EXIST  == *"(NotFound): serviceaccounts"* ]]; then
  kubectl create -f "$mydir"/ServiceAccount.yaml --namespace ${NAMESPACE}
  echo "service account evnfm created"
fi


