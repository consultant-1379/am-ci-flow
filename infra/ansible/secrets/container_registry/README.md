This is the htpasswd file required by the container registry which contains the username and password.

It is created using the htpasswd tool.

For more details have a look at the container registry documentation: https://adp.ericsson.se/marketplace/container-registry/documentation/general/service-deployment-guide

username: vnfm
password: ciTesting123!

## TO DO: Use environment variables instead of hardcode  

## creating the secret in multiple namespaces

To create the secret in the CI namespaces in one go I used xargs

```bash

cat hosts/rose_namespaces | grep evnfm | xargs -I \% kubectl create secret generic container-registry-users-secret --from-file=htpasswd=./secrets/container_registry/htpasswd  -n \%

```

What this command is doing is:

* cat prints the CI namespaces to the console
* grep filters down the namespaces to the ones which we want. Change this to the filter you want
* xargs takes each input line and executes the command substituting the namespace for %