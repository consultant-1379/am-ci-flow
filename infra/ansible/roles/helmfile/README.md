# Role for Install Helmfile utility
### For run this role you could use ***jenkins_agents*** playbook:
```
ansible-playbook -i inventory.yml \
--private-key [your_ssh_key] \
-u [your_username] \
jenkins_agents.yml
```

### Variables description:
* **helmfile_url** - URL for Helmfile utility binary. *Example: **"https://arm1s11-eiffel052.eiffel.gic.ericsson.se:8443/nexus/service/local/repositories/eo-3pp-tools/content/com/helm/helmfile"***
* **helmfile_version** - Version for Helmfile utility binary. *Example: **"0.149.0"***


### Examples
#### For Install Helmfile:
```
ansible-playbook -i inventory.yml \
--private-key [your_ssh_key] \
-u [your_username] \
jenkins_agents.yml \
--extra-vars "helmfile_url=https://arm1s11-eiffel052.eiffel.gic.ericsson.se:8443/nexus/service/local/repositories/eo-3pp-tools/content/com/helm/helmfile" \
--extra-vars "helmfile_version=0.149.0"
```