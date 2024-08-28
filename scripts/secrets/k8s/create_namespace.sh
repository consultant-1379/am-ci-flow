#!/usr/bin/env bash

# CHECK IF CLUSTER ROLE BINDING SECRET EXIST, IF NOT CREATE
NAMESPACE_EXIST=`kubectl get namespace ${NAMESPACE} 2>&1`
echo $NAMESPACE_EXIST
if [[ $NAMESPACE_EXIST == *"(NotFound): namespaces"* ]]; then
  kubectl create namespace ${NAMESPACE}
  echo "namespace created"
fi