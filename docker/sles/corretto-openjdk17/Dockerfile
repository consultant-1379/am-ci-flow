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

ARG CBO_RELEASE_VERSION
FROM armdocker.rnd.ericsson.se/proj-ldc/common_base_os_release/sles:${CBO_RELEASE_VERSION}

ARG CBO_RELEASE_VERSION

RUN zypper addrepo -C -G -f https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles/${CBO_RELEASE_VERSION}?ssl_verify=no \
    COMMON_BASE_OS_SLES_REPO \
    && zypper addrepo -C -G -f https://arm.sero.gic.ericsson.se/artifactory/proj-ldc-repo-rpm-local/adp-dev/adp-build-env/${CBO_RELEASE_VERSION}?ssl_verify=no COMMON_BASE_OS_DEVELEOPMENT_SDK \
    && zypper --no-gpg-checks --gpg-auto-import-keys refresh \
    && zypper refresh \
    && zypper install -l -y curl \
    && zypper install -l -y zip  \
    && zypper install -l -y unzip \
    && zypper --no-gpg-checks install -l -y https://arm1s11-eiffel052.eiffel.gic.ericsson.se:8443/nexus/service/local/repositories/evnfm_storage/content/aws/amazon-corretto/17-x64-linux/amazon-corretto-17-x64-linux.rpm \
    && zypper install -l -y ca-certificates-mozilla \
    && zypper install -l -y libncurses6 \
    && zypper clean --all

RUN rm -rf /usr/lib/jvm/java-17-amazon-corretto/lib/security/cacerts \
    && ln -s /var/lib/ca-certificates/java-cacerts /usr/lib/jvm/java-17-amazon-corretto/lib/security/cacerts