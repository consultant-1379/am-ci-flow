#!/bin/bash
set -o nounset
set -o errexit
SUPPORTED_VERSIONS_FILE_PATH=$1
PATH_TO_READ_HELM_TEMPLATES_FROM=$2

echo "Running kubeval against the oldest and newest supported kubernetes version"
OLDEST_SUPPORTED_VERSION=$(head -1 "$SUPPORTED_VERSIONS_FILE_PATH")
NEWEST_SUPPORTED_VERSION=$(tail -1 "$SUPPORTED_VERSIONS_FILE_PATH")

VERSIONS_TO_CHECK="
${OLDEST_SUPPORTED_VERSION}
${NEWEST_SUPPORTED_VERSION}
"

KUBEVAL_KINDS_TO_SKIP="HTTPProxy,Gateway,VirtualService${ADDITIONAL_KUBEVAL_KINDS_TO_SKIP:-""}"

echo "$VERSIONS_TO_CHECK" | grep "\." | while read -r supported_version
do
    echo "Running Kubeval against kubernetes version $supported_version"

    set +o errexit
    CHECK_OUTPUT=$(kubeval --skip-kinds $KUBEVAL_KINDS_TO_SKIP -v "$supported_version" --strict --force-color "${PATH_TO_READ_HELM_TEMPLATES_FROM}/${supported_version}.yaml" --additional-schema-locations https://arm.seli.gic.ericsson.se/artifactory/proj-ecm-k8s-schema-generic-local)
    EXIT_CODE=$?
    set -o errexit
    echo "$CHECK_OUTPUT"
    if [ $EXIT_CODE -ne 0 ]
    then
        set +o errexit
        PASS_COUNT=$(echo "$CHECK_OUTPUT" | grep -c "PASS")
        FAIL_COUNT=$(echo "$CHECK_OUTPUT" | grep -c "FAIL")
        WARN_COUNT=$(echo "$CHECK_OUTPUT" | grep -c "WARN")
        set -o errexit
        echo "Passed: $PASS_COUNT"
        echo "Failures: $FAIL_COUNT"
        echo "Warnings: $WARN_COUNT"
# ADPPRG-58386
        if (echo "$CHECK_OUTPUT" | grep -q 'eric-tm-ingress-controller-cr/templates/daemonset.yaml contains an invalid DaemonSet - strategy: Additional property strategy is not allowed')
        then
            echo "WARNING: Skipping eric-tm-ingress-controller-cr daemonset warning until it is fixed."
        elif (echo "$CHECK_OUTPUT" | grep -q 'contains an invalid PodDisruptionBudget - apiVersion: apiVersion must be one of the following: "policy/v1beta1')
        then
            echo "WARNING: Skipping Poddisruption budget policy as v1beta is deprecated"
        else
            echo "kubeval failed against kubernetes version $supported_version"
            exit 1
        fi
    fi
done
