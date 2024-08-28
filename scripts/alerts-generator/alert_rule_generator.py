#!/usr/bin/env python3

import json

# TODO:
# - Output summary: service list, PVC list

################################################################################
# Configuration
alerting_rules_file = '../collect-dependencies/evnfm-rules.yaml'  # evnfm-rules.yaml
fault_mapping_prefix= ''  # Shall be empty

specific_problem_prefix = 'EO '  # Example: prefix='EO ' (with space), human_name='CVNFM Database Unavailable' will result in faultName='CVNFMDatabaseUnavailable', specificProblem='EO CVNFM Database Unavailable'

services = [
    {
        'name': 'application-manager-postgres-bragent',
        'human_name': 'CVNFM Database BR Agent',
        'type': 'deployment',
        'flags': [],
        'pvcs': []
    },
    {
        'name': 'application-manager-postgres',
        'human_name': 'CVNFM Database',
        'type': 'statefulset',  # statefulset, deployment, daemonset
        'flags': ['cvnfmCritical'],
        'pvcs': [
            {
                'name_regex': 'storage-eric-data-search-engine-master-[0-9]',
                'human_name': 'CVNFM Application Database',
                'type': 'postgres'  # 'postgres'
            },
        ]
    },
    {
        'name': 'eric-adp-gui-aggregator-service',
        'human_name': 'GUI Aggregator',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-am-common-wfs-ui',
        'human_name': 'CVNFM GUI',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-am-common-wfs',
        'human_name': 'CVNFM Workflow Service',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-am-onboarding-service',
        'human_name': 'CVNFM Onboarding Service',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-eo-batch-manager',
        'human_name': 'Batch Manager',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-cloud-native-kvdb-rd-operand',
        'human_name': 'KVDB RD Operand',
        'type': 'deployment', # TODO: Confirm
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-cm-mediator-db-pg',
        'human_name': 'CM Mediator Database',
        'type': 'statefulset',
        'flags': ['cvnfmCritical'],
        'pvcs': [
            {
                'name_regex': 'pg-data-eric-cm-mediator-db-pg-[0-9]',
                'human_name': 'CM Mediator Database',
                'type': 'postgres'
            },
        ]
    },
    {
        'name': 'eric-cm-mediator-notifier',
        'human_name': 'CM Mediator Notifier',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-cm-mediator',
        'human_name': 'CM Mediator',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-cnom-server',
        'human_name': 'CNOM Server',
        'type': 'deployment',
        'flags': [],
        'pvcs': []
    },
    {
        'name': 'eric-ctrl-bro',
        'human_name': 'BR Orchestrator',
        'type': 'statefulset',
        'flags': [],
        'pvcs': [
            {
                'name_regex': 'backup-data-eric-ctrl-bro-[0-9]',
                'human_name': 'BR Orchestrator',
                'type': None
            },
        ]
    },
    {
        'name': 'eric-data-distributed-coordinator-ed-agent',
        'human_name': 'Distributed Coordinator ED Agent',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-data-distributed-coordinator-ed',
        'human_name': 'Distributed Coordinator ED',
        'type': 'statefulset',
        'flags': ['cvnfmCritical'],
        'pvcs': [
            {
                'name_regex': 'data-eric-data-distributed-coordinator-ed-[0-9]',
                'human_name': 'Distributed Coordinator ED',
                'type': None
            },
        ]
    },
    {
        'name': 'eric-data-key-value-database-rd',
        'human_name': 'KVDB RD',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-data-search-engine-data',
        'human_name': 'Search Engine Data Node',
        'type': 'statefulset',
        'flags': [],
        'pvcs': [
            {
                'name_regex': 'storage-eric-data-search-engine-data-[0-9]',
                'human_name': 'Search Engine Data Node',
                'type': None
            },
        ]
    },
    {
        'name': 'eric-data-search-engine-ingest',
        'human_name': 'Search Engine Ingest Node',
        'type': 'deployment',
        'flags': [],
        'pvcs': []
    },
    {
        'name': 'eric-data-search-engine-master',
        'human_name': 'Search Engine Master Node',
        'type': 'statefulset',
        'flags': [],
        'pvcs': [
            {
                'name_regex': 'storage-eric-data-search-engine-master-[0-9]',
                'human_name': 'Search Engine Master Node',
                'type': None
            },
        ]
    },
    {
        'name': 'eric-eo-api-gateway',
        'human_name': 'API Gateway',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-eo-common-br-agent',
        'human_name': 'Common BR Agent',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-eo-evnfm-crypto',
        'human_name': 'Crypto Service',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-eo-evnfm-mb',
        'human_name': 'CVNFM Message Bus',
        'type': 'statefulset',
        'flags': ['cvnfmCritical'],
        'pvcs': [
            {
                'name_regex': 'data-eric-eo-evnfm-mb-[0-9]',
                'human_name': 'EVNFM Message Bus',
                'type': None
            },
        ]
    },
    {
        'name': 'eric-eo-evnfm-nbi',
        'human_name': 'EVNFM NBI',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-eo-usermgmt-ui',
        'human_name': 'User Management UI',
        'type': 'deployment',
        'flags': [],
        'pvcs': []
    },
    {
        'name': 'eric-eo-usermgmt',
        'human_name': 'User Management',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-fh-alarm-handler-db-pg',
        'human_name': 'Alarm Handler Database',
        'type': 'statefulset',
        'flags': [],
        'pvcs': [
            {
                'name_regex': 'pg-data-eric-fh-alarm-handler-db-pg-[0-9]',
                'human_name': 'Alarm Handler Database',
                'type': 'postgres'
            },
        ]
    },
    {
        'name': 'eric-fh-alarm-handler',
        'human_name': 'Alarm Handler',
        'type': 'deployment',
        'flags': [],
        'pvcs': []
    },
    {
        'name': 'eric-fh-snmp-alarm-provider',
        'human_name': 'SNMP Alarm Provider',
        'type': 'deployment',
        'flags': [],
        'pvcs': []
    },
    {
        'name': 'eric-lcm-container-registry-registry',
        'human_name': 'Container Registry',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': [
            {
                'name_regex': 'eric-lcm-container-registry',
                'human_name': 'Container Registry',
                'type': None
            },
        ]
    },
    {
        'name': 'eric-lcm-helm-chart-registry',
        'human_name': 'Helm Chart Registry',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': [
            {
                'name_regex': 'eric-lcm-helm-chart-registry',
                'human_name': 'Helm Chart Registry',
                'type': None
            },
        ]
    },
    {
        'name': 'eric-log-shipper',
        'human_name': 'Log Shipper',
        'type': 'daemonset',
        'flags': [],
        'pvcs': []
    },
    {
        'name': 'eric-log-transformer',
        'human_name': 'Log Transformer',
        'type': 'deployment',
        'flags': [],
        'pvcs': []
    },
    {
        'name': 'eric-oss-common-postgres-bragent',
        'human_name': 'Common Database BR Agent',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-oss-common-postgres',
        'human_name': 'Common Database',
        'type': 'statefulset',
        'flags': ['cvnfmCritical'],
        'pvcs': [
            {
                'name_regex': 'pg-data-eric-oss-common-postgres-[0-9]',
                'human_name': 'Common Database',
                'type': 'postgres'
            },
        ]
    },
    {
        'name': 'eric-pm-kube-state-metrics',
        'human_name': 'PM Kube State Metrics',
        'type': 'deployment',
        'flags': [],
        'pvcs': []
    },
    {
        'name': 'eric-pm-server',
        'human_name': 'PM Server',
        'type': 'statefulset',
        'flags': [],
        'pvcs': [
            {
                'name_regex': 'storage-volume-eric-pm-server-[0-9]',
                'human_name': 'PM Server',
                'type': None
            },
        ]
    },
    {
        'name': 'eric-sec-access-mgmt-sip-oauth2',
        'human_name': 'IAM OAuth2',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-sec-access-mgmt',
        'human_name': 'IAM',
        'type': 'statefulset',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-sec-certm',
        'human_name': 'CertM',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': [],
        'special_fault_mappings': [
            {
                "faultName":"CertMCertificateToExpire",
                "specificProblem":"EO CertM Certificate Expiring Soon",
                "defaultSeverity":"Warning",
                "defaultDescription":"The certificate will expire in less than 90 days and should be renewed to prevent service failure",
                "defaultExpiration":3600,  # 1 hour
                "vendor":193,
                "code":9699329,
                "probableCause":351,
                "category":"ProcessingErrorAlarm",
                "createAlarm": True,
            },
            {
                "faultName": "CertMTrustedCertificateToExpire",
                "specificProblem": "EO CertM Trusted Certificate Expiring Soon",
                "defaultSeverity": "Warning",
                "defaultDescription": "The trusted certificate will expire in less than 90 days and should be renewed to prevent service failure",
                "defaultExpiration": 3600,  # 1 hour
                "vendor": 193,
                "code": 9699330,
                "probableCause": 351,
                "category": "ProcessingErrorAlarm",
                "createAlarm": True,
            },
        ]
    },
    {
        'name': 'eric-sec-key-management-main',
        'human_name': 'KMS',
        'type': 'statefulset',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-sec-sip-tls-main',
        'human_name': 'SIP TLS',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'eric-vnfm-orchestrator-service',
        'human_name': 'CVNFM Orchestrator',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'evnfm-alert-manager',
        'human_name': 'Alert Manager',
        'type': 'statefulset',
        'flags': [],
        'pvcs': [
            {
                'name_regex': 'eric-pm-alert-manager-storage-evnfm-alert-manager-[0-9]',
                'human_name': 'Alert Manager',
                'type': None
            },
        ]
    },
    {
        'name': 'evnfm-toscao',
        'human_name': 'TOSCAO Service',
        'type': 'deployment',
        'flags': ['cvnfmCritical'],
        'pvcs': []
    },
    {
        'name': 'idam-database-pg-bragent',
        'human_name': 'IAM Database BR Agent',
        'type': 'deployment',
        'flags': [],
        'pvcs': []
    },
    {
        'name': 'idam-database-pg',
        'human_name': 'IAM Database',
        'type': 'statefulset',
        'flags': ['cvnfmCritical'],
        'pvcs': [
            {
                'name_regex': 'pg-data-idam-database-pg-[0-9]',
                'human_name': 'IAM Database',
                'type': 'postgres'
            },
        ]
    },
    {
        'name': 'eric-data-object-storage-mn-mgt',
        'human_name': 'Object Storage Manager',
        'type': 'deployment',
        'flags': [],
        'pvcs': []
    },
    {
        'name': 'eric-data-object-storage-mn',
        'human_name': 'Object Storage',
        'type': 'statefulset',
        'flags': ['cvnfmCritical'],
        'pvcs': [
            {
                'name_regex': 'export-eric-data-object-storage-mn-[0-9]',
                'human_name': 'Object Storage',
                'type': None
            },
        ]
    },
    {
        'name': 'eric-tm-ingress-controller-cr-contour',
        'human_name': 'Contour Ingress Controller',
        'type': 'deployment',
        'flags': [],
        'pvcs': []
    },
    {
        'name': 'eric-tm-ingress-controller-cr-envoy',
        'human_name': 'Envoy Proxy',
        'type': 'daemonset',
        'flags': [],
        'pvcs': []
    },
    {
        'name': 'eric-gr-bur-orchestrator',
        'human_name': 'GR Orchestrator',
        'type': 'deployment',
        'flags': [],
        'pvcs': [],
        'special_fault_mappings': [
            {
                "faultName": "GrDisasterSwitchoverUnavailable",
                "specificProblem": "EO GR Switchover Unavailable",
                "defaultSeverity":"Major",
                "defaultDescription": "Georedundancy switchover is not available",
                "defaultExpiration": 0,
                "code": 10001,
            },
            {
                "faultName": "GrRegistrySyncStatus",
                "specificProblem": "EO GR Synchronization Failed",
                "defaultSeverity":"Major",
                "defaultDescription": "Failed container registry georedundancy synchronisation",
                "defaultExpiration": 0,
                "code": 10001,
            }
        ]
    },
]

