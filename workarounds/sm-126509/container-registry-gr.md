# Deployment instructions of a DNS Loadbalancing based Active-active Geo-Redundant Container Registry in EVNFM 20.7.2

## Description of solution

The solution describes a way that EVNFM's Container Image Registry will be reachable for read operations
under the same FQDN on both active and standby sites simultaneously, regardless of EVNFM's switchover state.
This is achieved by registering the external IPs of both the active and the standby EVNFMs in the DNS server.

The container runtime on the worker node, which wants to download an image from Container Image Registry,
will contact the DNS server for resolution of the Container Registry FQDN (<EVNFM_DOCKER_REGISTRY_HOSTNAME> below),
and the authoritative DNS server or some intermediary DNS cache is expected select one of the associated IP addresses at random (or any other algorithm).

### Limitations

#### **DNS Time-to-leave configuration and Image pull retries**

When the container runtime on the worker node is trying to download a given image, it will try to resolve the image's FQDN. If the resolution provides
an IP address (EVNFM site) that's has no healthy container registry, the image pull will fail.
In that case the kubelet will retry the image pull indefinitely with a exponential backoff strategy ( min((10s * 2^retries), 300) = [10s, 20s, 40s, 80s, 160s, 300s, 300s, 300s, ...]).

The above mean that depending on the DNS TTL configuration for the Container Registry FQDN, as long as the TTL does not time out and the provided IP address
has no backing healthy Container Registry, the image pull will keep failing.

So it is recommended to have the DNS TTL for the Container Registry FQDN as low as possible to reduce possible CNF downtime.

#### **Upgradeability of the solution**

This solution is specific to EVNFM 20.7.2, and it is not guaranteed that it will work after an upgrade to an other software release of EVNFM.

#### **Support for EO-CM**

This solution does not consider integration of EO-CM with this release of EVNFM.

#### **Onboarding usecases**

This solutions expects that both of the Container Registries have the same content.
Due to how Container Registry Synchronization works in EVNFM 20.7.2, image synchronization will take place after an image has been uploaded.
Image synchronization is tied to a time schedule, by default its 15 minutes. Also, depending on network speeds between sites and size of images,
image synchronization can take up to an hour.

It is recommended to wait at least an hour between onboarding a new package and starting to use it for EVNFM LCM operations.

#### **EVNFM Switchover**

This solution only considers the active-active setup of the container registry provided by EVNFM for read usecases, it does not provide a solution for a fully active-active EVNFM. EVNFM needs to be switched over to the standby site manually so that onboarding and LCM usecases become available.
The FQDN for the active-active Container registry shall never be used for write operations.

#### **Container Registry unavailability**

During the installation of this solution, there is an approximately 30 minute window where the container registry FQDN will be unavailable. This is marked in the document at the right place.

## Preparation steps

### Variables

- `<USER_ID>` must match the `<eric-eo-evnfm-nbi.eric-evnfm-rbac.defaultUser.username>` in the site values file.
- `<USER_PASSWORD>` must match the `<eric-eo-evnfm-nbi.eric-evnfm-rbac.defaultUser.password>` in the site values file.
- `<INGRESS_EXTERNAL_IP>`
- `<EVNFM_GLOBAL_REGISTRY_FQDN>` must match the previously used `<EVNFM_DOCKER_REGISTRY_HOSTNAME>` as per *EO Cloud Native Installation Instructions*. This FQDN can be resolved to either external IPs of the active or the standby cluster at any time, regardless of EVNFM switchover state.
- `<EVNFM_LOCAL_REGISTRY_FQDN>` shall be a newly provisioned FQDN that will be used as the new `<EVNFM_DOCKER_REGISTRY_HOSTNAME>` as per *EO Cloud Native Installation Instructions*. This DNS entry shall be switched over as part of EVNFM product switchover.
- `<WORKDIR_ACTIVE>` shall be interpreted as per the *EO Cloud Native Geographical Redundancy Deployment Guide*
- `<WORKDIR_PASSIVE>` shall be interpreted as per the *EO Cloud Native Geographical Redundancy Deployment Guide*
- `<NAMESPACE_NAME>` shall be interpreted as per the *EO Cloud Native Installation Instructions*

### Maintenance window

For applying this solution, a maintenance window shall be set up, where no operations can be executed on EVNFM. Also make sure that the image synchronization has completed successfully and all the images are synced over.

### SSL Certificate preparation

Secure Sockets Layer (SSL) Certificate to be created for the `<EVNFM_LOCAL_REGISTRY_FQDN>`.

The following are the guidelines for the certificates.

