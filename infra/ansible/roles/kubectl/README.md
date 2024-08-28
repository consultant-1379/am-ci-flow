# Role for Install Kubectl utility
### For run this role you could use ***jenkins_agents*** playbook:
```
ansible-playbook -i inventory.yml \
--private-key [your_ssh_key] \
-u [your_username] \
jenkins_agents.yml
```

### Variables description:
* **kubectl_url** - URL for Kubectl utility binary. *Example: **"https://www.rnd.gic.ericsson.se/release/v1.22.4-kaas.1/bin/linux/amd64/kubectl"***


### Examples
#### For Install Kubectl:
```
ansible-playbook -i inventory.yml \
--private-key [your_ssh_key] \
-u [your_username] \
jenkins_agents.yml \
--extra-vars "kubectl_url=https://www.rnd.gic.ericsson.se/release/v1.22.4-kaas.1/bin/linux/amd64/kubectl"
```