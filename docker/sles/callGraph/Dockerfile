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

FROM perl:stable-threaded
#armdocker.rnd.ericsson.se/proj-am/sles/callgraph:1.1.0-1
RUN apt update && apt install -y make graphviz
RUN cpan install GraphViz
RUN git clone https://github.com/koknat/callGraph.git
RUN cp /usr/local/bin/perl5.38.0 /usr/bin/perl
ENTRYPOINT [ "/usr/src/app/callGraph/callGraph" ]
