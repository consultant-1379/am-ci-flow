# Role for Install YQ utility
### For run this role you could use ***jenkins_agents*** playbook:
```
ansible-playbook -i inventory.yml \
--private-key [your_ssh_key] \
-u [your_username] \
jenkins_agents.yml
```


### Variables description:
* **yq_version** - Version for YQ utility. *Example: **"3.2.3"***


### Examples
#### For Install YQ:
```
ansible-playbook -i inventory.yml \
--private-key [your_ssh_key] \
-u [your_username] \
jenkins_agents.yml \
--extra-vars "yq_version=3.2.3"
```