A certificate bundle created which contains an SSL Certificate for the application (signed by a trusted certificate Authority) plus the Certificate Authority intermediate certificate. The application SSL certificate must be the first certificate in the bundle followed by the Certificate Authority intermediate certificate. Files must be named `<EVNFM_LOCAL_REGISTRY_FQDN>.crt`.

SSL keyfile required for each certificate. This is generated with the Certificate Signing Request (CSR). Files must be named `<EVNFM_LOCAL_REGISTRY_FQDN>.key`.

When using certificates signed by a private trust or an unrecognized signing authority, refer to the Certificate Management section of the EO Cloud Native Security User Guide for additional steps.

Any intermediate certificate used to sign an SSL certificate for any host mentioned in the preceding must be included in a file named intermediate-ca.crt. It is recommended that you include all signing certificates (from intermediate to root) in this file.

If all SSL certificates are signed by the same Certificate Authority, then it is sufficient that a single copy of the intermediate certificate and a single copy of the root certificate are in the intermediate-ca.crt file.

The key and cert files created shall be put into `<WORKDIR_ACTIVE>/certificates` and the `<WORKDIR_PASSIVE>/certificates` folders.

### Save `site_values.yaml` files

Create a backup of both the primary and the secondary site's `site_values.yaml` file. This is needed if a rollback needs to be implemented.

## Solution deployment steps

### Partial DNS setup

Set up DNS so that the newly provisioned `<EVNFM_LOCAL_REGISTRY_FQDN>` shall be pointing to the active site's external IP.
Set up DNS so that the `<EVNFM_GLOBAL_REGISTRY_FQDN>` shall be pointing to the active site's external IP.

The DNS load balancing solution for `<EVNFM_GLOBAL_REGISTRY_FQDN>` cannot be put in place until both of the sites have been upgraded with the new configuration.

### Configuration of workflow service on the active site

#### Set up container registry access configuration for workflow service on the active site

```shell
kubectl create secret  generic container-credentials --from-literal=url=<EVNFM_GLOBAL_REGISTRY_FQDN> --from-literal=userid=<USER_ID> --from-literal=userpasswd=<USER_PASSWORD> --namespace=<NAMESPACE_NAME>
```

#### `site_values.yaml` configurations on the active site

The following shall be merged to the site_values.yaml file of the active site.

```yaml
eric-eo-evnfm:
  eric-am-common-wfs:
    dockerRegistry:
      secret: 'container-credentials'
```

### Modify the default registry ingress to the local fqdn on the active site

The following shall be merged to the site_values.yaml file of the active site.

```yaml
eric-eo-evnfm:
  eric-lcm-container-registry:
    ingress:
      hostname: <EVNFM_LOCAL_REGISTRY_FQDN>
```

### Creating a new ingress on the active site

A new ingress configuration shall be created for the `<EVNFM_GLOBAL_REGISTRY_FQDN>` on the active site.

#### Uploading the SSL certificate on the active site

The following shall be executed with kubectl pointing to the active site.

```sh
kubectl create secret tls global-registry-tls-secret \
  --cert=<WORKDIR_ACTIVE>/certificates/<EVNFM_GLOBAL_REGISTRY_FQDN>.crt \
  --key=<WORKDIR_ACTIVE>/certificates/<EVNFM_GLOBAL_REGISTRY_FQDN>.key \
  --namespace=<NAMESPACE_NAME>
```

#### Creating the new HTTPProxy configuration on the active site

**Warning: After executing this step, Container registry will become unavailable for CNFs until the active site upgrade is done**
The following HTTPProxy configuration shall be added to `<WORKDIR_ACTIVE>/global-registry-ingress.yaml` and `<EVNFM_GLOBAL_REGISTRY_FQDN>` shall be replaced in the file with the FQDN.

```yaml
apiVersion: projectcontour.io/v1
kind: HTTPProxy
metadata:
  annotations:
    ingress.kubernetes.io/body-size: "0"
    ingress.kubernetes.io/ssl-redirect: "true"
    kubernetes.io/ingress.class: eo_iccr
  labels:
    app: eric-eo-evnfm
  name: global-container-registry-ingress
spec:
  routes:
  - conditions:
    - prefix: /v2
    services:
    - name: eric-lcm-container-registry-registry
      port: 5000
    timeoutPolicy:
      response: 1800s
  virtualhost:
    fqdn: <EVNFM_GLOBAL_REGISTRY_FQDN>
    tls:
      passthrough: false
      secretName: global-registry-tls-secret
```

The following shall be executed with kubectl pointing to the active site.

