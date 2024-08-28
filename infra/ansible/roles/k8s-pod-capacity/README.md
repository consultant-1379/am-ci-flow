# Role for update max pods capacity in Kubelet configs
### For run this role you could use ***kubernetes*** playbook:
```
ansible-playbook -i inventory.yml --key-file [your_ssh_key] -u [your_username] kubernetes.yml
```

### Tags description:
* **pods_capacity** - update pods capacity in Kubelet configs, used variable: ***maxPods***
* **restart** - restart Kubelet service

### Variables description:
* **maxPods** - max pods capacity. *Example: **72***


### Examples
#### For Update max pods capacity:
```
ansible-playbook -i inventory.yml --key-file [your_ssh_key] -u [your_username] kubernetes.yml --extra-vars "maxPods=[your_max_pods_value]" --tags pods_capacity
```
#### For Update max pods capacity and approve new updates:
```
ansible-playbook -i inventory.yml --key-file [your_ssh_key] -u [your_username] kubernetes.yml --extra-vars "maxPods=[your_max_pods_value]" --tags pods_capacity --tags restart
```
#### For Restart Kubelet service:
```
ansible-playbook -i inventory.yml --key-file [your_ssh_key] -u [your_username] kubernetes.yml --tags restart
```
