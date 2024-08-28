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

### Run prepare steps
FROM armdocker.rnd.ericsson.se/proj-am/jenkins/inbound-agent:${BASE_IMAGE_VERSION} as prepare
ARG KUBECTL_VERSION \
    HELM_VERSION \
    HELMFILE_VERSION \
    HELMFILE_URL="https://arm1s11-eiffel052.eiffel.gic.ericsson.se:8443/nexus/service/local/repositories" \
    HELMFILE_REPO="eo-3pp-tools/content/com/helm/helmfile" \
    SPIN_VERSION

WORKDIR /tmp

## Set install user to root
USER root

## Get binary
# Renew apt cache
Run  apt update && \
# Install base packages
    apt install -y  wget \
                    curl && \
# Install Kubectl
    wget https://dl.k8s.io/${KUBECTL_VERSION}/kubernetes-client-linux-amd64.tar.gz && \
    tar -zxf kubernetes-client-linux-amd64.tar.gz && \
    mv -v kubernetes/client/bin/kubectl kubectl && \
    chmod +x kubectl && \
# Install Helm
    wget https://www.rnd.gic.ericsson.se/release/${HELM_VERSION}-helm/bin/helm_linux-amd64 && \
    mv -v helm_linux-amd64 helm && \
    chmod +x helm && \
# Install Helmfile
    wget ${HELMFILE_URL}/${HELMFILE_REPO}/${HELMFILE_VERSION}/helmfile-${HELMFILE_VERSION}.gz && \
    tar -xvf helmfile-${HELMFILE_VERSION}.gz helmfile && \
    chmod +x helmfile && \
# Install Spinnaker CLI
    curl -LO https://storage.googleapis.com/spinnaker-artifacts/spin/${SPIN_VERSION}/linux/amd64/spin && \
    chmod +x spin && \
# Check install directory
    ls -lsh /tmp/


################################################################################
### Build image steps
FROM armdocker.rnd.ericsson.se/proj-am/jenkins/inbound-agent:${BASE_IMAGE_VERSION}

## ARGs for build
ARG CONFIGS_DIR \
    DOCKER_VERSION

## Add config for SSH
ADD ${CONFIGS_DIR}/.ssh /home/jenkins/.ssh

## Copy binaries
COPY --from=prepare /tmp/kubectl /usr/bin/kubectl
COPY --from=prepare /tmp/helm /usr/bin/helm
COPY --from=prepare /tmp/helmfile /usr/bin/helmfile
COPY --from=prepare /tmp/spin /usr/bin/spin

## Set install user to root
USER root

## Install packages
# Update permission
RUN chown -R jenkins:jenkins /home/jenkins && \
# Renew apt cache
    apt update && \
# Configure Sudo
    apt install -y sudo  && \
    usermod -aG sudo jenkins && \
    echo 'jenkins ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers && \
# Install base packages
    apt install -y  curl \
                    wget \
                    tree \
                    nano \
                    apache2-utils \
                    zip \
                    netcat-traditional \
                    dnsutils \
                    ldap-utils \
                    procps \
                    jq \
                    gawk \
                    xxd && \
# Install pip3 and python packages
    ln -s /usr/bin/python3 /usr/bin/python && \
    apt install -y python3-pip && \
    pip3 install  pyyaml \
                  requests \
                  docker \
                  yq && \
# Install Docker CLI
    apt install -y  lsb-release \
                    ca-certificates \
                    gnupg && \
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/docker.gpg && \
    echo "deb [arch=amd64 signed-by=/etc/apt/docker.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" > \
        /etc/apt/sources.list.d/docker.list && \
    apt update && \
    apt install -y docker-ce-cli=${DOCKER_VERSION} && \
# Install post-install steps
    ln -s /bin/sh /usr/bin/sh && \
    ln -s /usr/bin/helm /usr/bin/helm3 && \
# Clean cache
    apt clean

## Set run user
USER jenkins