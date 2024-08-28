# Role for Install Spinnaker CLI utility
### For run this role you could use ***jenkins_agents*** playbook:
```
ansible-playbook -i inventory.yml \
--private-key [your_ssh_key] \
-u [your_username] \
jenkins_agents.yml
```

### Variables description:
* **spin_url** - URL for Spinnaker CLI utility binary. *Example: **"https://storage.googleapis.com/spinnaker-artifacts/spin"***
* **spin_version** - Version for Spinnaker CLI utility binary. *Example: **"1.29.0"***


### Examples
#### For Install Spinnaker CLI:
```
ansible-playbook -i inventory.yml \
--private-key [your_ssh_key] \
-u [your_username] \
jenkins_agents.yml \
--extra-vars "spin_url=https://storage.googleapis.com/spinnaker-artifacts/spin" \
--extra-vars "spin_version=1.29.0"
```