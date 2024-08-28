#!/usr/bin/env bash

mydir="${0%/*}"

# CHECK IF CLUSTER ROLE BINDING SECRET EXIST, IF NOT CREATE
CLUSTER_ROLE_BINDING_EXIST=`kubectl get clusterrolebindings evnfm-${NAMESPACE} 2>&1`
echo $CLUSTER_ROLE_BINDING_EXIST
if [[ $CLUSTER_ROLE_BINDING_EXIST == *"(NotFound): clusterrolebindings.rbac.authorization.k8s.io"* ]]; then
  kubectl create clusterrolebinding evnfm-${NAMESPACE} --clusterrole=cluster-admin --serviceaccount=${NAMESPACE}:evnfm
  echo "cluster role binding created"
fi