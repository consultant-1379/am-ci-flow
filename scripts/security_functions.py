#!/usr/bin/env python

import argparse
import io
import logging
import os
import requests  # manual install needed 'python3 -m pip install requests'
import subprocess
import tarfile
import yaml      # manual install needed 'python3 -m pip install pyyaml'
import zipfile
import shutil

LOG = logging.getLogger(__name__)
logging.basicConfig(format="%(asctime)s %(levelname)s %(message)s", level=logging.INFO)

charts_to_exclude = ['eric-eo-evnfm', 'eric-data-object-storage-mn', 'eric-lcm-container-registry', 'eric-lcm-helm-chart-registry', 'eric-data-document-database-pg']

armdocker_link = "armdocker.rnd.ericsson.se/proj-am/releases/"
trivy_docker_image = "armdocker.rnd.ericsson.se/proj-adp-cicd-drop/trivy-inline-scan:latest"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--eoVersion',
        help="Version of the EO Chart to download",
        required=True
    )
    parser.add_argument(
        '--eoPreviousVersion',
        help="Version of the EO Chart to run a diff against",
        required=False
    )
    parser.add_argument(
        '--xray',
        help="Enables the collection of Xray reports",
        required=False,
        action='store_true'
    )
    parser.add_argument(
        '--anchore',
        help="Enables the generation of anchore reports",
        required=False,
        action='store_true'
    )
    parser.add_argument(
        '--trivy',
        help="Enables the generation of trivy reports",
        required=False,
        action='store_true'
    )
    parser.add_argument(
        '--print',
        help="print the version of dependencies",
        required=False,
        action='store_true'
    )
    parser.add_argument(
        '--debug',
        help="Enables debug logging",
        required=False,
        action='store_true'
    )
    args = parser.parse_args()

    download_chart(args.eoVersion)
    dependencies = extract_versions()
    if os.path.exists('eric-eo.tgz'):
        os.remove('eric-eo.tgz')

    if args.eoPreviousVersion:
        download_chart(args.eoPreviousVersion)
        old_dependencies = extract_versions()
        if os.path.exists('eric-eo.tgz'):
            os.remove('eric-eo.tgz')

    any_failures = False
    if args.debug:
        enable_debug_logs()

    if args.print:
        print_dependencies(dependencies)

    if args.xray:
        LOG.info("Running xray analysis")
        if download_xray(dependencies, 'new_xray'):
            any_failures = True
        if args.eoPreviousVersion:
            if download_xray(old_dependencies, 'old_xray'):
                any_failures = True
            script_location = os.path.dirname(os.path.realpath(__file__))
            result = subprocess.call([script_location + "/security_function_sort_diff_csvs.sh"])
            if result != 0:
                any_failures = True

    if args.anchore:
        LOG.info("Running anchore analysis")
        if generate_anchore(dependencies):
            any_failures = True

    if args.trivy:
        LOG.info("Running Trivy analysis")
        if generate_trivy(dependencies):
            any_failures = True

    if any_failures:
        exit('Some reports were not generated successfully. Please check logs for more details')


def enable_debug_logs():
    LOG.setLevel(logging.DEBUG)


def download_chart(eo_chart_version):
    chart_to_download = "eric-eo-" + eo_chart_version + ".tgz"
    LOG.info("Chart to download is " + chart_to_download)
    chart_url = "https://arm.seli.gic.ericsson.se/artifactory/proj-eo-drop-helm/eric-eo/" + chart_to_download
    LOG.info("Url will be " + chart_url)
    r = requests.get(chart_url, allow_redirects=True)
    with open('eric-eo.tgz', 'wb') as f:
        f.write(r.content)
    LOG.info("Finished downloading chart ")


