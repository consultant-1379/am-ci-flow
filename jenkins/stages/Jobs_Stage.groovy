/*******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/
import static Boolean.parseBoolean
import static org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional
import static com.ericsson.orchestration.mgmt.libs.VnfmArtifact.getArtifactProperty
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.addQuotesToList
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.addQuotesToMap
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkArgs
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkNotBlank
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.pathExists
import static com.ericsson.orchestration.mgmt.libs.VnfmGerrit.getContent
import static com.ericsson.orchestration.mgmt.libs.VnfmGit.getChartPath
import static com.ericsson.orchestration.mgmt.libs.VnfmHelm.getStableHelmfileVersion
import static com.ericsson.orchestration.mgmt.libs.VnfmJenkins.getCredential
import static com.ericsson.orchestration.mgmt.libs.VnfmJenkins.getJobs
import static com.ericsson.orchestration.mgmt.libs.VnfmJenkins.getLockResourceLabels
import static com.ericsson.orchestration.mgmt.libs.VnfmUtils.getAppInfo


/* Stage for Discover Jenkins jobs. Use:
- Args:
    file(require): type String; Content file name
    description: type String; Description of the page; default ''
*/
def Discover(Map Args) {
    String stageName = 'Discover'
    Map argsList = [file: [value: Args['file'], type: 'string'],
                    description: [value: Args['description'], type: 'string', require: false]]
    String content


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['description'] = Args.containsKey('description') ? Args['description'] : ''


    stage('Jobs Discover') {
        ArrayList jobsAll = getJobs()

        println('Set content value...')
        content = """${Args['description']}
                    |<table>
                    | <tr>
                    |   <th></th>
                    |   <th>Name</th>
                    |   <th>Type</th>
                    |   <th>URL</th>
                    |   <th>Enable</th>
                    |   <th>Last trigger</th>
                    |   <th>Description</th>
                    | </tr>""".stripMargin()

        for(Map jobParams in jobsAll) {
            String color = '#F99F8D'
            if(jobParams['enable']) {
                color = '#0000ffff'
            }

            content = content + """ <tr style='background-color:${color};'>
                                  |   <td>${(jobParams['']+1)}</td>
                                  |   <td>${jobParams['name']}</td>
                                  |   <td>${jobParams['type']}</td>
                                  |   <td><a href='${jobParams['url']}'>[Jenkins]</a></td>
                                  |   <td>${jobParams['enable']}</td>
                                  |   <td>${jobParams['trigger']}</td>
                                  |   <td>${jobParams['description']}</td>
                                  | </tr>""".stripMargin()
        }
        content = content + '</table>'

        writeFile(file: Args['file'], text: content)
    }
}


/* Stage for Create/reload Spinnaker job. Use:
- VARs:
    SPINNAKER_ENDPOINT
- Args:
    name(require): type String; Name of the Spinnaker job
    project(require): type String; Name of the Spinnaker project
    template(require): type String; Name of the Spinnaker template
    path(require): type String; Path to templates
    configs: type Map; Configs of the Spinnaker job; default is [:]
    stage: type String; Name of the stage; default is 'Reload [name] pipeline'
*/
def CreateSpinnakerJob(Map Args) {
    String stageName = 'CreateSpinnakerJob'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    project: [value: Args['name'], type: 'string'],
                    template: [value: Args['template'], type: 'string'],
                    path: [value: Args['path'], type: 'string'],
                    configs: [value: Args['configs'], type: 'map', require: false],
                    stage: [value: Args['stage'], type: 'string', require: false]]
    def jobParams
    def jobTemplate
    String credId
    String comm


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['configs'] = Args.containsKey('configs') ? Args['configs'] : [:]
    Args['stage'] = Args.containsKey('stage') ? Args['stage'] : 'Reload ' + Args['name'] + ' pipeline'


    stage(Args['stage']) {
        dir(Args['path']) {
            println('Prepare parameters of the pipeline...')
            jobParams = [name: Args['name']]
            jobParams += [application: Args['project']]
            // Set pipelines configs
            jobParams += Args['configs'] ?: [:]

            println('Jobs params:')
            println(jobParams)

            println('Read pipeline template...')
            jobTemplate = readJSON(file: Args['template'] + '.json')

            println('Set parameters of the pipeline...')
            jobTemplate += jobParams

            println('Export updated template...')
            writeJSON(file: jobTemplate['name'], json: jobTemplate)

            println('Reload pipeline...')
            credId = getCredential(env.SPINNAKER_ENDPOINT)
            withCredentials([file(credentialsId: credId, variable: 'SPIN_CONFIG')]) {
                comm = """spin --config \$SPIN_CONFIG \\
                          | --gate-endpoint ${env.SPINNAKER_ENDPOINT} \\
                          | pipeline save \\
                          | --file ${jobTemplate['name']}""".stripMargin()
                sh(comm)
            }
        }
    }
}