bro_operations = [
    {
        'action': 'CREATE_BACKUP',
        'human_name': 'Create Backup',
        'name_in_description': 'create backup'
    },
    {
        'action': 'RESTORE',
        'human_name': 'Restore',
        'name_in_description': 'restore'
    },
    {
        'action': 'DELETE_BACKUP',
        'human_name': 'Delete Backup',
        'name_in_description': 'delete backup'
    },
]

applications = [
    {
        'name': 'eric-eo-cvnfm',
        'human_name': 'CVNFM',
        'flag': 'cvnfmCritical'
    },
]

################################################################################
# Implementation

alert_rules = ''
fault_mappings = {}  # service -> list of mappings

def validate_service(service):
    service_name = service['name']

    for field in ['name', 'human_name', 'type']:
        if type(service[field]) is not str or service[field] == '':
            raise ValueError("Service '{}' has invalid '{}' value: '{}'".format(service_name, field, service[field]))

    allowed_values = ['statefulset', 'deployment', 'daemonset']
    if service['type'] not in allowed_values:
        raise ValueError("Service '{}' has invalid 'type' value: '{}'".format(service_name, service['type']))

    allowed_values = ['cvnfmCritical']
    for flag in service['flags']:
        if flag not in allowed_values:
            raise ValueError("Service '{}' has invalid 'flags' value: '{}'".format(service_name, service['flags']))

