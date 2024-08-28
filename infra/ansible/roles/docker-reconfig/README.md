# Role for reconfigure Docker settings
### For run this role you could use ***jenkins_agents*** playbook:
```
ansible-playbook -i inventory.yml \
--private-key [your_ssh_key] \
-u [your_username] \
jenkins_agents.yml
```

### Tags description:
* **config** - update Docker config file, used variable: ***docker_mirror***
* **pull** - pulling custom Docker images, used variable: ***docker_host***, ***custom_image***

### Variables description:
* **docker_mirror** - Docker mirror host. *Example: **"http://armdockerhub.rnd.ericsson.se"***
* **docker_host** - Docker host path. *Example: **"armdocker.rnd.ericsson.se/dockerhub-ericsson-remote"***
* **custom_image** - Docker custom image. *Example: **"rabbitmq:3.7-management-alpine"***


### Examples
#### For reconfigure Docker:
```
ansible-playbook -i inventory.yml \
--private-key [your_ssh_key] \
-u [your_username] \
jenkins_agents.yml \
--extra-vars "docker_mirror=http://armdockerhub.rnd.ericsson.se" \
--tags config
```
#### For pulling custom Docker images:
```
ansible-playbook -i inventory.yml \
--private-key [your_ssh_key] \
-u [your_username] \
jenkins_agents.yml \
--extra-vars "docker_host=armdocker.rnd.ericsson.se/dockerhub-ericsson-remote" \
--extra-vars "custom_image=rabbitmq:3.7-management-alpine" \
--tags pull
```