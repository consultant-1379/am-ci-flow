# Documentation

This script will resolve the situation where EVNFM cannot lifecycle manage CNFs on the default cluster. i.e. the cluster EVNFM is deployed on.
When EVNFM attempts lifecycle operations, they will fail with the error message: 401 Unauthorized.
This will happen on deployments with version 1.22 or greater of Kubernetes.
The script will resolve the situation without having to terminate CNF instances.

The user provides a kube config file which has access to the default cluster.
The script will upload this kube config file to EVNFM, go into the database and replace the default config with it.

## Prerequisites

* A machine with the following:
    network access to the cluster
    python3 installed
* kube config file
    This is the kube config which will be uploaded to EVNFM.
    This kube config file will also be used by the script to access the cluster.
* Helm Binary
    The Helm Binary must be accessible to the script
    It must be at a minimum version 3 and in the supported range of the version of Kubernetes on the cluster.
* Kubernetes binary
    In the case of CCD
      kubectl must be available to the script
      It must be in the supported range of the version of Kubernetes on the cluster.
    In the case of OpenShift
      oc or kubectl must be available to the script
      It must be in the supported range of the version of OpenShift/Kubernetes on the cluster.
* No lifecycle operations against the default cluster are in progress in EVNFM
    This is to ensure no operations fail while the default config is being replaced.

## Execution

```python

python3 replace-default-cluster.py --help

```

This will show the arguments.

Note: the arguments are listed under optional, however namespace, config-path, kube-binary, helm-binary and helm-release are mandatory arguments.


```python

python3 replace-default-cluster.py  -n <evnfm-namespace> -c <absolute-path-to-config> -k <abolsute-path-to-kubernetes-binary> -hm <absolute-path-to-helm-binary> -hr <helm-release-name>

python3 replace-default-cluster.py  -n eric-eo-evnfm -c /home/myuser/replace-default.config -k /usr/local/bin/kubectl -hm /usr/local/bin/helm -hr evnfm-prod


```
