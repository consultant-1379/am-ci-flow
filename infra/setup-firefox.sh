#!/usr/bin/env bash

sudo su amadm100;
cd /home/amadm100;
wget https://github.com/mozilla/geckodriver/releases/download/v0.24.0/geckodriver-v0.24.0-linux64.tar.gz;
tar -xvf geckodriver-v0.24.0-linux64.tar.gz;
rm -f geckodriver-v0.24.0-linux64.tar.gz;
mkdir /home/amadm100/geckodriver-24;
mv ./geckodriver /home/amadm100/geckodriver-24/geckodriver;
echo "export PATH=$PATH:/home/amadm100/geckodriver-24" >> /home/amadm100/.bashrc;