def validate_pvc(pvc, service_name):
    for field in ['name_regex', 'human_name']:
        if type(pvc[field]) is not str or pvc[field] == '':
            raise ValueError("PVC of service '{}' has invalid '{}' value: '{}'".format(service_name, field, pvc[field]))

    allowed_values = [None, 'postgres']
    if pvc['type'] not in allowed_values:
        raise ValueError("PVC of Service '{}' has invalid 'type' value: '{}'".format(service_name, pvc['type']))

def add_rules_service_unavailable(service):
    global alert_rules
    global fault_mappings
    global specific_problem_prefix

    service_name = service['name']
    fault_name = service['human_name'].replace(' ','') + 'Unavailable'
    specific_problem = specific_problem_prefix + service['human_name'] + ' Unavailable'

    # Alerting rule
    expr_unavailable = None
    expr_expected = None
    if service['type'] == 'deployment':
        expr_unavailable = 'kube_deployment_status_replicas_unavailable{deployment="' + service['name'] + '"}'
        expr_expected =    'kube_deployment_status_replicas{deployment="' + service['name'] + '"}'
    elif service['type'] == 'statefulset':
        expr_unavailable = '(kube_statefulset_replicas{statefulset="' + service['name'] + '"} - kube_statefulset_status_replicas_available{statefulset="' + service['name'] + '"})'
        expr_expected =    'kube_statefulset_status_replicas{statefulset="' + service['name'] + '"}'
    elif service['type'] == 'daemonset':
        expr_unavailable = '(kube_daemonset_status_desired_number_scheduled{daemonset="' + service['name'] + '"} - kube_daemonset_status_number_available{daemonset="' + service['name'] + '"})'
        expr_expected =    'kube_daemonset_status_desired_number_scheduled{daemonset="' + service['name'] + '"}'
    else:
        raise ValueError("Unexpected service type: '{}'".format(service['type']))

    alert_rules = alert_rules + '''\
          - alert: ''' + fault_name + '''
            annotations:
              description: {{ printf "%q" "All {{ $value }} replicas are down, service unavailable" }}
              summary:     {{ printf "%q" "All {{ $value }} replicas are down, service unavailable" }}
            expr: ''' + expr_unavailable + ''' == ''' + expr_expected + ''' and ''' + expr_expected + ''' > 0
            for: 0m
            labels:
              severity: critical
              serviceName: ''' +  service['name'] + '''
              faultyResource: {{ printf "%q" "/no_model/namespace[name='{{ $labels.namespace }}']/''' + service['type'] + """[name='""" + service['name'] + """']" }}""" + '''
'''
    for flag in service['flags']:
        alert_rules = alert_rules + '''\
              cvnfmCritical: true
'''

    # Fault mapping
    if service_name not in fault_mappings:
        fault_mappings[service_name] = []

    fault_mappings[service_name].append({
        'faultName': fault_name,
        'specificProblem': specific_problem,
        'defaultSeverity': 'Critical',
        'defaultDescription': 'All replicas are down, service unavailable',
        'defaultExpiration': 0,
        "code": 10002,
    })


