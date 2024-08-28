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

############### Download and prepare
ARG CBO_RELEASE_VERSION
FROM armdocker.rnd.ericsson.se/proj-ldc/common_base_os_release/sles:${CBO_RELEASE_VERSION} as builder

ARG CBO_RELEASE_VERSION \
    KUBECTL_VERSION \
    HELM_VERSION \
    HELMFILE_VERSION \
    HELMFILE_URL=https://arm1s11-eiffel052.eiffel.gic.ericsson.se:8443/nexus/service/local/repositories \
    HELMFILE_REPO=eo-3pp-tools/content/com/helm/helmfile SPIN_VERSION \
    CONFIGS_DIR \
    MAVEN_VERSION \
    SPIN_VERSION \
    JENKINS_AGENT=https://repo.jenkins-ci.org/public/org/jenkins-ci/main/remoting/3190.va_7870fc137d9/remoting-3190.va_7870fc137d9.jar

RUN zypper addrepo -C -G -f https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles/${CBO_RELEASE_VERSION}?ssl_verify=no \
    COMMON_BASE_OS_SLES_REPO \
    && zypper --no-gpg-checks --gpg-auto-import-keys refresh \
    && zypper install -l -y curl fontconfig git-core openssh patch

##Kubectl
RUN curl -LO https://dl.k8s.io/${KUBECTL_VERSION}/kubernetes-client-linux-amd64.tar.gz \
    &&     tar -zxf kubernetes-client-linux-amd64.tar.gz \
    &&     mv -v kubernetes/client/bin/kubectl kubectl \
    &&     chmod +x kubectl \
    &&     cp kubectl /usr/bin/kubectl

## Helm
RUN curl -LO https://www.rnd.gic.ericsson.se/release/${HELM_VERSION}-helm/bin/helm_linux-amd64 \
    &&     mv -v helm_linux-amd64 helm \
    &&     chmod +x helm \
    &&     cp helm /usr/bin/helm \
    &&     ln -s /usr/bin/helm /usr/bin/helm3

## Hekmfile
RUN curl -LO ${HELMFILE_URL}/${HELMFILE_REPO}/${HELMFILE_VERSION}/helmfile-${HELMFILE_VERSION}.gz \
    &&     tar -xvf helmfile-${HELMFILE_VERSION}.gz helmfile \
    &&     chmod +x helmfile \
    &&     cp helmfile /usr/bin/helmfile \
    &&     ls -lsh /tmp/

## Maven
RUN chown -R ${uid}:${gid} /home/${user} \
    &&     curl -o /opt/apache-maven-${MAVEN_VERSION}-bin.tar.gz https://arm.seli.gic.ericsson.se/artifactory/apache-maven-2rc-cache/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    &&     tar -xvf /opt/apache-maven-${MAVEN_VERSION}-bin.tar.gz -C /opt \
    &&     rm -rf /opt/apache-maven-${MAVEN_VERSION}-bin.tar.gz

## Jenkins agent
RUN curl --create-dirs \
         -sSLo /usr/share/jenkins/agent.jar \
         ${JENKINS_AGENT}
# Install Spinnaker CLI
RUN curl -o /usr/bin/spin https://storage.googleapis.com/spinnaker-artifacts/spin/${SPIN_VERSION}/linux/amd64/spin && \
    chmod +x /usr/bin/spin

################ Main Image
ARG CBO_RELEASE_VERSION
FROM armdocker.rnd.ericsson.se/proj-ldc/common_base_os_release/sles:${CBO_RELEASE_VERSION}

## ARGs for build
ARG CONFIGS_DIR \
    MAVEN_VERSION \
    user=jenkins \
    group=jenkins \
    uid=1000 \
    gid=1000 \
    AGENT_WORKDIR=/home/jenkins/agent \
    VERSION=3192.v713e3b_039fb_e \
    CBO_RELEASE_VERSION \
    KUBECTL_VERSION HELM_VERSION \
    HELMFILE_VERSION \
    HELMFILE_URL=https://arm1s11-eiffel052.eiffel.gic.ericsson.se:8443/nexus/service/local/repositories \
    HELMFILE_REPO=eo-3pp-tools/content/com/helm/helmfile SPIN_VERSION

ENV TZ=Etc/UTC
ENV LANG=C.UTF-8

#
RUN zypper addrepo -C -G -f https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles/${CBO_RELEASE_VERSION}?ssl_verify=no \
    COMMON_BASE_OS_SLES_REPO \
    && zypper addrepo -C -G -f https://download.opensuse.org/repositories/security:SELinux/15.4/security:SELinux.repo \
    && zypper addrepo -C -G -f https://download.opensuse.org/repositories/Virtualization:containers/15.5/Virtualization:containers.repo \
    && zypper addrepo https://yum.corretto.aws/corretto.repo \
    && zypper --no-gpg-checks --gpg-auto-import-keys refresh \
    && zypper install -l -y --force-resolution container-selinux \
    && zypper install -l -y docker \
    && zypper install -l -y java-17-amazon-corretto-devel \
    && zypper install -l -y curl fontconfig git-core openssh patch unzip\
    && zypper install -l -y ca-certificates-mozilla \
    && zypper install -l -y libncurses6 \
    && zypper install -l -y sudo \
    && zypper install -l -y jq \
    && zypper install -l -y apache2-utils \
    && zypper install -l -y python python3-pip \
    && zypper clean --all

