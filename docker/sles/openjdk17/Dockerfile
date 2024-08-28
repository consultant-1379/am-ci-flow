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
    && zypper install -l -y curl \
    && zypper install -l -y java-17-openjdk-headless \
    && zypper install -l -y ca-certificates-mozilla \
    && zypper install -l -y libncurses6 \
    && zypper clean --all
