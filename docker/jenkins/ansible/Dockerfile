#
# COPYRIGHT Ericsson 2024
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

ARG BASE_IMAGE_VERSION
FROM armdocker.rnd.ericsson.se/proj-am/jenkins/base-agent-jdk17:${BASE_IMAGE_VERSION}

ARG ANSIBLE_VERSION
## Set install user to root
USER root

RUN apt-get update; \
    apt-get install -y gcc python3; \
    apt-get install -y python3-pip; \
    apt-get clean all; \
    pip3 install --no-cache-dir --upgrade pip; \
    pip3 install --no-cache-dir "ansible-core~=${ANSIBLE_VERSION}"


## Set run user
USER jenkins