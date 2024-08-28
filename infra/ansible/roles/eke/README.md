# Role for Install EKE utility
### For run this role you could use ***jenkins_agents*** playbook:
```
ansible-playbook -i inventory.yml \
--private-key [your_ssh_key] \
-u [your_username] \
jenkins_agents.yml
```

### Variables description:
* **eke_url** - URL for EKE utility binary. *Example: **"https://www.rnd.gic.ericsson.se/release/v0.0.1-eke/bin/linux/amd64/eke"***


### Examples
#### For Install EKE:
```
ansible-playbook -i inventory.yml \
--private-key [your_ssh_key] \
-u [your_username] \
jenkins_agents.yml \
--extra-vars "eke_url=https://www.rnd.gic.ericsson.se/release/v0.0.1-eke/bin/linux/amd64/eke"
```