/* Stage for Set Job Parameter. Use:
- Args:
    name(require): type String; Name of the job parameter
    job(require): type String; Name of the job
    artifact: type String; Path to the artifact value; default is ''
    stage: type String; Name of the stage; default is 'Set Job Parameter'
    skip: type Boolean; if true to skip the current stage; default is false
*/
def SetJobParameter(Map Args) {
    String stageName = 'SetJobParameter'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    job: [value: Args['job'], type: 'string'],
                    artifact: [value: Args['artifact'], type: 'string', require: false],
                    stage: [value: Args['stage'], type: 'string', require: false],
                    skip: [value: Args['skip'], type: 'bool', require: false]]


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['artifact'] = Args.containsKey('artifact') ? Args['artifact'] : ''
    Args['stage'] = Args.containsKey('stage') ? Args['stage'] : 'Set Job Parameter'
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false


    stage(Args['stage']) {
        if(Args['skip']) {
            env[Args['name']] = []
            markStageSkippedForConditional(STAGE_NAME)
        } else {
            switch(Args['job']) {
                case 'adp-fetch-upload':
                    /*- Use VARs:
                          APP_NAME
                      - Use Job's ENVs:
                          MAIN_CHART_NAME
                          CHART_NAME
                          CHART_REPO
                          CHART_VERSION
                          GERRIT_REFSPEC
                          ALLOW_DOWNGRADE
                          EVNFM_BRANCH
                    */
                    String appPath = getAppInfo(env.APP_NAME)['CHART_PATH']
                    String appRepo = getAppInfo(env.APP_NAME)['GIT_REPO_URL']
                    String helmDropRepo = getAppInfo(env.APP_NAME)['HELM_DROP_REPO']
                    String helmReleasedRepo = getAppInfo(env.APP_NAME)['HELM_RELEASED_REPO']
                    String helmInternalRepo = getAppInfo(env.APP_NAME)['HELM_INTERNAL_REPO']
                    String vcsBranch = checkNotBlank(env.EVNFM_BRANCH) ? env.EVNFM_BRANCH : 'master'
                    String chartName = env.MAIN_CHART_NAME ?: env.CHART_NAME

                    env[Args['name']] += ('\n' + 'APP_NAME=' + env.APP_NAME)
                    env[Args['name']] += ('\n' + 'CHART_PATH=' + appPath)
                    env[Args['name']] += ('\n' + 'GIT_REPO_URL=' + appRepo)
                    env[Args['name']] += ('\n' + 'HELM_DROP_REPO=' + helmDropRepo)
                    env[Args['name']] += ('\n' + 'HELM_RELEASED_REPO=' + helmReleasedRepo)
                    env[Args['name']] += ('\n' + 'HELM_INTERNAL_REPO=' + helmInternalRepo)
                    env[Args['name']] += ('\n' + 'VCS_BRANCH=' + vcsBranch)

                    switch(true) {
                        case checkNotBlank(chartName):
                            env[Args['name']] += ('\n' + 'CHART_NAME=' + chartName)
                            env[Args['name']] += ('\n' + 'CHART_REPO=' + env.CHART_REPO)
                            env[Args['name']] += ('\n' + 'CHART_VERSION=' + env.CHART_VERSION)
                        break
                        case checkNotBlank(env.GERRIT_REFSPEC):
                            env[Args['name']] += ('\n' + 'GERRIT_REFSPEC=' + env.GERRIT_REFSPEC)
                        break
                    }

                    if(env.ALLOW_DOWNGRADE) {
                        env[Args['name']] += ('\n' + 'ALLOW_DOWNGRADE=' + env.ALLOW_DOWNGRADE)
                    }
                break
                case 'adp-helmfile-fbu':
                    /*- Use Job's ENVs:
                          ALLOW_DOWNGRADE
                    */
                    String chartName = getArtifactProperty( path: Args['artifact'],
                                                            property: 'INT_CHART_NAME',
                                                            this)
                    String chartRepo = getArtifactProperty( path: Args['artifact'],
                                                            property: 'INT_CHART_REPO',
                                                            this)
                    String chartVersion = getArtifactProperty(path: Args['artifact'],
                                                              property: 'INT_CHART_VERSION',
                                                              this)
                    String downgrade = env.ALLOW_DOWNGRADE ? env.ALLOW_DOWNGRADE : 'false'

                    if (env.OFOC_SNAPSHOT_VERSION){
                        env[Args['name']] += ('\n' + 'CHART_NAME=' + chartName + ', ' + env.OFOC_GERRIT_PROJECT)
                        env[Args['name']] += ('\n' + 'CHART_REPO=' + chartRepo + ', ' + chartRepo)
                        env[Args['name']] += ('\n' + 'CHART_VERSION=' + chartVersion + ', ' + env.OFOC_SNAPSHOT_VERSION)
                    } else {
                        env[Args['name']] += ('\n' + 'CHART_NAME=' + chartName)
                        env[Args['name']] += ('\n' + 'CHART_REPO=' + chartRepo)
                        env[Args['name']] += ('\n' + 'CHART_VERSION=' + chartVersion)
                    }
                    env[Args['name']] += ('\n' + 'ALLOW_DOWNGRADE=' + downgrade)
                break
                case 'reserve-env':
                    /*- Use Job's ENVs:
                        ENV_LABEL
                        FLOW_URL_TAG
                    */
                    List<Map> params = new ArrayList<Map>()
                    String tag
                    String urlTag = env.FLOW_URL_TAG ?: 'EVNFM App Staging'
                    String envLabel = env.ENV_LABEL

                    // Set tag
                    switch(Args['name']) {
                        case ~/TESTNG(.*)/:
                            tag = 'TestNG'
                        break
                        case ~/PRIMARY(.*)/:
                            tag = 'Primary Site'
                            envLabel += '-primary'
                        break
                        case ~/SECONDARY(.*)/:
                            tag = 'Secondary Site'
                            envLabel += '-secondary'
                        break
                        default:
                            tag = 'Install'
                        break
                    }

                    params.add( name: 'ENV_LABEL',
                                type: 'string',
                                value: envLabel)
                    params.add( name: 'FLOW_URL',
                                type: 'string',
                                value: BUILD_URL + ':' + tag)
                    params.add( name: 'FLOW_URL_TAG',
                                type: 'string',
                                value: urlTag)
                    params.add( name: 'WAIT_TIME',
                                type: 'string',
                                value: '7200')

                    params = addQuotesToList(params)
                    env[Args['name']] = params.toString()
                break
                case 'resource-name':
                    String resourceName = getArtifactProperty(path: Args['artifact'],
                                                              property: 'RESOURCE_NAME',
                                                              this)
                    String tag = Args['name'].contains('TESTNG') ? 'TestNG' : 'Install'

                    env[Args['name']] = resourceName

                    addShortText( background: '',
                                  borderColor: 'white',
                                  text: tag + ': ' + env[Args['name']])
                break
                case 'helmfile-install':
                    /*- Use VARs:
                          HELMFILE_CHART_NAME
                          RELEASE_HELMFILE_REPO
                      - Job's ENVs:
                          JOB_TYPE
                          EO_HELMFILE_BASE_VERSION
                          CLEAN_RESOURCE_NAME
                          DEPLOYMENT_MANAGER_DOCKER_IMAGE
                          FULL_PATH_TO_SITE_VALUES_FILE
                          PATH_TO_SITE_VALUES_OVERRIDE_FILE
                          GR_ENABLE
                          EO_HELMFILE_PROJECT(for pointfix and pointfix-release)
                          EO_BRANCH(for pointfix and pointfix-release)
                          GERRIT_URL(for pointfix and pointfix-release)
                      - Use ENVs from Jenkins host:
                          GERRIT_HTTP_URL(for pointfix and pointfix-release)
                    */
                    List<Map> params = new ArrayList<Map>()
                    String chartName = env.HELMFILE_CHART_NAME
                    String chartRepo = env.RELEASE_HELMFILE_REPO
                    String chartVersion

                    switch(true) {
                        case checkNotBlank(env.EO_HELMFILE_BASE_VERSION):
                            chartVersion = env.EO_HELMFILE_BASE_VERSION
                            println('INFO: Helmfile chart version is ' + chartVersion)
                        break
                        case env.JOB_TYPE in ['pointfix', 'pointfix-release', 'ha-tests']:
                            String branch = env.EO_BRANCH ?: 'master'
                            String chartPath = getChartPath(project: env.EO_HELMFILE_PROJECT,
                                                            url: GERRIT_HTTP_URL,
                                                            branch: env.EO_BRANCH,
                                                            this)
                            String chartFile = chartPath + '/metadata.yaml'
                            String fileContent = getContent(project: env.EO_HELMFILE_PROJECT,
                                                            url: env.GERRIT_URL,
                                                            branch: branch,
                                                            file: chartFile,
                                                            this)

                            chartVersion = readYaml(text: fileContent)['version']
                            println('INFO: Helmfile chart version is ' + chartVersion)
                        break
                        default:
                            chartVersion = getStableHelmfileVersion(this)
                            println('INFO: Stable Helmfile Version is ' + chartVersion)
                        break
                    }

                    params.add( name: 'INT_CHART_NAME',
                                value: chartName)
                    params.add( name: 'INT_CHART_REPO',
                                value: chartRepo)
                    params.add( name: 'INT_CHART_VERSION',
                                value: chartVersion)
                    params.add( name: 'RESOURCE_NAME',
                                value: env.CLEAN_RESOURCE_NAME)
                    params.add( name: 'DEPLOYMENT_MANAGER_DOCKER_IMAGE',
                                value: env.DEPLOYMENT_MANAGER_DOCKER_IMAGE)
                    params.add( name: 'FULL_PATH_TO_SITE_VALUES_FILE',
                                value: env.FULL_PATH_TO_SITE_VALUES_FILE)
                    params.add( name: 'DEPLOYMENT_TYPE',
                                value: 'install')
                    params.add( name: 'CREATE_NAMESPACE',
                                type: 'bool',
                                value: true)
                    params.add( name: 'GR_ENABLE',
                                type: 'bool',
                                value: parseBoolean(env.GR_ENABLE))

                    if(env.PATH_TO_SITE_VALUES_OVERRIDE_FILE) {
                        params.add( name: 'PATH_TO_SITE_VALUES_OVERRIDE_FILE',
                                    value: env.PATH_TO_SITE_VALUES_OVERRIDE_FILE)
                    }

                    params = addQuotesToList(params)
                    env[Args['name']] = params.toString()
                break
                case 'helmfile-upgrade':
                    /*- Use Job's ENVs:
                          CLEAN_RESOURCE_NAME
                          DEPLOYMENT_MANAGER_DOCKER_IMAGE
                          FULL_PATH_TO_SITE_VALUES_FILE
                          UPGRADE_SITE_VALUES_OVERRIDE_FILE
                    */
                    List<Map> params = new ArrayList<Map>()
                    String chartName = getArtifactProperty( path: Args['artifact'],
                                                            property: 'INT_CHART_NAME',
                                                            this)
                    String chartRepo = getArtifactProperty( path: Args['artifact'],
                                                            property: 'INT_CHART_REPO',
                                                            this)
                    String chartVersion = getArtifactProperty(path: Args['artifact'],
                                                              property: 'INT_CHART_VERSION',
                                                              this)

                    params.add( name: 'INT_CHART_NAME',
                                value: chartName)
                    params.add( name: 'INT_CHART_REPO',
                                value: chartRepo)
                    params.add( name: 'INT_CHART_VERSION',
                                value: chartVersion)
                    params.add( name: 'RESOURCE_NAME',
                                value: env.CLEAN_RESOURCE_NAME)
                    params.add( name: 'DEPLOYMENT_MANAGER_DOCKER_IMAGE',
                                value: env.DEPLOYMENT_MANAGER_DOCKER_IMAGE)
                    params.add( name: 'FULL_PATH_TO_SITE_VALUES_FILE',
                                value: env.FULL_PATH_TO_SITE_VALUES_FILE)
                    params.add( name: 'PATH_TO_SITE_VALUES_OVERRIDE_FILE',
                                value: env.UPGRADE_SITE_VALUES_OVERRIDE_FILE)
                    params.add( name: 'DEPLOYMENT_TYPE',
                                value: 'upgrade')
                    params.add( name: 'CREATE_NAMESPACE',
                                type: 'bool',
                                value: false)

                    params = addQuotesToList(params)
                    env[Args['name']] = params.toString()
                break
                case 'helmfile-deploy':
                    /*- Use Job's ENVs:
                          TESTNG_RESOURCE_NAME
                          INSTALL_ENM_STUB
                          DRAC_ENABLE
                          FULL_PATH_TO_SITE_VALUES_FILE
                          DEPLOYMENT_MANAGER_DOCKER_IMAGE
                    */
                    List<Map> params = new ArrayList<Map>()
                    String chartName = getArtifactProperty( path: Args['artifact'],
                                                            property: 'INT_CHART_NAME',
                                                            this)
                    String chartRepo = getArtifactProperty( path: Args['artifact'],
                                                            property: 'INT_CHART_REPO',
                                                            this)
                    String chartVersion = getArtifactProperty(path: Args['artifact'],
                                                              property: 'INT_CHART_VERSION',
                                                              this)

                    params.add( name: 'INT_CHART_NAME',
                                value: chartName)
                    params.add( name: 'INT_CHART_REPO',
                                value: chartRepo)
                    params.add( name: 'INT_CHART_VERSION',
                                value: chartVersion)
                    params.add( name: 'RESOURCE_NAME',
                                value: env.TESTNG_RESOURCE_NAME)
                    params.add( name: 'INSTALL_ENM_STUB',
                                type: 'bool',
                                value: parseBoolean(env.INSTALL_ENM_STUB))
                    params.add( name: 'DRAC_ENABLE',
                                type: 'bool',
                                value: parseBoolean(env.DRAC_ENABLE))
                    params.add( name: 'FULL_PATH_TO_SITE_VALUES_FILE',
                                value: env.FULL_PATH_TO_SITE_VALUES_FILE)
                    params.add( name: 'DEPLOYMENT_MANAGER_DOCKER_IMAGE',
                                value: env.DEPLOYMENT_MANAGER_DOCKER_IMAGE)
                    params.add( name: 'DEPLOYMENT_TYPE',
                                value: 'install')
                    params.add( name: 'CREATE_NAMESPACE',
                                type: 'bool',
                                value: true)

                    params = addQuotesToList(params)
                    env[Args['name']] = params.toString()
                break
                case 'gr-helmfile-deploy':
                    /*- Use VARs:
                          BASE_OVERRIDE_FILE
                          PRIMARY_OVERRIDE_FILE(when name contains 'PRIMARY')
                          SECONDARY_OVERRIDE_FILE(when name contains 'SECONDARY')
                          HOSTNAME_TYPE
                      - Job's ENVs:
                          DEPLOYMENT_TYPE
                          TAGS
                          INT_CHART_NAME
                          INT_CHART_REPO
                          INT_CHART_VERSION
                          GAS_HOSTNAME
                          HELM_REGISTRY_HOSTNAME
                          VNFM_HOSTNAME
                          VNFM_REGISTRY_HOSTNAME
                          GLOBAL_VNFM_REGISTRY_HOSTNAME
                          PRIMARY_ENV((when name contains 'PRIMARY'))
                          SECONDARY_ENV(when name contains 'SECONDARY')
                          OSS_INTEGRATION_CI_REFSPEC
                    */
                    List<Map> params = new ArrayList<Map>()
                    String envName = Args['name'].contains('SECONDARY') ? env.SECONDARY_ENV : env.PRIMARY_ENV
                    String overrideFile = Args['name'].contains('SECONDARY') ? env.SECONDARY_OVERRIDE_FILE : env.PRIMARY_OVERRIDE_FILE
                    overrideFile = env.BASE_OVERRIDE_FILE + ', ' + overrideFile

                    // Set common parameters
                    params.add( name: 'DEPLOYMENT_TYPE',
                                value: env.DEPLOYMENT_TYPE)
                    params.add( name: 'CREATE_NAMESPACE',
                                type: 'bool',
                                value: true)
                    params.add( name: 'INT_CHART_NAME',
                                value: env.INT_CHART_NAME)
                    params.add( name: 'INT_CHART_REPO',
                                value: env.INT_CHART_REPO)
                    params.add( name: 'INT_CHART_VERSION',
                                value: env.INT_CHART_VERSION)
                    params.add( name: 'GAS_HOSTNAME',
                                value: env.GAS_HOSTNAME)
                    params.add( name: 'HELM_REGISTRY_HOSTNAME',
                                value: env.HELM_REGISTRY_HOSTNAME)
                    params.add( name: 'VNFM_HOSTNAME',
                                value: env.VNFM_HOSTNAME)
                    params.add( name: 'VNFM_REGISTRY_HOSTNAME',
                                value: env.VNFM_REGISTRY_HOSTNAME)
                    params.add( name: 'GLOBAL_VNFM_REGISTRY_HOSTNAME',
                                value: env.GLOBAL_VNFM_REGISTRY_HOSTNAME)
                    params.add( name: 'GR_SECONDARY_HOSTNAME',
                                value: env.GR_SECONDARY_HOSTNAME)
                    params.add( name: 'RESOURCE_NAME',
                                value: envName)
                    params.add( name: 'PATH_TO_SITE_VALUES_OVERRIDE_FILE',
                                value: overrideFile)

                    if(checkNotBlank(env.TAGS)) {
                        params.add( name: 'TAGS',
                                    value: env.TAGS)
                    }

                    if(checkNotBlank(env.HOSTNAME_TYPE)) {
                        params.add( name: 'HOSTNAME_TYPE',
                                    value: env.HOSTNAME_TYPE)
                    }

                    if(checkNotBlank(env.OSS_INTEGRATION_CI_REFSPEC)) {
                        params.add( name: 'JOB_TYPE',
                                    value: 'values-test')
                        params.add( name: 'OSS_INTEGRATION_CI_REFSPEC',
                                    value: env.OSS_INTEGRATION_CI_REFSPEC)
                    }

                    params = addQuotesToList(params)
                    env[Args['name']] = params.toString()
                break
                case 'adp-helmfile-deploy':
                    /*- Use Job's ENVs:
                          RESOURCE_NAME
                          DEPLOYMENT_TYPE
                          INT_CHART_NAME
                          INT_CHART_REPO
                          INT_CHART_VERSION
                          FULL_PATH_TO_SITE_VALUES_FILE
                          PATH_TO_SITE_VALUES_OVERRIDE_FILE
                          NAMESPACE
                          KUBECONFIG_FILE
                          PATH_TO_CERTIFICATES_FILES
                          GAS_HOSTNAME
                          HELM_REGISTRY_HOSTNAME
                          IAM_HOSTNAME
                          VNFM_HOSTNAME
                          VNFM_REGISTRY_HOSTNAME
                          GR_SECONDARY_HOSTNAME
                          INGRESS_IP
                          INGRESS_CLASS
                          USE_DM_PREPARE
                          DEPLOYMENT_MANAGER_DOCKER_IMAGE
                          TAGS
                          DEPLOY_ALL_CRDS
                          OSS_INTEGRATION_CI_REFSPEC
                    */
                    String platformType = 'openshift' in getLockResourceLabels(env.RESOURCE_NAME) ? 'openshift' : 'default'

                    env[Args['name']] += '\n' + 'DEPLOYMENT_TYPE=' + env.DEPLOYMENT_TYPE
                    env[Args['name']] += '\n' + 'INT_CHART_NAME=' + env.INT_CHART_NAME
                    env[Args['name']] += '\n' + 'INT_CHART_REPO=' + env.INT_CHART_REPO
                    env[Args['name']] += '\n' + 'INT_CHART_VERSION=' + env.INT_CHART_VERSION
                    env[Args['name']] += '\n' + 'FULL_PATH_TO_SITE_VALUES_FILE=' + env.FULL_PATH_TO_SITE_VALUES_FILE
                    env[Args['name']] += '\n' + 'PATH_TO_SITE_VALUES_OVERRIDE_FILE=' + env.PATH_TO_SITE_VALUES_OVERRIDE_FILE
                    env[Args['name']] += '\n' + 'NAMESPACE=' + env.NAMESPACE
                    env[Args['name']] += '\n' + 'KUBECONFIG_FILE=' + env.KUBECONFIG_FILE
                    env[Args['name']] += '\n' + 'PATH_TO_CERTIFICATES_FILES=eo-integration-ci/' + env.PATH_TO_CERTIFICATES_FILES
                    env[Args['name']] += '\n' + 'GAS_HOSTNAME=' + env.GAS_HOSTNAME
                    env[Args['name']] += '\n' + 'HELM_REGISTRY_HOSTNAME=' + env.HELM_REGISTRY_HOSTNAME
                    env[Args['name']] += '\n' + 'IAM_HOSTNAME=' + env.IAM_HOSTNAME
                    env[Args['name']] += '\n' + 'VNFM_HOSTNAME=' + env.VNFM_HOSTNAME
                    env[Args['name']] += '\n' + 'VNFM_REGISTRY_HOSTNAME=' + env.VNFM_REGISTRY_HOSTNAME
                    env[Args['name']] += '\n' + 'INGRESS_IP=' + env.INGRESS_IP
                    env[Args['name']] += '\n' + 'INGRESS_CLASS=' + env.INGRESS_CLASS
                    env[Args['name']] += '\n' + 'USE_DM_PREPARE=' + env.USE_DM_PREPARE
                    env[Args['name']] += '\n' + 'DEPLOYMENT_MANAGER_DOCKER_IMAGE=' + env.DEPLOYMENT_MANAGER_DOCKER_IMAGE
                    env[Args['name']] += '\n' + 'PLATFORM_TYPE=' + platformType
                    env[Args['name']] += '\n' + 'TAGS=' + env.TAGS
                    env[Args['name']] += '\n' + 'DEPLOY_ALL_CRDS=' + env.DEPLOY_ALL_CRDS

                    if(env.GR_HOSTNAME) {
                        env[Args['name']] += '\n' + 'GR_HOSTNAME=' + env.GR_HOSTNAME
                    }

                    if(env.GR_SECONDARY_HOSTNAME) {
                        env[Args['name']] += '\n' + 'GR_SECONDARY_HOSTNAME=' + env.GR_SECONDARY_HOSTNAME
                    }

                    if(env.GLOBAL_VNFM_REGISTRY_HOSTNAME) {
                        env[Args['name']] += '\n' + 'GLOBAL_VNFM_REGISTRY_HOSTNAME=' + env.GLOBAL_VNFM_REGISTRY_HOSTNAME
                    }

                    if(env.OSS_INTEGRATION_CI_REFSPEC) {
                        env[Args['name']] += '\n' + 'GERRIT_REFSPEC=' + env.OSS_INTEGRATION_CI_REFSPEC
                    }
                break
                case 'helmfile-release':
                    /*- Use Job's ENVs:
                          APP_NAME
                          CHART_NAME
                          CHART_REPO
                          CHART_VERSION
                          GERRIT_REFSPEC
                          EVNFM_BRANCH
                    */
                    List<Map> params = new ArrayList<Map>()
                    String gerritRefspec = '""'

                    if(Args['artifact']) {
                        env.CHART_NAME = getArtifactProperty( path: Args['artifact'],
                                                              property: 'CHART_NAME',
                                                              this)
                        env.CHART_REPO = getArtifactProperty( path: Args['artifact'],
                                                              property: 'CHART_REPO',
                                                              this)
                        env.CHART_VERSION = getArtifactProperty(path: Args['artifact'],
                                                                property: 'CHART_VERSION',
                                                                this)
                    } else {
                        env.CHART_NAME = env.CHART_NAME ? env.CHART_NAME : '""'
                        env.CHART_REPO = env.CHART_REPO ? env.CHART_REPO : '""'
                        env.CHART_VERSION = env.CHART_VERSION ? env.CHART_VERSION : '""'
                        gerritRefspec = env.GERRIT_REFSPEC ? env.GERRIT_REFSPEC : '""'
                    }

                    params.add( name: 'APP_NAME',
                                value: env.APP_NAME)
                    params.add( name: 'CHART_NAME',
                                value: env.CHART_NAME)
                    params.add( name: 'CHART_REPO',
                                value: env.CHART_REPO)
                    params.add( name: 'CHART_VERSION',
                                value: env.CHART_VERSION)
                    params.add( name: 'GERRIT_REFSPEC',
                                value: gerritRefspec)
                    params.add( name: 'EVNFM_BRANCH',
                                value: env.EVNFM_BRANCH)

                    params = addQuotesToList(params)
                    env[Args['name']] = params.toString()
                break
                case 'testng':
                    /*- Use Job's ENVs:
                          IDAM_USERNAME
                          IDAM_PASSWORD
                          AM_INTEGRATION_REFSPEC
                          TESTNG_SLAVE_LABEL
                    */
                    List<Map> params = new ArrayList<Map>()
                    String cluster = getArtifactProperty( path: Args['artifact'],
                                                          property: 'CLUSTER',
                                                          this)
                    String namespace = getArtifactProperty( path: Args['artifact'],
                                                            property: 'NAMESPACE',
                                                            this)
                    String hostHelm = getArtifactProperty(path: Args['artifact'],
                                                          property: 'HELM_REGISTRY_HOSTNAME',
                                                          this)
                    String hostIam = getArtifactProperty( path: Args['artifact'],
                                                          property: 'IAM_HOSTNAME',
                                                          this)
                    String hostVnfm = getArtifactProperty(path: Args['artifact'],
                                                          property: 'VNFM_HOSTNAME',
                                                          this)
                    String amIntegrationRefspec = env.AM_INTEGRATION_REFSPEC ? env.AM_INTEGRATION_REFSPEC : '""'

                    params.add( name: 'CLUSTER',
                                type: 'string',
                                value: cluster)
                    params.add( name: 'NAMESPACE',
                                type: 'string',
                                value: namespace)
                    params.add( name: 'HOST_HELM',
                                type: 'string',
                                value: hostHelm)
                    params.add( name: 'HOST_IAM',
                                type: 'string',
                                value: hostIam)
                    params.add( name: 'HOST_VNFM',
                                type: 'string',
                                value: hostVnfm)
                    params.add( name: 'AM_INTEGRATION_REFSPEC',
                                type: 'string',
                                value: amIntegrationRefspec)
                    params.add( name: 'IDAM_USERNAME',
                                type: 'string',
                                value: env.IDAM_USERNAME)
                    params.add( name: 'IDAM_PASSWORD',
                                type: 'string',
                                value: env.IDAM_PASSWORD)
                    params.add( name: 'SLAVE_LABEL',
                                type: 'string',
                                value: env.TESTNG_SLAVE_LABEL)
                    params.add( name: 'TEST_SUITES_FLOW',
                                type: 'string',
                                value: env.TEST_SUITES_FLOW)
                    params.add( name: 'TEST_THREAD_COUNT',
                                type: 'string',
                                value: env.TEST_THREAD_COUNT)
                    println(params)
                    params = addQuotesToList(params)
                    env[Args['name']] = params.toString()
                break
                case 'unlock':
                    /*- Use Job's ENVs:
                          CLEAN_RESOURCE_NAME(when name = 'CLEAN_UNLOCK_PARAMS')
                          TESTNG_RESOURCE_NAME(when name = 'TESTNG_UNLOCK_PARAMS')
                    */
                    List<Map> params = new ArrayList<Map>()
                    String resName

                    switch(true) {
                        case Args['artifact'] && pathExists(Args['artifact'], this):
                            resName = getArtifactProperty(path: Args['artifact'],
                                                          property: 'RESOURCE_NAME',
                                                          this)
                        break
                        case Args['name'] == 'CLEAN_UNLOCK_PARAMS':
                            resName = env.CLEAN_RESOURCE_NAME
                        break
                        case Args['name'] == 'TESTNG_UNLOCK_PARAMS':
                            resName = env.TESTNG_RESOURCE_NAME
                        break
                        default:
                            resName = '""'
                        break
                    }

                    params.add( name: 'ENV_NAME',
                                type: 'string',
                                value: resName)

                    params = addQuotesToList(params)
                    env[Args['name']] = params.toString()
                break
                case 'evnfm-prerelease':
                    /*- Use Job's ENVs:
                          CHART_NAME
                          CHART_VERSION
                          APP_NAME
                          JOB_TYPE
                          GERRIT_PROJECT
                          GERRIT_BRANCH
                          GERRIT_REFSPEC
                          SKIP_TESTNG
                    */
                    List<Map> params = new ArrayList<Map>()
                    String chartList = '""'
                    String gerritRefspec = '""'

                    if(env.CHART_NAME && env.CHART_VERSION) {
                        chartList = env.CHART_NAME + ':' + env.CHART_VERSION
                    } else {
                        gerritRefspec = env.GERRIT_REFSPEC
                    }

                    params.add( name: 'CHART_LIST',
                                type: 'string',
                                value: chartList)
                    params.add( name: 'GERRIT_REFSPEC',
                                type: 'string',
                                value: gerritRefspec)
                    params.add( name: 'APP_NAME',
                                type: 'string',
                                value: env.APP_NAME)
                    params.add( name: 'JOB_TYPE',
                                type: 'string',
                                value: env.JOB_TYPE)
                    params.add( name: 'SKIP_TESTNG',
                                type: 'bool',
                                value: env.SKIP_TESTNG.toBoolean())

                    if(env.JOB_TYPE == 'pointfix') {
                        params.add( name: 'EVNFM_BRANCH',
                                    type: 'string',
                                    value: env.GERRIT_BRANCH)
                        params.add( name: 'EO_BRANCH',
                                    type: 'string',
                                    value: env.GERRIT_BRANCH)
                    }

                    if(env.GERRIT_PROJECT == 'am-integration-charts') {
                        params.add( name: 'AM_INTEGRATION_REFSPEC',
                                    type: 'string',
                                    value: gerritRefspec)
                    }

                    params = addQuotesToList(params)
                    env[Args['name']] = params.toString()
                break
                case 'pre-release':
                    /*- Use Job's ENVs:
                          GERRIT_BRANCH
                          GERRIT_REFSPEC
                          GERRIT_TOPIC
                          SKIP_TESTNG
                    */
                    List<Map> params = new ArrayList<Map>()
                    String jobType = env.GERRIT_BRANCH.contains('_track') ? 'pointfix' : 'pre-release'

                    params.add( name: 'JOB_TYPE',
                                value: jobType)
                    params.add( name: 'GERRIT_BRANCH',
                                value: env.GERRIT_BRANCH)
                    params.add( name: 'GERRIT_REFSPEC',
                                value: env.GERRIT_REFSPEC)

                    if(checkNotBlank(env.GERRIT_TOPIC)) {
                        params.add( name: 'GERRIT_TOPIC',
                                    value: env.GERRIT_TOPIC)
                    }

                    if(env.SKIP_TESTNG) {
                        params.add( name: 'SKIP_TESTNG',
                                    value: parseBoolean(env.SKIP_TESTNG))
                    }

                    params = addQuotesToList(params)
                    env[Args['name']] = params.toString()
                break
                case 'release':
                    /*- Use Job's ENVs:
                          GERRIT_BRANCH
                          GERRIT_CHANGE_URL
                          GERRIT_CHANGE_SUBJECT
                          GERRIT_CHANGE_ID
                          GERRIT_CHANGE_OWNER
                    */
                    List<Map> params = new ArrayList<Map>()
                    String jobType = env.GERRIT_BRANCH.contains('_track') ? 'pointfix' : 'base'

                    params.add( name: 'TYPE',
                                value: jobType)
                    params.add( name: 'BRANCH',
                                value: env.GERRIT_BRANCH)
                    params.add( name: 'GERRIT_CHANGE_URL',
                                value: env.GERRIT_CHANGE_URL)
                    params.add( name: 'GERRIT_CHANGE_SUBJECT',
                                value: env.GERRIT_CHANGE_SUBJECT)
                    params.add( name: 'GERRIT_CHANGE_ID',
                                value: env.GERRIT_CHANGE_ID)

                    if(env.GERRIT_CHANGE_OWNER) {
                        params.add( name: 'GERRIT_CHANGE_OWNER',
                                    value: env.GERRIT_CHANGE_OWNER)
                    }

                    params = addQuotesToList(params)
                    env[Args['name']] = params.toString()
                break
                case 'ha-tests':
                    /*- Use Jobs ENVs:
                          PATH_TO_TEST
                          GERRIT_BRANCH
                          GERRIT_REFSPEC
                    */
                    List<Map> params = new ArrayList<Map>()
                    String cluster = getArtifactProperty( path: Args['artifact'],
                                                          property: 'CLUSTER',
                                                          this)
                    String namespace = getArtifactProperty( path: Args['artifact'],
                                                            property: 'NAMESPACE',
                                                            this)
                    String hostVnfm = getArtifactProperty(path: Args['artifact'],
                                                          property: 'VNFM_HOSTNAME',
                                                          this)
                    String hostName = hostVnfm.minus(hostVnfm.split('\\.')[0] + '.')

                    params.add( name: 'CLUSTER',
                                type: 'string',
                                value: cluster)
                    params.add( name: 'NAMESPACE',
                                type: 'string',
                                value: namespace)
                    params.add( name: 'HOSTNAME',
                                type: 'string',
                                value: hostName)
                    params.add( name: 'PATH_TO_TEST',
                                type: 'string',
                                value: env.PATH_TO_TEST)
                    params.add( name: 'GERRIT_BRANCH',
                                type: 'string',
                                value: env.GERRIT_BRANCH)
                    params.add( name: 'GERRIT_REFSPEC',
                                type: 'string',
                                value: env.GERRIT_REFSPEC)

                    println(params)
                    params = addQuotesToList(params)
                    env[Args['name']] = params.toString()
                break
                case 'ha-deploy':
                    /*- Use Job's ENVs:
                          ENV_LABEL
                          PATH_TO_SITE_VALUES_OVERRIDE_FILE
                          EO_HELMFILE_BASE_VERSION
                    */
                    List<Map> params = new ArrayList<Map>()

                    params.add( name: 'ENV_LABEL',
                                type: 'string',
                                value: env.ENV_LABEL)
                    params.add( name: 'PATH_TO_SITE_VALUES_OVERRIDE_FILE',
                                type: 'string',
                                value: env.PATH_TO_SITE_VALUES_OVERRIDE_FILE)

                    if(checkNotBlank(env.EO_HELMFILE_BASE_VERSION)) {
                        params.add( name: 'EO_HELMFILE_BASE_VERSION',
                                    type: 'string',
                                    value: env.EO_HELMFILE_BASE_VERSION)
                    }

                    params = addQuotesToList(params)
                    env[Args['name']] = params.toString()
                break
                case 'gr-deploy':
                    /*- Use VARs:
                          HELMFILE_CHART_NAME
                          RELEASE_HELMFILE_REPO
                      - Job's ENVs:
                          PRIMARY_ENV
                          SECONDARY_ENV
                    */
                    List<Map> params = new ArrayList<Map>()
                    String deployType
                    String chartName
                    String chartRepo
                    String chartVersion

                    println('INFO: Get Chart Version...')
                    switch(Args['name']) {
                        case ~/DEPLOY(.*)/:
                            deployType = 'install'
                            chartName = env.HELMFILE_CHART_NAME
                            chartRepo = env.RELEASE_HELMFILE_REPO
                            chartVersion = getStableHelmfileVersion(this)
                        break
                        default:
                            deployType = 'upgrade'
                            chartVersion = getArtifactProperty( path: Args['artifact'],
                                                                property: 'INT_CHART_VERSION',
                                                                this)
                            chartName = getArtifactProperty(path: Args['artifact'],
                                                            property: 'INT_CHART_NAME',
                                                            this)
                            chartRepo = getArtifactProperty(path: Args['artifact'],
                                                            property: 'INT_CHART_REPO',
                                                            this)
                        break
                    }
                    println('INFO: Chart Version is ' + chartVersion)

                    println('INFO: Set parameters...')
                    params.add( name: 'DEPLOYMENT_TYPE',
                                value: deployType)
                    params.add( name: 'INT_CHART_NAME',
                                value: chartName)
                    params.add( name: 'INT_CHART_REPO',
                                value: chartRepo)
                    params.add( name: 'INT_CHART_VERSION',
                                value: chartVersion)
                    params.add( name: 'PRIMARY_ENV',
                                value: env.PRIMARY_ENV)
                    params.add( name: 'SECONDARY_ENV',
                                value: env.SECONDARY_ENV)

                    params = addQuotesToList(params)
                    env[Args['name']] = params.toString()
                break
                case 'gr-tests':
                    /*- Use VARs:
                          ARTIFACTS_DIR
                    */
                    List<Map> params = new ArrayList<Map>()
                    String filePath = env.ARTIFACTS_DIR + '/' + Args['artifact']
                    String cluster
                    String namespace

                    // Check the artifact
                    switch(true) {
                        case fileExists(filePath):
                            println('INFO: Artifact exists with path ' + filePath)
                        break
                        case fileExists(Args['artifact']):
                            filePath = Args['artifact']
                            println('INFO: Artifact exists with path ' + Args['artifact'])
                        break
                        default:
                            error(message: 'Artifact "' + Args['artifact'] + '" is absent')
                        break
                    }

                    cluster = getArtifactProperty(path: filePath,
                                                  property: 'CLUSTER',
                                                  this)
                    namespace = getArtifactProperty(path: filePath,
                                                    property: 'NAMESPACE',
                                                    this)

                    params.add( name: 'CLUSTER',
                                value: cluster)
                    params.add( name: 'NAMESPACE',
                                value: namespace)

                    params = addQuotesToList(params)
                    env[Args['name']] = params.toString()
                break
                case 'get-chart-version':
                    List<Map> params = new ArrayList<Map>()

                    params.add( name: 'CHART_NAME',
                                value: 'eo-helmfile')
                    params.add( name: 'VERSION_TYPE',
                                value: 'stable')

                    params = addQuotesToList(params)
                    env[Args['name']] = params.toString()
                break
            }

            println('INFO: List parameters for ' + Args['name'] + '...')
            println(env[Args['name']])
        }
    }
}


