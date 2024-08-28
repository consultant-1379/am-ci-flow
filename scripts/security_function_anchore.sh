#!/usr/bin/env bash

if [[ -z $1 ]]
then
    echo "Image path not passed as a argument"
    exit 1
fi

IMAGE_PATH=$1
IMAGE_NAME=$(echo "${IMAGE_PATH}" | cut -d"/" -f4)

export PATH="$HOME/.local/bin/:$PATH"
export LC_ALL=en_IE.utf8
export LANG=en_IE.utf8

source <(curl -sfL "https://ews.rnd.gic.ericsson.se/a/?a=anchore_env")
echo "adding ${IMAGE_PATH}"
anchore-cli image add ${IMAGE_PATH}

echo "waiting ${IMAGE_PATH}"
anchore-cli image wait ${IMAGE_PATH}

echo "generating anchore/${IMAGE_NAME}-all-json"
anchore-cli --json image vuln ${IMAGE_PATH} all > anchore/${IMAGE_NAME}-all.json