def add_rules_service_degraded(service):
    global alert_rules
    global fault_mappings
    global specific_problem_prefix

    service_name = service['name']
    fault_name = service['human_name'].replace(' ','') + 'Degraded'
    specific_problem = specific_problem_prefix + service['human_name'] + ' Degraded'

    # Alerting rule
    expr_unavailable = None
    expr_expected = None
    if service['type'] == 'deployment':
        expr_unavailable = 'kube_deployment_status_replicas_unavailable{deployment="' + service['name'] + '"}'
        expr_expected =    'kube_deployment_status_replicas{deployment="' + service['name'] + '"}'
    elif service['type'] == 'statefulset':
        expr_unavailable = '(kube_statefulset_replicas{statefulset="' + service['name'] + '"} - kube_statefulset_status_replicas_available{statefulset="' + service['name'] + '"})'
        expr_expected =    'kube_statefulset_status_replicas{statefulset="' + service['name'] + '"}'
    elif service['type'] == 'daemonset':
        expr_unavailable = '(kube_daemonset_status_desired_number_scheduled{daemonset="' + service['name'] + '"} - kube_daemonset_status_number_available{daemonset="' + service['name'] + '"})'
        expr_expected =    'kube_daemonset_status_desired_number_scheduled{daemonset="' + service['name'] + '"}'
    else:
        raise ValueError("Unexpected service type: '{}'".format(service['type']))

    alert_rules = alert_rules + '''\
          - alert: ''' + fault_name + '''
            annotations:
              description: {{ printf "%q" "{{ $value }} replica(s) are down, capacity, performance, resiliency may be degraded"}}
              summary:     {{ printf "%q" "{{ $value }} replica(s) are down, capacity, performance, resiliency may be degraded"}}
            expr: ''' + expr_unavailable + ''' > 0 and ''' + expr_expected + ''' > ''' + expr_unavailable + '''
            for: 0m
            labels:
              severity: major
              serviceName: ''' +  service['name'] + '''
              faultyResource: {{ printf "%q" "/no_model/namespace[name='{{ $labels.namespace }}']/''' + service['type'] + """[name='""" + service['name'] + """']" }}""" + '''
'''

    # Fault mapping
    if service_name not in fault_mappings:
        fault_mappings[service_name] = []

    fault_mappings[service_name].append({
        'faultName': fault_name,
        'specificProblem': specific_problem,
        'defaultSeverity': 'Major',
        'defaultDescription': 'Not all replicas are operational; capacity, performance, resiliency may be degraded',
        'defaultExpiration': 0,
        "code": 10002,
    })

