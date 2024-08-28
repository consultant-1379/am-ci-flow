#!/bin/bash

yum install -y gcc-c++ patch readline readline-devel zlib zlib-devel libffi-devel \
openssl-devel make bzip2 autoconf automake libtool bison sqlite-devel
curl -sSL https://rvm.io/mpapis.asc | gpg2 --import -
curl -sSL https://rvm.io/pkuczynski.asc | gpg2 --import -
curl -sSL https://get.rvm.io | sudo bash -s stable --ruby
