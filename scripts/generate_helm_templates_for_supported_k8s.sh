#!/bin/bash
set -o nounset
set -o errexit

CHART=$1
SITE_VALUES_FILE=$2
SUPPORTED_VERSIONS_FILE_PATH=$3
PATH_TO_WRITE_TEMPLATES_TO=$4

SCHEMAS_LOCATION='https://arm.seli.gic.ericsson.se/artifactory/proj-ecm-k8s-schema-generic-local'

get_supported_api_versions_for_k8s_version() {
    K8S_VERSION=$1
    all_json_url="${SCHEMAS_LOCATION}/v${K8S_VERSION}-standalone-strict/all.json"
    wget --output-document="all_${K8S_VERSION}.json" "${all_json_url}"
    sed -n 's:^.*io\.k8s\.api\.\(.*\)\.\(.*\)\.\(.*\)"$:--api-versions \1.k8s.io/\2/\3:gp' "all_${K8S_VERSION}.json" | tr '\n' ' '
}

mkdir -p "${PATH_TO_WRITE_TEMPLATES_TO}"
MINOR_K8S_VERSIONS=$(awk -F. '{print $1 "." $2}' "${SUPPORTED_VERSIONS_FILE_PATH}" | sort -u)
echo "$MINOR_K8S_VERSIONS" | while read -r supported_version
do
    supported_version="${supported_version}.0"
    echo "Getting k8s api versions supported in version ${supported_version}"
    api_versions=$(get_supported_api_versions_for_k8s_version "${supported_version}")
    echo "Rendering helm template for k8s version ${supported_version}"
    echo "Executing: helm template ${CHART} -f ${SITE_VALUES_FILE} ${api_versions}  --kube-version "${supported_version}" > ${PATH_TO_WRITE_TEMPLATES_TO}/${supported_version}.yaml"
    helm template "${CHART}" -f "${SITE_VALUES_FILE}" ${api_versions} --kube-version "${supported_version}" > "${PATH_TO_WRITE_TEMPLATES_TO}/${supported_version}.yaml"
done