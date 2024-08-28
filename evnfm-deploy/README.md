# Using the site_values.yaml file

This site_values file should be used when testing or debugging your changes. It can be configured to only deploy what services you require.

# Prerequisites

Before using the site_values.yaml file the following prerequisites are required

1. The namespace has been configured as per the EVNFM deployment guide and that all certs, service accounts etc have been configured correctly.
2. The site_values.yaml file has been edited and updated with the hosts as per the certs for the namespace where EVNFM is to be deployed

# Updating hosts in file

   * Replace iam and vnfm with your corresponding host names
    
    ```
    global:
      hosts:
        so: ""
        sdd: ""
        iam: "keycloak.dummy.hahn061.rnd.gic.ericsson.se"
        wano: ""
        vnfm: "evnfm.dummy.hahn061.rnd.gic.ericsson.se"
    ```

   * replace the following hostnames for registry and keycloak:
    ```
    eric-sec-access-mgmt:
      ingress:
        hostname: "keycloak.dummy.hahn061.rnd.gic.ericsson.se"

    # Fill only if deploying EO Container VNFM (EO EVNFM)
    eric-eo-evnfm:
      eric-lcm-container-registry:
        ingress:
          hostname: "registry.dummy.hahn061.rnd.gic.ericsson.se"
    ```

# Using the file

This file is configured to deploy the minimum services required to test the onboarding service. All other services have been disabled.

If you require any other additional services such as wfs, UI or orchestrator updates required to the following section:

```
tags:
  eoEvnfm: true
  eoVmvnfm: false
  eoSo: false
  eoWano: false
  onboarding: true
  ui: false
  # If turning on wfs or orchestrator please ensure eric-eo-evnfm-mb is also set to true
  wfs: false
  orchestrator: false
eric-eo-evnfm-mb:
  enabled: false
```

**Note** Do not edit anything else - no other services required to test EVNFM

# Disabled services

* logging disabled
* pm server disabled
* User management disabled
* HA for postgress
* BUR disabled
