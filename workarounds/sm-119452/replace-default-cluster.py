#!/usr/bin/python3

from subprocess import Popen, PIPE
import sys
import logging
import argparse
import yaml
import requests
import os

def validate_args(args, helm_binary, kube_binary):
    logging.info(f"Validating the arguments. {args}")

    if not os.path.exists(args.config_path):
        raise ValueError(f"Supplied kube config file does not exist: {args.config_path}")
    if not os.path.exists(args.kube_binary):
        raise ValueError(f"Supplied kubernetes binary does not exist: {args.kube_binary}")
    if not os.path.exists(args.helm_binary):
        raise ValueError(f"Supplied helm binary does not exist: {args.helm_binary}")

    get_namespace_command = f"{kube_binary} get namespace {args.namespace}"
    logging.debug(f"Command is: {get_namespace_command}")
    get_namespace = Popen(get_namespace_command, shell=True, stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, error = get_namespace.communicate()
    if get_namespace.returncode is not 0:
        raise ValueError(f"Supplied namespace, {args.namespace}, cannot be found. Error: {error}")

    get_release_command = f"{helm_binary} get status -n {args.namespace} {args.helm_release}"
    logging.debug(f"Command is: {get_release_command}")
    get_release = Popen(get_release_command, shell=True, stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, error = get_release.communicate()
    if get_namespace.returncode is not 0:
        raise ValueError(f"Supplied release name, {args.release_name}, cannot be found. Error: {error}")

def parse_args():
    parser = argparse.ArgumentParser()
    parser.set_defaults(func=generate_func)

    parser.add_argument('-n', '--namespace', required=True, help='The namespace that evnfm is deployed in')
    parser.add_argument('-c', '--config-path', required=True, help='The absolute path to the kube config file which will replace the default config. This will also be used by the script to connect to the cluster')
    parser.add_argument('-k', '--kube-binary', required=True, help='The absolute path to the binary which will interact with the cluster, CCD: kubectl, OpenShift: oc')
    parser.add_argument('-hm', '--helm-binary', required=True, help='The absolute path to the Helm binary')
    parser.add_argument('-hr', '--helm-release', required=True, help='The name of the evnfm Helm release')
    parser.add_argument('-l', '--log-level', help='Log level, default is INFO', default='INFO')

    args = parser.parse_args(sys.argv[1:])
    logging.basicConfig(format='%(asctime)s %(levelname)s %(message)s', level=args.log_level.upper())
    args.func(args)

def generate_func(args):

    namespace= args.namespace
    config_path = args.config_path
    kube_binary = args.kube_binary
    helm_binary = args.helm_binary
    release_name = args.helm_release

    kube_binary = kube_binary + f" --kubeconfig={config_path}"
    helm_binary = helm_binary + f" --kubeconfig={config_path}"

    validate_args(args, helm_binary, kube_binary)

    retrieve_helm_values_command = f"{helm_binary} get values {release_name} -n {namespace}"
    retrieve_helm_values_command_output = Popen(retrieve_helm_values_command, shell=True, stdin=PIPE, stdout=PIPE, stderr=PIPE)
    values_output, values_error = retrieve_helm_values_command_output.communicate()

    if retrieve_helm_values_command_output.returncode is not 0:
        raise ValueError(f"Unable to find the helm release due to :{values_error}")

    values_dct = yaml.safe_load(values_output)

    vnfm_hostname = values_dct.get("global").get("hosts").get("vnfm")
    vnfm_username = values_dct.get("eric-eo-evnfm-nbi").get("eric-evnfm-rbac").get("defaultUser").get("username")
    vnfm_password = values_dct.get("eric-eo-evnfm-nbi").get("eric-evnfm-rbac").get("defaultUser").get("password")

    logging.info("EVNFM details are:")
    logging.info(f"Hostname: {vnfm_hostname}")
    logging.info(f"Username: {vnfm_username}")

    token_url = "https://" + vnfm_hostname + "/auth/v1"
    get_token_headers = {'Content-Type': 'application/json', 'X-login': vnfm_username, 'X-password': vnfm_password}
    token_response = requests.post(token_url, headers=get_token_headers, verify=False, timeout=30)
    if token_response is None or token_response.status_code is not 200:
        raise ValueError(f"Unable to get the token response due to {token_response}")

    logging.info(f"Token is: {token_response.text}")

    upload_cluster_config_file_url = "https://" + vnfm_hostname + "/vnflcm/v1/clusterconfigs"
    token_headers = {'cookie': 'JSESSIONID=' + token_response.text}
    cluster_config_file = {'clusterConfig': open(config_path, 'rb')}

    cluster_config_upload_response = requests.post(upload_cluster_config_file_url, files=cluster_config_file,
                                                   headers=token_headers, verify=False, timeout=180)
    if cluster_config_upload_response is None or cluster_config_upload_response.status_code is not 201:
        raise ValueError(f"Unable to upload cluster config due to {cluster_config_upload_response}")

    logging.info(f"Cluster config response: {cluster_config_upload_response.json()}")
    new_config_id=cluster_config_upload_response.json().get('id')

    logging.info("Finding out database cluster leader")
    patronictl_list_command = f"kubectl -n {namespace} exec -it application-manager-postgres-0 -c application-manager-postgres -- patronictl list -f yaml"
    patronictl_list_command_shell = Popen(patronictl_list_command, shell=True, stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, error = patronictl_list_command_shell.communicate()
    if patronictl_list_command_shell.returncode is not 0:
        raise ValueError(f"Unable to get postgres cluster members. {error}")

    members_dict = yaml.safe_load(output)
    for member in members_dict:
        if member.get("Role") == "Leader":
            db_pod_name = member.get("Member")
            break

    logging.info(f"Database cluster leader is {db_pod_name}")

    sql = f"""
    BEGIN;
    UPDATE app_cluster_config_file SET config_file_status = (SELECT config_file_status from app_cluster_config_file where config_file_name='default.config') WHERE id = '{new_config_id}';
    UPDATE app_cluster_config_file SET config_file_description = (SELECT config_file_description from app_cluster_config_file where config_file_name='default.config') WHERE id = '{new_config_id}';
    DELETE FROM app_cluster_config_file WHERE config_file_name='default.config';
    UPDATE app_cluster_config_file SET config_file_name='default.config' WHERE id = '{new_config_id}';
    COMMIT;
    """

    command = f'{kube_binary} -n {namespace} exec {db_pod_name} -c application-manager-postgres -- psql -U postgres -d orchestrator -c "{sql}"'

    logging.info( f'Kube command is: {command}')

    shell = Popen(command, shell=True, stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, error = shell.communicate()
    logging.info(f'Output is: {output}')
    if shell.returncode is not 0:
        logging.error(f'Error is: {error}')
    logging.info("Script finished")

def main():
    args = parse_args()


if __name__ == '__main__':
    main()