def add_special_fault_mappings(service):
    global fault_mappings

    if 'special_fault_mappings' not in service:
        return

    service_name = service['name']

    # Fault mapping
    if service_name not in fault_mappings:
        fault_mappings[service_name] = []

    fault_mappings[service_name] = fault_mappings[service_name] + service['special_fault_mappings']

def add_rules_pvc_pending(pvc, service_name):
    global alert_rules
    global fault_mappings
    global specific_problem_prefix

    # service_name
    fault_name = pvc['human_name'].replace(' ','') + 'PVCPending'
    specific_problem = specific_problem_prefix + pvc['human_name'] + ' PVC Pending'

    # Alerting rule
    alert_rules = alert_rules + '''\
          - alert: ''' + fault_name + '''
            annotations:
              description: PersistentVolumeClaim phase is 'Pending'
              summary:     PersistentVolumeClaim phase is 'Pending'
            expr: kube_persistentvolumeclaim_status_phase{persistentvolumeclaim=~"''' + pvc['name_regex'] + '''",phase="Pending"} > 0
            for: 0m
            labels:
              severity: critical
              serviceName: ''' + service_name + '''
              faultyResource: {{ printf "%q" "/no_model/namespace[name='{{ $labels.namespace }}']/pvc[name='{{ $labels.persistentvolumeclaim }}']" }}
'''

    # Fault mapping
    if service_name not in fault_mappings:
        fault_mappings[service_name] = []

    fault_mappings[service_name].append({
        'faultName': fault_name,
        'specificProblem': specific_problem,
        'defaultSeverity': 'Critical',
        'defaultDescription': "PersistentVolumeClaim phase is 'Pending'",
        'defaultExpiration': 0,
        "code": 10002,
    })