/* Stage for Set Parameter for Spinnaker job. Use:
- Args:
    name(require): type String; Name of the job parameter
    job(require): type String; Name of the job
    stage: type String; Name of the stage; default is 'Set Job Parameter'
    skip: type Boolean; if true to skip the current stage; default is false
*/
def SetSpinnakerParameter(Map Args) {
    String stageName = 'SetSpinnakerParameter'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    job: [value: Args['job'], type: 'string'],
                    stage: [value: Args['stage'], type: 'string', require: false],
                    skip: [value: Args['skip'], type: 'bool', require: false]]


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['artifact'] = Args.containsKey('artifact') ? Args['artifact'] : ''
    Args['stage'] = Args.containsKey('stage') ? Args['stage'] : 'Set Spinnaker Job Parameter'
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false


    stage(Args['stage']) {
        if(Args['skip']) {
            env[Args['name']] = []
            markStageSkippedForConditional(STAGE_NAME)
        } else {
            switch(Args['job']) {
                case 'evnfm-release':
                    /* Use Job's ENVs:
                          GERRIT_REFSPEC
                          GERRIT_BRANCH
                          GIT_COMMIT_AUTHOR
                          GIT_COMMIT_AUTHOR_EMAIL
                          GIT_COMMIT_SUMMARY
                          GERRIT_CHANGE_URL
                          GERRIT_CHANGE_NUMBER
                          GERRIT_PATCHSET_NUMBER
                          JOB_NAME
                          BUILD_NUMBER
                    */
                    Map params = [:]
                    params += [GERRIT_REFSPEC: env.GERRIT_REFSPEC]
                    params += [GERRIT_BRANCH: env.GERRIT_BRANCH]
                    params += [GIT_COMMIT_AUTHOR: env.GIT_COMMIT_AUTHOR]
                    params += [GIT_COMMIT_AUTHOR_EMAIL: env.GIT_COMMIT_AUTHOR_EMAIL]
                    params += [GIT_COMMIT_SUMMARY: env.GIT_COMMIT_SUMMARY]
                    params += [GERRIT_CHANGE_URL: env.GERRIT_CHANGE_URL]
                    params += [GERRIT_CHANGE_NUMBER: env.GERRIT_CHANGE_NUMBER]
                    params += [GERRIT_PATCHSET_NUMBER: env.GERRIT_PATCHSET_NUMBER]
                    params += [parameters: [TRIGGER: env.JOB_NAME + ' Build #' + env.BUILD_NUMBER]]

                    params = addQuotesToMap(params)
                    env[Args['name']] = params.toString()
                break
            }

            println(Args['name'] + ' parameters:')
            println(env[Args['name']])
        }
    }
}

return this