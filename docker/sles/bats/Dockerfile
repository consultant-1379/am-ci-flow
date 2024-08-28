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

FROM bats/bats
# armdocker.rnd.ericsson.se/proj-am/sles/bats:1.10.0-1
RUN apk --no-cache --update add curl git && \
    curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" && \
    chmod 755 ./kubectl && \
    cp ./kubectl /bin/ && \
    git clone https://github.com/bats-core/bats-detik.git /opt/lib/bats-detik && \
    git clone https://github.com/bats-core/bats-support.git /opt/lib/bats-support && \
    git clone https://github.com/bats-core/bats-file.git /opt/lib/bats-file && \
    git clone https://github.com/bats-core/bats-assert.git /opt/lib/bats-assert
ENV BATS_LIB_PATH="/opt/lib:~/.bats/lib"