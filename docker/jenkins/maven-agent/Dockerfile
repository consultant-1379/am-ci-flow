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

ARG BASE_IMAGE_NAME \
    BASE_IMAGE_VERSION

FROM armdocker.rnd.ericsson.se/proj-am/jenkins/${BASE_IMAGE_NAME}:${BASE_IMAGE_VERSION}

## ARGs for build
ARG CONFIGS_DIR \
    MAVEN_VERSION


## Add config for Maven
ADD ${CONFIGS_DIR}/.m2 /home/jenkins/.m2
## Add Root CA
ADD ${CONFIGS_DIR}/certificates /opt/certificates


## Set install user to root
USER root


## Install packages
# Update permission
RUN chown -R jenkins:jenkins /home/jenkins && \

# Install maven
    wget https://arm.seli.gic.ericsson.se/artifactory/apache-maven-2rc-cache/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz -P /opt && \
    tar -xvf /opt/apache-maven-${MAVEN_VERSION}-bin.tar.gz -C /opt && \
    rm -rf /opt/apache-maven-${MAVEN_VERSION}-bin.tar.gz

# Setup maven variables
ENV M2_HOME /opt/apache-maven-${MAVEN_VERSION}
ENV M2 $M2_HOME/bin
ENV PATH $M2:$PATH

## Setup certificates
# Install new certificates
RUN cp /opt/certificates/SelfRootCA.crt /usr/local/share/ca-certificates/ && \
    cp /opt/certificates/EGADIssuingCA3.crt /usr/local/share/ca-certificates/ && \
    cp /opt/certificates/EGADRootCA.crt /usr/local/share/ca-certificates/ && \
    update-ca-certificates && \
# Import new certificates to Java
    mkdir -p /etc/pki/java && \
    keytool -storepass 'changeit' \
            -noprompt \
            -trustcacerts \
            -importcert \
            -file /usr/local/share/ca-certificates/SelfRootCA.crt \
            -alias selfRootCA \
            -keystore /opt/java/openjdk/lib/security/cacerts && \
    keytool -storepass 'changeit' \
            -noprompt \
            -trustcacerts \
            -importcert \
            -file /usr/local/share/ca-certificates/EGADIssuingCA3.crt \
            -alias egadCA3 \
            -keystore /opt/java/openjdk/lib/security/cacerts && \
    keytool -storepass 'changeit' \
            -noprompt \
            -trustcacerts \
            -importcert \
            -file /usr/local/share/ca-certificates/EGADRootCA.crt \
            -alias egadRootCA \
            -keystore /opt/java/openjdk/lib/security/cacerts && \
# Copy certificates to Anchor directory
    mkdir -p /etc/pki/ca-trust/source/anchors && \
    cp /opt/certificates/SelfRootCA.crt /etc/pki/ca-trust/source/anchors/ && \
    cp /opt/certificates/EGADIssuingCA3.crt /etc/pki/ca-trust/source/anchors/ && \
    cp /opt/certificates/EGADRootCA.crt /etc/pki/ca-trust/source/anchors/


## Set run user
USER jenkins