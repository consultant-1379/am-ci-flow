#!/usr/bin/env bash

# Backups Jenkins jobs from Jenkins server to destination folder in user home directory


SERVER_URL='https://fem4s11-eiffel052.eiffel.gic.ericsson.se:8443/jenkins/'     # URI of your Jenkins server
DEST_FOLDER="$HOME/jenkins_backup"     	    # Output folder (will be created if it doesn't exist)
JENKINS_USER=""
JENKINS_PASS=""

if [ -z "$SERVER_URL" ]; then
    echo "Need to set environment variable JENKINS_URL (Operations Center root URL)."
    exit 1
fi

if [ -z "$JENKINS_USER" ]; then
    echo "Need to set environment variable JENKINS_USER."
    read -p "Enter jenkins user name:" JENKINS_USER
fi

if [ -z "$JENKINS_PASS" ]; then
    echo "Need to set environment variable JENKINS_PASS."
    read -s -p "Enter jenkins user password: "  JENKINS_PASS
fi

if [ -d "$DEST_FOLDER" ]
then
        echo "Using destination folder: $DEST_FOLDER."
else
        mkdir $DEST_FOLDER
fi


JobList=$(curl -sSq "$SERVER_URL/api/json" | jq -r '.jobs[] | .name')

echo "Job list: {$JobList} " > $HOME/jenkins_jobs_list.txt

for i in $JobList; do
    curl -sSq -u "$JENKINS_USER:$JENKINS_PASS" "$SERVER_URL/job/${i}/config.xml" -o "$DEST_FOLDER/${i}.xml";
#    echo "done: $i.xml";

done


# Write date
date=`date '+%Y%m%d%H%M%S'`

# Create archiv file adding timestamp

cd $DEST_FOLDER
tar cvfj "jenkins-jobs-$date.tar.bz2" *.xml
