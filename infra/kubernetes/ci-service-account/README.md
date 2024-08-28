# K8S Cluster administration

## service account for compute engines

We can no longer use the default admin kube config file for the jenkins compute engines.

The ci-service-account.yaml file can be used to create a service account in a cluster to be used in the CI flow.
Then follow the instructions on [this page](https://confluence.lmera.ericsson.se/display/AD/1045-IAM#id-1045-IAM-ServiceAccountforCI/CD) to create the kube config using the service account created above.
Finally, use the playbook in the ansible folder to copy this config onto all the compute engine nodes.