################################################################################
# Script to automatically delete CRDs used in e2e testing from a given cluster #
################################################################################

#!/bin/bash

helm_executable="helm3"

kube_config=$1
if [[ -z "$kube_config" ]]; then
    echo "1st parameter should be the path to the kube config"
fi
if [ ! -f $kube_config ]; then
    echo "specified kube config not found: $kube_config"
fi

crd_namespace=$2
if [[ -z "$crd_namespace" ]]; then
    echo "2nd parameter should be the namespace CRD should be deleted from"
fi

cluster_name=$(kubectl config view -o jsonpath='{.contexts[].context.cluster}')

function uninstall_crd(){
  echo "Uninstalling CRDs from $cluster_name in $crd_namesapce"

  while read crd_release; do
    echo "Uninstalling $crd_release from $crd_namesapce namespace";
    $helm_executable --kubeconfig=$kube_config uninstall -n $crd_namespace $crd_release || echo "Failed to delete $crd_release from $crd_namespace";
  done < ./jenkins/crd/cleanup/CRDs.txt

  while read custom_resource; do
    echo "Deleting Custom Resource $custom_resource"
    kubectl --kubeconfig=$kube_config delete crd $custom_resource || echo "Failed to delete Custom Resource $custom_resource"
  done < ./jenkins/crd/cleanup/CRs.txt
}

uninstall_crd


