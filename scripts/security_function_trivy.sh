#!/usr/bin/env bash

if [[ -z $1 ]]
then
    echo "Image path not passed as a argument"
    exit 1
fi

IMAGE_PATH=$1
if [[ ${IMAGE_PATH} = "combine" ]]; then
  less trivy/*.json | egrep -e VulnerabilityID | sort | uniq | awk -F  "\"" '{print $4}' | for i in $(cat) ; do echo -e "\n$i" && less trivy/*.json | egrep -A10 -e ${i} | egrep -e Title | head -1 | awk -F  "\"" '{print $4}' && less trivy/*.json | egrep -A10 -e ${i} | egrep -e Severity | head -1 | awk -F  "\"" '{print $4}' ; done > trivy/Simple-Trivy-`date +%F`.txt
else
  IMAGE_NAME=$(echo "${IMAGE_PATH}" | cut -d"/" -f4)
  echo "Generating Trivy report for ${IMAGE_PATH}"
  docker run --user $(id -u):$(id -g) -v $PWD:$PWD -w $PWD --rm armdocker.rnd.ericsson.se/proj-adp-cicd-drop/trivy-inline-scan:latest --format json --output trivy/trivy-${IMAGE_NAME}.json ${IMAGE_PATH}
fi