```sh
kubectl apply -f <WORKDIR_ACTIVE>/global-registry-ingress.yaml -n <NAMESPACE_NAME>
```

It will be marked as invalid by k8s because there will be 2 HTTPProxy objects referring to the same FQDN. After the next step this should be resolved automatically.

### Execute upgrade on the active site

`docker run --rm -v <PATH_TO_LOCAL_DOCKER_SOCKET>:/var/run/docker.sock -v - <WORKDIR_ACTIVE>:/workdir -v <PATH_TO_LOCAL_ETC_HOSTS>:/etc/hosts deployment-manager:<TARGET_EO_VERSION> upgrade --namespace <NAMESPACE_NAME> --skip-crds`

### Restart pods on the active site

The following pods need to be restarted after the upgrade of EVNFM

- `am-onboarding`
- `am-common-wfs`
- `gr-bur-orchestrator`

---

### Configuration of workflow service on the standby site

#### Set up container registry access configuration for workflow service

```shell
kubectl create secret  generic container-credentials --from-literal=url=<EVNFM_GLOBAL_REGISTRY_FQDN> --from-literal=userid=<USER_ID> --from-literal=userpasswd=<USER_PASSWORD> --namespace=<NAMESPACE_NAME>
```

#### `site_values.yaml` configurations

The following shall be merged to the site_values file of the standby site.

```yaml
eric-eo-evnfm:
  eric-am-common-wfs:
    dockerRegistry:
      secret: 'container-credentials'
```

### Modify the default registry ingress to the local fqdn on the standby site

The following shall be merged to the site_values.yaml file of the standby site.

```yaml
eric-eo-evnfm:
  eric-lcm-container-registry:
    ingress:
      hostname: <EVNFM_LOCAL_REGISTRY_FQDN>
```

### Creating a new ingress on the standby site

A new ingress configuration shall be created for the `<EVNFM_GLOBAL_REGISTRY_FQDN>` on the standby site.

#### Uploading the SSL certificate to the standby site

The following shall be executed with kubectl pointing to the standby site.

```sh
kubectl create secret tls global-registry-tls-secret \
  --cert=<WORKDIR_PASSIVE>/certificates/<EVNFM_GLOBAL_REGISTRY_FQDN>.crt \
  --key=<WORKDIR_PASSIVE>/certificates/<EVNFM_GLOBAL_REGISTRY_FQDN>.key \
  --namespace=<NAMESPACE_NAME>
```

#### Creating the new HTTPProxy configuration on the standby site

The following HTTPProxy configuration shall be added to `<WORKDIR_PASSIVE>/global-registry-ingress.yaml` and `<EVNFM_GLOBAL_REGISTRY_FQDN>` shall be replaced in the file with the FQDN.

```yaml
apiVersion: projectcontour.io/v1
kind: HTTPProxy
metadata:
  annotations:
    ingress.kubernetes.io/body-size: "0"
    ingress.kubernetes.io/ssl-redirect: "true"
    kubernetes.io/ingress.class: eo_iccr
  labels:
    app: eric-eo-evnfm
  name: global-container-registry-ingress
spec:
  routes:
  - conditions:
    - prefix: /v2
    services:
    - name: eric-lcm-container-registry-registry
      port: 5000
    timeoutPolicy:
      response: 1800s
  virtualhost:
    fqdn: <EVNFM_GLOBAL_REGISTRY_FQDN>
    tls:
      passthrough: false
      secretName: global-registry-tls-secret
```

The following shall be executed with kubectl pointing to the standby site.

```sh
kubectl apply -f <WORKDIR_PASSIVE>/global-registry-ingress.yaml -n <NAMESPACE_NAME>
```

It will be marked as invalid by k8s because there will be 2 HTTPProxy objects referring to the same FQDN. After the next step this should be resolved automatically.

### Execute upgrade on the standby site

`docker run --rm -v <PATH_TO_LOCAL_DOCKER_SOCKET>:/var/run/docker.sock -v - <WORKDIR_PASSIVE>:/workdir -v <PATH_TO_LOCAL_ETC_HOSTS>:/etc/hosts deployment-manager:<TARGET_EO_VERSION> upgrade --namespace <NAMESPACE_NAME> --skip-crds`

### Restart pods

The following pods need to be restarted after the upgrade of EVNFM

- `am-onboarding`
- `am-common-wfs`
- `gr-bur-orchestrator`


### Final DNS setup

The DNS load balancing solution for `<EVNFM_GLOBAL_REGISTRY_FQDN>` can be put in place now.