def add_rules_pvc_lost(pvc, service_name):
    global alert_rules
    global fault_mappings
    global specific_problem_prefix

    # service_name
    fault_name = pvc['human_name'].replace(' ','') + 'PVCLost'
    specific_problem = specific_problem_prefix + pvc['human_name'] + ' PVC Lost'

    alert_rules = alert_rules + '''\
          - alert: ''' + fault_name + '''
            annotations:
              description: PersistentVolumeClaim phase is 'Lost'
              summary:     PersistentVolumeClaim phase is 'Lost'
            expr: kube_persistentvolumeclaim_status_phase{persistentvolumeclaim=~"''' + pvc['name_regex'] + '''",phase="Lost"} > 0
            for: 0m
            labels:
              severity: critical
              serviceName: ''' + service_name + '''
              faultyResource: {{ printf "%q" "/no_model/namespace[name='{{ $labels.namespace }}']/pvc[name='{{ $labels.persistentvolumeclaim }}']" }}
'''

    # Fault mapping
    if service_name not in fault_mappings:
        fault_mappings[service_name] = []

    fault_mappings[service_name].append({
        'faultName': fault_name,
        'specificProblem': specific_problem,
        'defaultSeverity': 'Critical',
        'defaultDescription': "PersistentVolumeClaim phase is 'Lost'",
        'defaultExpiration': 0,
        "code": 10002,
    })


def add_rules_pvc_pg_low_disk_space(pvc, service_name):
    global alert_rules
    global fault_mappings
    global specific_problem_prefix

    # service_name
    fault_name = pvc['human_name'].replace(' ','') + 'PVCLowDiskSpace'
    specific_problem = specific_problem_prefix + pvc['human_name'] + ' PVC Low Disk Space'

    # Alerting rule
    alert_rules = alert_rules + '''\
          - alert: ''' + fault_name + '''
            annotations:
              description: {{ printf "%q" "{{ $value | humanizePercentage }} of disk space is used"}}
              summary:     {{ printf "%q" "{{ $value | humanizePercentage }} of disk space is used"}}
            expr: pg_volume_stats_used_bytes / pg_volume_stats_capacity_bytes{persistentvolumeclaim=~"''' + pvc['name_regex'] + '''"} > 0.8
            labels:
              severity: warning
              serviceName: ''' + service_name + '''
              faultyResource: {{ printf "%q" "/no_model/namespace[name='{{ $labels.namespace }}']/pvc[name='{{ $labels.persistentvolumeclaim }}']" }}
'''

    # Fault mapping
    if service_name not in fault_mappings:
        fault_mappings[service_name] = []

    fault_mappings[service_name].append({
        'faultName': fault_name,
        'specificProblem': specific_problem,
        'defaultSeverity': 'Warning',
        'defaultDescription': "Over 80% of disk space is used",
        'defaultExpiration': 0,
        "code": 10002,
    })

def add_rules_app_availability(app):
    global alert_rules
    global fault_mappings
    global specific_problem_prefix

    service_name = app['name']
    fault_name = app['human_name'].replace(' ','') + 'CoreFunctionalityUnavailable'
    specific_problem = specific_problem_prefix + app['human_name'] + ' Core Functionality Unavailable'

    # Alerting rule
    alert_rules = alert_rules + '''\
          - alert: ''' + app['human_name'].replace('  ','') + '''CoreFunctionalityUnavailable
            annotations:
              description: At least one of core ''' + app['human_name'] + ''' functionality is not available due to failure of one or several microservices
              summary:     At least one of core ''' + app['human_name'] + ''' functionality is not available due to failure of one or several microservices
            expr: ALERTS{alertstate="firing", ''' + app['flag'] + '''="true"} > 0
            for: 0m
            labels:
              severity: critical
              serviceName: ''' + app['name'] + '''
              faultyResource: {{ printf "%q" "/no_model/namespace[name='{{ $labels.namespace }}']/application/''' +  app['name'] + '''" }}
'''

    # Fault mapping
    if service_name not in fault_mappings:
        fault_mappings[service_name] = []

    fault_mappings[service_name].append({
        'faultName': fault_name,
        'specificProblem': specific_problem,
        'defaultSeverity': 'Critical',
        'defaultDescription': "At least one of core " + app['human_name'] + " functionality is not available due to failure of one or several microservices",
        'defaultExpiration': 0,
        "code": 10002,
    })

