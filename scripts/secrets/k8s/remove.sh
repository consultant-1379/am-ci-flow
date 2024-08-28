#!/usr/bin/env bash

mydir="${0%/*}"

# CHECK IF ClusterRole EXIST, IF YES DELETE
CLUSTERROLE_EXIST=`kubectl get clusterrolebindings workflow-account-binding-${NAMESPACE} 2>&1`
echo $CLUSTERROLE_EXIST
if [[ $CLUSTERROLE_EXIST  == *"(NotFound): clusterrolebindings"* ]]; then
  echo "clusterrolebinding workflow-account-binding-${NAMESPACE} already deleted"
else
  kubectl delete clusterrolebinding workflow-account-binding-${NAMESPACE}
  echo "clusterrolebinding workflow-account-binding-${NAMESPACE} has been deleted"
fi

CLUSTERROLE_EXIST=`kubectl get evnfm-${NAMESPACE} 2>&1`
echo $CLUSTERROLE_EXIST
if [[ $CLUSTERROLE_EXIST  == *"(NotFound): clusterrolebindings"* ]]; then
  echo "clusterrolebinding evnfm-${NAMESPACE} already deleted"
else
  kubectl delete clusterrolebinding evnfm-${NAMESPACE}
  echo "clusterrolebinding evnfm-${NAMESPACE} has been deleted"
fi

kubectl delete namespace ${NAMESPACE}