def extract_versions():
    dependencies = []
    with tarfile.open('eric-eo.tgz', 'r') as tar:
        LOG.info("Extracting EO requirements.yaml from tar")
        member = tar.getmember("eric-eo/requirements.yaml")
        eo_requirements_file = tar.extractfile(member)
        LOG.info("Extracting values from EO requirements.yaml")
        documents = yaml.full_load(eo_requirements_file)
        for item in filter(filter_eo_evnfm_items, documents.__getitem__('dependencies')):
            dependencies.append(item)

        LOG.info("Extracting EVNFM requirements.yaml from tar")
        member = tar.getmember("eric-eo/charts/eric-eo-evnfm/Chart.yaml")
        evnfm_requirements_file = tar.extractfile(member)
        LOG.info("Extracting values from EVNFM requirements.yaml")
        documents = yaml.full_load(evnfm_requirements_file)
        for item in filter(filter_out_adp, documents.__getitem__('dependencies')):
            dependencies.append(item)
    return dependencies


def print_dependencies(dependencies):
    for dependency in dependencies:
        LOG.info(dependency['name'] + ":" + dependency['version'])
        LOG.debug(dependency)


def filter_eo_evnfm_items(dependency):
    if "tags" not in dependency:
        return False
    tags = dependency.get('tags')
    if "eoEvnfm" not in tags:
        return False
    if "eoSo" in tags:
        return False
    return filter_out_adp(dependency)


def filter_out_adp(dependency):
    if dependency.get("name") in charts_to_exclude:
        return False
    return True


def get_docker_version(dependency):
    if 'tags' in dependency and "eoEvnfm" in dependency.get('tags'):
        return dependency.get('version').replace('+1', '')
    else:
        return dependency.get('version').replace('+', '-')


def download_xray(dependencies, folder_name):
    any_failures = False
    if os.path.exists(folder_name):
        shutil.rmtree(folder_name)
    os.mkdir(folder_name)

    for dependency in dependencies:
        version = get_docker_version(dependency)
        chart = dependency.get('name') + ':' + version
        LOG.info("Downloading report for " + chart)
        url = ("https://arm.seli.gic.ericsson.se/ui/api/v1/xray/ui/component/exportComponentDetails"
               "?comp_name=proj-am/releases/" + chart +
               "&prefix=docker&format=csv&license=false&violations=false&security=true&exclude_unknown=false")
        headers = {'X-JFrog-Art-Api': 'AKCp5bBhBeBH1StEyF5jb1ZCrhWWJ97jkUCGFpcZvbnAqVSVAzH5RkzKCJi7dVdYJxjNDoCq9'}
        LOG.info("download url : " + url)

        with requests.get(url, headers=headers) as r:
            LOG.info(str(r.status_code))
            if r.status_code == 200:
                z = zipfile.ZipFile(io.BytesIO(r.content))
                z.extractall(folder_name)
            else:
                LOG.error("Error downloading xray for chart " + chart)
                any_failures = True
    return any_failures


def generate_anchore(dependencies):
    any_failures = False
    if not os.path.exists('anchore'):
        os.mkdir('anchore')

    for dependency in dependencies:
        image_name = dependency.get('name') + ":" + get_docker_version(dependency)
        image_path = armdocker_link + image_name
        script_location = os.path.dirname(os.path.realpath(__file__))
        result = subprocess.call([script_location + "/security_function_anchore.sh", image_path])
        if result != 0:
            any_failures = True
    return any_failures


def generate_trivy(dependencies):
    any_failures = False
    if not os.path.exists('trivy'):
        os.mkdir('trivy')

    for dependency in dependencies:
        image_name = dependency.get('name') + ":" + get_docker_version(dependency)
        image_path = armdocker_link + image_name
        script_location = os.path.dirname(os.path.realpath(__file__))
        result = subprocess.call([script_location + "/security_function_trivy.sh", image_path])
        if result != 0:
            any_failures = True
    subprocess.call([script_location + "/security_function_trivy.sh", "combine"])
    return any_failures


if __name__ == "__main__":
    main()

