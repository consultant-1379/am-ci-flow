# Instruction for prepare Kubernetes environment for Jenkins agent running
### Create resources in Cluster:
```
kubectl --kubeconfig [your_kubeconfig_file] apply -f .
kubectl --kubeconfig [your_kubeconfig_file] --namespace jenkins create token jenkins
kubectl --kubeconfig [your_kubeconfig_file] apply -f saSecret.yaml
```

### Get serviceaccount token:
```
chmod +x get-token.sh
./get-token.sh [your_kubeconfig_file]
```

### Create kubeconfig file:
```
cp jenkins-template.config jenkins-[your_cluster_name].config
```

### Set value in ***jenkins-[your_cluster_name].config*** file:
* **{{cluster name}}** - set cluster name. *Example: **haber002***
* **{{certificate data}}** - set certificate-authority-data
* **{{server URL}}** - set cluster URL. *Example: **https://haber002.k8s.gic.ericsson.se:6443***
* **{{token}}** - use value from ***get-token.sh*** script