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
ARG user=pgclient
# User Id generated based on ADP rule DR-D1123-122 (pgInitContainer : 113992)
ARG uid=113992
ARG gid=113992
ARG POSTGRES_VERSION

RUN echo "${user}:x:${uid}:${gid}:postgres-client-user:/:/bin/bash" >> /etc/passwd
RUN sed -i '/root/s/bash/false/g' /etc/passwd

COPY create_db.sh /tmp/

RUN zypper addrepo -C -G -f https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles/${CBO_RELEASE_VERSION}?ssl_verify=no \
    COMMON_BASE_OS_SLES_REPO_PG \
    && zypper install -l -y curl \
    && zypper install -l -y postgresql${POSTGRES_VERSION} \
    && zypper install -l -y ca-certificates \
    && zypper install -l -y ca-certificates-mozilla \
    && zypper clean --all \
    && chmod 777 /tmp/create_db.sh

USER ${uid}:${gid}

ENTRYPOINT ["/tmp/create_db.sh"]
