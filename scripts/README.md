# How to run these scripts (WIP)

## cluster-health.sh
Need to have your default kube config pointing to the cluster you wish to run script against then run without any parameters

## collect_adp_logs.sh
Need to have your default kube config pointing to the cluster you wish to run script against then run with one parameter

**First param, Namespace:** Namespace to collect logs from

## backup-jenkins-jobs.sh
To backup Jenkins job config files from server It needs:

    1. download script  from gerrit repository: am-ci-flow/scripts/backup-jenkins-jobs.sh to your home directory on your Linux VM.
    2. install jq - Command-line JSON processor
    3. run script locally:    $ ./backup-jenkins-jobs.sh
    4. jobs config files and a archive file would be stored in $HOME/jenkins-backup directory.

## jenkins-backup.sh

To backup Jenkins server config files, It needs:

1. run on master node Jenkins job: jenkins_backup_config_pipe.
2. from working directory download file: jenkins_backup.tar.gz

To restore Jenkins server config files, It needs:
1. connect to Jenkins master node and in terminal run next commands.
2. sudo /etc/init.d/jenkins stop
3. cd /path/to/backup_dir
4. tar xzvf jenkins_backup.tar.gz
5. sudo cp -R jenkins-backup/* /path/to/jenkins_home/
6. sudo chown jenkins:jenkins -R /path/to/jenkins_home/
7. sudo /etc/init.d/jenkins start