def add_rules_bro_operation(bro_operation, service_name):
    global alert_rules
    global fault_mappings
    global specific_problem_prefix

    # service_name
    fault_name = bro_operation['human_name'].replace(' ','') + 'OperationFailed'
    specific_problem = specific_problem_prefix + bro_operation['human_name'] + ' Operation Failed'

    # Alerting rule
    alert_rules = alert_rules + '''\
          - alert: ''' + bro_operation['human_name'].replace(' ','') + '''OperationFailed
            annotations:
              description: Last ''' + bro_operation['name_in_description'] + ''' operation failed
              summary:     Last ''' + bro_operation['name_in_description'] + ''' operation failed for backup '{{ printf "%q" "{{ $labels.backup_name }}" }}'.
            expr: bro_operation_info{action="''' + bro_operation['action'] + '''", status!="SUCCESS"} > 0
            for: 0m
            labels:
              severity: major
              serviceName: ''' + service_name + '''
              faultyResource: {{ printf "%q" "/no_model/namespace[name='{{ $labels.namespace }}']/bro/backup[name='{{ $labels.backup_name }}']" }}
'''

    # Fault mapping
    if service_name not in fault_mappings:
        fault_mappings[service_name] = []

    fault_mappings[service_name].append({
        'faultName': fault_name,
        'specificProblem': specific_problem,
        'defaultSeverity': 'Major',
        'defaultDescription': "Last " + bro_operation['name_in_description'] + " operation failed",
        'defaultExpiration': 3600,  # 1 hour
        "code": 10002,
    })

def remove_helm_directives(helm_template):
    s = ''

    opened = 0
    for c in helm_template:
        if c == '{':
            opened = opened + 1
        elif c == '}' and opened > 0:
            opened = opened - 1
        elif c == "\n":
            opened = 0
            s = s + c
        else:
            if opened == 0:
                s = s + c

    return s

def validate_rules():
    global alert_rules

    try:
        import yaml
    except ModuleNotFoundError:
        print("No YAML validation performed because of no python YAML module installed. Make `pip install pyyaml`")
        return


    s = alert_rules
    s = remove_helm_directives(alert_rules)
    #print(s)

    yaml.safe_load(s)

def output_alerting_rules():
    global alert_rules
    global alerting_rules_file

    # print(alert_rules)

    f = open(alerting_rules_file, 'w')
    f.write(alert_rules)
    f.close()

def output_fault_mappings():
    global fault_mappings
    global fault_mapping_prefix

    for service_name, mappings_struct in fault_mappings.items():
        s = json.dumps(mappings_struct, indent=4)

        # print("*** " + service_name)
        # print(s)

        fault_mapping_file = fault_mapping_prefix + service_name + ".json"
        f = open(fault_mapping_file, 'w')
        f.write(s)
        f.close()

################################################################################


# Header
alert_rules = alert_rules + '''\
{{- if .Values.tags.eoEvnfm }}
apiVersion: v1
kind: ConfigMap
metadata:
  name: eric-eo-evnfm-alerting-rules
data:
  eric-eo-evnfm-alerting-rules.yml: |
    groups:'''

# Pods, PVCs
for service in services:
    # New group for each service
    alert_rules = alert_rules + '''
      - name: service_''' + service['name'] + '''
        rules:
'''
    validate_service(service)

    add_rules_service_unavailable(service)
    add_rules_service_degraded(service)
    for pvc in service['pvcs']:
        validate_pvc(pvc, service['name'])

        add_rules_pvc_pending(pvc, service['name'])
        add_rules_pvc_lost(pvc, service['name'])
        if pvc['type'] == 'postgres':
            add_rules_pvc_pg_low_disk_space(pvc, service['name'])

    add_special_fault_mappings(service)

# Availability
alert_rules = alert_rules + '''\
      - name: app_availability_alerts
        rules:
'''
for app in applications:
    add_rules_app_availability(app)

# Backup and restore
alert_rules = alert_rules + '''\
      - name: backup_and_restore_operations_alerts
        rules:
'''
for bro_operation in bro_operations:
    add_rules_bro_operation(bro_operation, 'eric-ctrl-bro')

# Footer
alert_rules = alert_rules + '''\
{{- end }}
'''

# Output
output_alerting_rules()
output_fault_mappings()

validate_rules()