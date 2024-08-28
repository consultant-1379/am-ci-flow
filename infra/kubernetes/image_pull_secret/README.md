# Adding the image pull secret to a namespace

The config.json file in this folder contains the amadm100 user logged into armdocker.

## create the secret in one namespace


```bash

kubectl create secret generic armdocker --from-file=.dockerconfigjson=config.json --type=kubernetes.io/dockerconfigjson --namespace <your_namespace>

```

## create the secret in all namespaces for a microservice

This is required for CI, each of our jenkins slaves has a namespace per service so that the jobs can execute in parallel on different jenkins slaves.

```bash

kubectl get namespace | grep <microservice> | awk '{ print $1 }' | xargs -I % kubectl create secret generic armdocker --from-file=
.dockerconfigjson=config.json --type=kubernetes.io/dockerconfigjson --namespace %

```

## using the secret

when you install your chart you will need to add another argument to the command

```bash

--set global.registry.pullSecret="armdocker"

```