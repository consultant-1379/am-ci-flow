#!/usr/bin/env bash


kubefile="$1"
sa="jenkins"
ns="jenkins"
secret="jenkins-token"


echo "INFO: Secret name - $secret"

echo 'INFO: Get token...'
token=$(kubectl get secret $secret \
                    --namespace $ns \
                    --kubeconfig $kubefile \
                    -o jsonpath='{.data.token}' | base64 -d)
echo "INFO: Print token..."
echo "$token"