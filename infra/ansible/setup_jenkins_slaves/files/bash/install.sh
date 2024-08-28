#!/bin/bash
set -e

yum groupinstall -y "Development tools"
cd /usr/local/src
curl https://ftp.gnu.org/gnu/bash/bash-4.4.tar.gz -o bash-4.4.tar.gz
tar xf bash-4.4.tar.gz
cd bash-4.4/
./configure
make
make install
ln -fs /usr/local/src/bash-4.4/bash /usr/bin/bash