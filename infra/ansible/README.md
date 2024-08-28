# Ansible playbooks for infrastructure management

The playbooks in this folder automate some configuration and management of our CI infrastructure.

* Jenkins Compute Engines
* Kubernetes Clusters

There are two prerequisites:

1. You have Ansible installed (Minimum version of 2.8)
2. you've uploaded your public SSH key to [EWS](https://ews.rnd.gic.ericsson.se/)

execute a playbook as follows:

```bash

ansible-playbook -i hosts/{host_file} {path_to_playbook} --extra-vars="..."
```

The `hosts` folder contains files which contain the IP addresses of our current infrastructure.

extra-vars should include any of the variables you want to set in the playbook.
For example, in the `worker_node_ssh_access.yaml` playbook you need to set the `user_to_add` variable.

## Running on Windows subsystem for Linux
When running these in the windows subsystem there are a few things you will need to change in order to run these scripts

1) For script files make sure these files have unix line endings and not windows. Otherwise you will get a *bad interpreter error*.
2) Linux subsystem runs as root. But these scripts are supposed to run against the current user if you where on a Ubuntu only machine.
You need to configure the ansible hosts with your signum in the hosts file i.e. amamd100@150.132.197.183 or use ssh config file to configure your ssh to define a host alias that logs in as
a particular user. Here is an example config file

```bash
Host process-engine-1
  Hostname 150.132.197.183
  Port 22
  User amadm100
  IdentityFile ~/.ssh/amadm100_rsa
Host process-engine-2
  Hostname 150.132.197.184
  Port 22
  User amadm100
  IdentityFile ~/.ssh/amadm100_rsa
Host process-engine-3
  Hostname 150.132.197.185
  Port 22
  User amadm100
  IdentityFile ~/.ssh/amadm100_rsa
Host process-engine-4
  Hostname 150.132.197.186
  Port 22
  User amadm100
  IdentityFile ~/.ssh/amadm100_rsa
Host process-engine-5
  Hostname 150.132.197.187
  Port 22
  User amadm100
  IdentityFile ~/.ssh/amadm100_rsa
```

## Playbooks

### jenkins slave setup

This playbook pulls together multiple roles to complete all the setup for a jenkins slave

#### Manual Steps after playbook
* Docker login to armdocker.rnd.ericsson.se
````bash
docker login armdocker.rnd.ericsson.se
````
* Run helm init - used for helm 2 adding of helm local repository
````bash
helm init
````
* Pull a git repository with gerrit mirror to accept ssh key for the server. Build will throw an error failed to verify ssh key.
All it is looking for is the user to answer yes to accept the connection.
* Copy CSAR files to new slaves

### clean up compute engines

This playbook deletes file on the compute engines (jenkins slaves) to make space.

* delete all docker images
* delete everything in the jenkins workspace folder
* delete files in the .m2 folder which haven't been accessed in 100 days

It doesn't take any variables and there are no prerequisites.

### ssh access to vms

This playbook gives an ericsson id ssh access to all vms listed in the given hosts file.

### copy file/dir to remote hosts

This playbook enables copying a file or contents of a directory to all hosts specified.

There are two mandatory parameters:

* source:   absolute/relative path to the file/directory to be copied on the local machine
* dest:     absolute path to location on remote machines to copy to.

There are some optiona parameters too:

* owner         defaults to amadm100
* group         defaults to amadm100
* permissions   defaults to 755

This playbook is based on the [copy](https://docs.ansible.com/ansible/latest/modules/copy_module.html) Ansible module.