RUN rm -rf /usr/lib/jvm/java-17-amazon-corretto/lib/security/cacerts || echo "not found /usr/lib/jvm/java-17-amazon-corretto/lib/security/cacerts" \
    && ln -s /var/lib/ca-certificates/java-cacerts /usr/lib/jvm/java-17-amazon-corretto/lib/security/cacerts || echo "not found /usr/lib/jvm/java-17-amazon-corretto/lib/security/cacerts"

RUN useradd  -U -m -p "kjhkj6hjhk^*&kjhkj" ${user} \
    && echo 'jenkins ALL=(ALL) NOPASSWD: ALL' >> /etc/sudoers \
    && sed -i 's/Defaults targetpw//g' /etc/sudoers \
    && groupadd docker || echo "Group docker exist"\
    && usermod -aG docker ${user}


COPY  ${CONFIGS_DIR}/certificates /opt/certificates
COPY --from=builder /usr/share/jenkins/agent.jar /usr/share/jenkins/agent.jar
COPY  ${CONFIGS_DIR}/jenkins-agent /usr/local/bin/jenkins-agent
COPY --from=builder /usr/bin/kubectl /usr/bin/kubectl
COPY --from=builder /usr/bin/helm /usr/bin/helm
COPY --from=builder /usr/bin/helm3 /usr/bin/helm3
COPY --from=builder /usr/bin/helmfile /usr/bin/helmfile
COPY --from=builder /usr/bin/spin /usr/bin/spin
COPY --from=builder /opt/apache-maven-${MAVEN_VERSION}/ /opt/apache-maven-${MAVEN_VERSION}/
COPY ${CONFIGS_DIR}/.ssh /home/jenkins/.ssh
COPY ${CONFIGS_DIR}/.m2 /home/jenkins/.m2

RUN pip install pyyaml \
    requests \
    docker \
    yq

RUN cp /opt/certificates/SelfRootCA.crt /var/lib/ca-certificates/ \
    &&     cp /opt/certificates/EGADIssuingCA3.crt /var/lib/ca-certificates/ \
    &&     cp /opt/certificates/EGADRootCA.crt /var/lib/ca-certificates/ \
    &&     update-ca-certificates \
    &&     mkdir -p /etc/pki/java \
    && keytool -storepass 'changeit' \
       -noprompt \
       -trustcacerts \
       -importcert \
       -file /var/lib/ca-certificates/SelfRootCA.crt \
       -alias selfRootCA \
       -keystore /var/lib/ca-certificates/java-cacerts \
    && keytool -storepass 'changeit' \
        -noprompt \
        -trustcacerts  \
        -importcert \
        -file /var/lib/ca-certificates/EGADIssuingCA3.crt  \
        -alias egadCA3 \
        -keystore /var/lib/ca-certificates/java-cacerts \
    && keytool -storepass 'changeit' \
        -noprompt \
        -trustcacerts \
        -importcert \
        -file /var/lib/ca-certificates/EGADRootCA.crt \
        -alias egadRootCA \
        -keystore /var/lib/ca-certificates/java-cacerts \
    && mkdir -p /etc/pki/ca-trust/source/anchors \
    && cp /opt/certificates/SelfRootCA.crt /etc/pki/ca-trust/source/anchors/ \
    && cp /opt/certificates/EGADIssuingCA3.crt /etc/pki/ca-trust/source/anchors/ \
    && cp /opt/certificates/EGADRootCA.crt /etc/pki/ca-trust/source/anchors \
    && ln -sf /usr/share/jenkins/agent.jar /usr/share/jenkins/slave.jar \
    && chmod +x /usr/local/bin/jenkins-agent

RUN mkdir /home/${user}/.jenkins \
    && mkdir -p "${AGENT_WORKDIR}" \
    && chown -R ${user}:${group} /home/${user}/
USER ${user}:${group}
WORKDIR /home/${user}
ENV AGENT_WORKDIR=/home/jenkins/agent
ENV user=jenkins
ENV AGENT_WORKDIR=/home/${user}/agent
ENV HOME=/home/${user}
ENV M2_HOME=/opt/apache-maven-${MAVEN_VERSION}
ENV M2=/opt/apache-maven-${MAVEN_VERSION}/bin
ENV PATH=/opt/apache-maven-${MAVEN_VERSION}/bin:$PATH
ENTRYPOINT ["/usr/local/bin/jenkins-agent"]

### DOCKER_BUILDKIT=1 docker -l debug build \--no-cache --pull --tag armdocker.rnd.ericsson.se/proj-am/jenkins/base-agent-corretto17:1.5.0 -f maven-agent-corretto17/Dockerfile --build-arg CBO_RELEASE_VERSION=6.3.0-13 --build-arg CONFIGS_DIR=configs --build-arg KUBECTL_VERSION=v1.27.6 --build-arg HELM_VERSION=v3.8.2 --build-arg HELMFILE_VERSION=0.149.0 --build-arg SPIN_VERSION=1.29.0 --build-arg MAVEN_VERSION=3.9.3 .