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
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils
import static com.ericsson.orchestration.mgmt.libs.VnfmArtifact.uploadToNexus
import static com.ericsson.orchestration.mgmt.libs.VnfmDocker.getDockerImagePath
import static com.ericsson.orchestration.mgmt.libs.VnfmDocker.pullDockerImage
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkArgs
import static com.ericsson.orchestration.mgmt.libs.VnfmGit.getChartPath
import static com.ericsson.orchestration.mgmt.libs.VnfmJenkins.getCredential
import static com.ericsson.orchestration.mgmt.libs.VnfmUtils.overwriteYaml
import static com.ericsson.orchestration.mgmt.libs.VnfmHelm.getHelmRepository


/* Stage for Package Helm Chart. Use:
- Job's ENVs:
    CHART_NAME
    CHART_VERSION
    DOCKER_REPO
    IMAGE_VERSION
    HELM_URL
    GLOBAL_HELM_FOLDER
    UPDATE_SOURCE
- Args:
    project(require): type String; Name of the project
    upload: type Boolean; If true to upload Helm chart; default is true
    folder: type String; Path of the chart folder; default is 'charts'
    chart: type String; Name(s) of the Helm Chart(s); delimeter is ','; default is env.CHART_NAME
    version: type String; Version of the Helm Chart; default is env.CHART_VERSION
    stage: type String; Name of the stage; default is 'Package Helm Chart'
*/
def PackageChart(Map Args) {
    String stageName = 'PackageChart'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    upload: [value: Args['upload'], type: 'bool', require: false],
                    folder: [value: Args['folder'], type: 'string', require: false],
                    chart: [value: Args['chart'], type: 'string', require: false],
                    version: [value: Args['version'], type: 'string', require: false],
                    stage: [value: Args['stage'], type: 'string', require: false]]
    String comm
    String dockerImage = getDockerImagePath('bob-adp')
    String dockerImageUpload = getDockerImagePath('adp-int')
    String packageArgs = ''
    String filePath
    def tempFile


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['upload'] = Args.containsKey('upload') ? Args['upload'] : true
    Args['folder'] = Args['folder'] ?: 'charts'
    Args['chart'] = Args.containsKey('chart') ? Args['chart'] : env.CHART_NAME
    Args['version'] = Args.containsKey('version') ? Args['version'] : env.CHART_VERSION
    Args['stage'] = Args.containsKey('stage') ? Args['stage'] : 'Package Helm Chart'


    stage(Args['stage']) {
        dir(Args['project']) {
            switch(Args['project']) {
                case 'am-common-wfs':
                    packageArgs = """ --replace VERSION=${env.IMAGE_VERSION} \\
                                    | --replace SLES_PG10_IMAGE=${env.IMAGE_VERSION}""".stripMargin()

                    filePath = 'charts/' + Args['chart'] + '/eric-product-info.yaml'
                    println('Update Docker repository in eric-product-info.yaml file...')
                    overwriteYaml(file: filePath,
                                  key: 'images.commonWfs.repoPath',
                                  value: 'proj-am/' + env.DOCKER_REPO,
                                  this)
                    overwriteYaml(file: filePath,
                                  key: 'images.pgInitContainer.repoPath',
                                  value: 'proj-am/' + env.DOCKER_REPO,
                                  this)

                    packageArgs += " --replace eric-product-info.yaml:VERSION=${env.IMAGE_VERSION}"

                    if(env.UPDATE_SOURCE == 'eric-eo-vnfm-helm-executor') {
                        println('Update eric-eo-vnfm-helm-executor in eric-product-info.yaml file...')
                        filePath = 'charts/' + Args['chart'] + '/eric-product-info.yaml'
                        overwriteYaml(file: filePath,
                                      key: 'images.helmExecutor.repoPath',
                                      value: env.HELM_EXECUTOR_REPO,
                                      this)
                        overwriteYaml(file: filePath,
                                      key: 'images.helmExecutor.tag',
                                      value: env.HELM_EXECUTOR_VERSION,
                                      this)
                    }
                break
                case 'am-common-wfs-ui':
                    packageArgs = ''

                    filePath = 'charts/' + Args['chart'] + '/eric-product-info.yaml'
                    println('Update Docker image in eric-product-info.yaml file...')
                    overwriteYaml(file: filePath,
                                  key: 'images.wfsUIService.repoPath',
                                  value: 'proj-am/' + env.DOCKER_REPO,
                                  this)
                    overwriteYaml(file: filePath,
                                  key: 'images.wfsUIService.tag',
                                  value: env.IMAGE_VERSION,
                                  this)
                break
                case 'eric-eo-evnfm-crypto':
                case 'eric-eo-evnfm-sol-agent':
                    packageArgs = """ --replace VERSION=${env.IMAGE_VERSION} \\
                                    | --replace RSTATE=false \\
                                    | --replace REPO_PATH='proj-am/${env.DOCKER_REPO}' \\
                                    | --replace eric-product-info.yaml:VERSION=${env.IMAGE_VERSION} \\
                                    | --replace eric-product-info.yaml:REPO_PATH='proj-am/${env.DOCKER_REPO}'""".stripMargin()
                break
                case 'am-integration-charts':
                    /*- Use VARs:
                          HELM_MIGRATION_TOOL_DIR
                      - Jenkins variables:
                          GERRIT_HTTP_URL
                    */
                    String chartPath = getChartPath(project: Args['project'],
                                                    url: GERRIT_HTTP_URL,
                                                    branch: 'master',
                                                    this)

                    println('INFO: Update version into Helm chart...')
                    filePath = chartPath + '/Chart.yaml'
                    comm = """yq -i \\
                              | --yaml-output \\
                              | --indentless \\
                              | '.version = "${Args['version']}"' \\
                              | ${filePath}
                              |cat ${filePath}""".stripMargin()
                    sh(comm)

                    println('INFO: Check that Helm Migration tool is present...')
                    filePath = chartPath + '/eric-product-info.yaml'
                    tempFile = readYaml(file: filePath)

                    if(tempFile['images'] && tempFile['images']['helmMigration']) {
                        println('INFO: Update Helm Migration tool image path...')
                        comm = """yq -i \\
                                  | --yaml-output \\
                                  | --indentless \\
                                  | '.images.helmMigration.repoPath = "proj-am/${env.DOCKER_REPO}"' \\
                                  | ${filePath}
                                  |cat ${filePath}""".stripMargin()
                        sh(comm)

                        println('INFO: Update Helm Migration tool image version...')
                        comm = """yq -i \\
                                  | --yaml-output \\
                                  | --indentless \\
                                  | '.images.helmMigration.tag = "${env.IMAGE_VERSION}"' \\
                                  | ${filePath}
                                  |cat ${filePath}""".stripMargin()
                        sh(comm)
                    } else {
                        println('INFO: Helm Migration tool is absent')
                    }
                break
                case 'eric-oss-function-orchestration-common':
                    /*- Use Jenkins variables:
                        GERRIT_HTTP_URL
                    */
                    String chartPath = getChartPath(project: Args['project'],
                                                    url: GERRIT_HTTP_URL,
                                                    branch: 'master',
                                                    this)

                    println('INFO: Update version into Helm chart...')
                    filePath = chartPath + '/Chart.yaml'
                    comm = """yq -i \\
                              | --yaml-output \\
                              | --indentless \\
                              | '.version = "${Args['version']}"' \\
                              | ${filePath}
                              |cat ${filePath}""".stripMargin()
                    sh(comm)
                break
                case 'am-onboarding-service':
                    packageArgs = """ --replace VERSION=${env.IMAGE_VERSION} \\
                                    | --replace SLES_PG10_IMAGE=${env.IMAGE_VERSION} \\
                                    | --replace eric-product-info.yaml:VERSION=${env.IMAGE_VERSION}""".stripMargin()

                    filePath = 'charts/' + Args['chart'] + '/eric-product-info.yaml'
                    println('Update Docker repository in eric-product-info.yaml file...')
                    overwriteYaml(file: filePath,
                                  key: 'images.onboardingService.repoPath',
                                  value: 'proj-am/' + env.DOCKER_REPO,
                                  this)
                    overwriteYaml(file: filePath,
                                  key: 'images.pgInitContainer.repoPath',
                                  value: 'proj-am/' + env.DOCKER_REPO,
                                  this)
                break
                case 'eric-eo-batch-manager':
                    packageArgs = """ --replace VERSION=${env.IMAGE_VERSION} \\
                                    | --replace SLES_PG10_IMAGE=${env.IMAGE_VERSION} \\
                                    | --replace eric-product-info.yaml:VERSION=${env.IMAGE_VERSION}""".stripMargin()

                    filePath = 'charts/' + Args['chart'] + '/eric-product-info.yaml'
                    println('Update Docker repository in eric-product-info.yaml file...')
                    overwriteYaml(file: filePath,
                                  key: 'images.batchManager.repoPath',
                                  value: 'proj-am/' + env.DOCKER_REPO,
                                  this)
                    overwriteYaml(file: filePath,
                                  key: 'images.pgInitContainer.repoPath',
                                  value: 'proj-am/' + env.DOCKER_REPO,
                                  this)
                break
                case 'evnfm-rbac':
                case 'eric-eo-evnfm-library-chart':
                    packageArgs = " --replace VERSION=${env.IMAGE_VERSION}"
                break
                case 'eric-eo-fh-event-to-alarm-adapter':
                    packageArgs = """ --replace VERSION=${env.IMAGE_VERSION} \\
                                    | --replace eric-product-info.yaml:VERSION=${env.IMAGE_VERSION}""".stripMargin()

                    filePath = 'charts/' + Args['chart'] + '/eric-product-info.yaml'
                    println('Update Docker repository in eric-product-info.yaml file...')
                    overwriteYaml(file: filePath,
                                  key: 'images.eventToAlarmAdapter.repoPath',
                                  value: 'proj-am/' + env.DOCKER_REPO,
                                  this)
                break
                case 'eric-eo-lm-consumer':
                    packageArgs = """ --replace VERSION=${env.IMAGE_VERSION} \\
                                    | --replace SLES_PG10_IMAGE=${env.IMAGE_VERSION} \\
                                    | --replace eric-product-info.yaml:VERSION=${env.IMAGE_VERSION}""".stripMargin()

                    filePath = 'charts/' + Args['chart'] + '/eric-product-info.yaml'
                    println('Update Docker repository in eric-product-info.yaml file...')
                    overwriteYaml(file: filePath,
                                  key: 'images.licenseConsumer.repoPath',
                                  value: 'proj-am/' + env.DOCKER_REPO,
                                  this)
                    overwriteYaml(file: filePath,
                                  key: 'images.pgInitContainer.repoPath',
                                  value: 'proj-am/' + env.DOCKER_REPO,
                                  this)
                break
                case 'vnfm-orchestrator':
                    packageArgs = """ --replace VERSION=${env.IMAGE_VERSION} \\
                                    | --replace SLES_PG10_IMAGE=${env.IMAGE_VERSION} \\
                                    | --replace eric-product-info.yaml:VERSION=${env.IMAGE_VERSION}""".stripMargin()

                    filePath = 'charts/' + Args['chart'] + '/eric-product-info.yaml'
                    println('Update Docker repository in eric-product-info.yaml file...')
                    overwriteYaml(file: filePath,
                                  key: 'images.orchestratorService.repoPath',
                                  value: 'proj-am/' + env.DOCKER_REPO,
                                  this)
                    overwriteYaml(file: filePath,
                                  key: 'images.pgInitContainer.repoPath',
                                  value: 'proj-am/' + env.DOCKER_REPO,
                                  this)
                break
                case 'gr-controller':
                    // Old chart compatibility
                    if(fileExists('chart')) {
                        println('INFO: Rewrite chart path...')
                        Args['folder'] = 'chart'
                    }

                    packageArgs = """ --replace eric-product-info.yaml:IMAGE_TAG=${env.IMAGE_VERSION} \\
                                    | --replace eric-product-info.yaml:IMAGE_REPO=proj-am/${env.DOCKER_REPO}""".stripMargin()
                break
                default:
                    packageArgs = """ --replace VERSION=${env.IMAGE_VERSION} \\
                                    | --replace SLES_PG10_IMAGE=${env.IMAGE_VERSION}""".stripMargin()

                    filePath = 'charts/' + Args['chart'] + '/values.yaml'
                    if(fileExists(filePath)) {
                        println('Update Docker repository in values.yaml file...')
                        tempFile = readFile(file: filePath).trim()
                        tempFile = tempFile.replace('proj-am/snapshots', 'proj-am/' + env.DOCKER_REPO)
                        writeFile(text: tempFile, file: filePath)
                    }

                    filePath = 'charts/' + Args['chart'] + '/eric-product-info.yaml'
                    if(fileExists(filePath)) {
                        println('Update Docker repository in eric-product-info.yaml file...')
                        tempFile = readFile(file: filePath).trim()
                        tempFile = tempFile.replace('proj-am/snapshots', 'proj-am/' + env.DOCKER_REPO)
                        writeFile(text: tempFile, file: filePath)

                        packageArgs = packageArgs + """ --replace eric-product-info.yaml:VERSION=${env.IMAGE_VERSION} \\
                                                      | --replace eric-product-info.yaml:SLES_PG10_IMAGE=${env.IMAGE_VERSION}""".stripMargin()
                    }
                break
            }

            println('INFO: Pull Helm package image...')
            pullDockerImage(name: dockerImage.split(':')[0],
                            tag: dockerImage.split(':')[1],
                            this)

            for(String chart in Args['chart'].split(',')) {
                chart = chart.replaceAll(' ', '')

                println('INFO: Packaging Helm Chart ' + chart + '...')
                withCredentials([usernamePassword(credentialsId: getCredential('ldap'),
                                                  usernameVariable: 'HELM_USER',
                                                  passwordVariable: 'HELM_TOKEN')]) {
                    comm = """docker run --init --rm \\
                              | --volume ${WORKSPACE}/${Args['project']}:/app:rw \\
                              | --workdir /app \\
                              | --user \$(id -u):\$(id -g) \\
                              | ${dockerImage} \\
                              | helm-package \\
                              | --folder ${Args['folder']}/${chart} \\
                              | --output ${env.GLOBAL_HELM_FOLDER}/${chart} \\
                              | --helm-user \$HELM_USER \\
                              | --arm-api-token \$HELM_TOKEN \\
                              | --version ${Args['version']} \\
                              | ${packageArgs}""".stripMargin()
                    sh(comm)
                }

                if(Args['upload']) {
                    println('INFO: Pull Helm upload image...')
                    pullDockerImage(name: dockerImageUpload.split(':')[0],
                                    tag: dockerImageUpload.split(':')[1],
                                    this)

                    println('INFO: Upload Helm chart ' + chart + '...')
                    withCredentials([usernamePassword(credentialsId: getCredential(env.HELM_URL),
                                                      usernameVariable: 'HELM_USER',
                                                      passwordVariable: 'HELM_TOKEN')]) {
                        comm = """docker run --init --rm \\
                                | --volume ${WORKSPACE}/${Args['project']}:/app:rw \\
                                | --workdir /app \\
                                | --user \$(id -u):\$(id -g) \\
                                | ${dockerImageUpload} \\
                                | arm-upload \\
                                | --file ${env.GLOBAL_HELM_FOLDER}/${chart}/${chart}-${Args['version']}.tgz \\
                                | --destination ${env.HELM_URL}/${chart} \\
                                | --token \$HELM_TOKEN""".stripMargin()
                        sh(comm)
                    }
                }
            }

            println('INFO: Set Short Text in build history...')
            currentBuild.description = currentBuild.description ? currentBuild.description : ''
            currentBuild.description += "<p>${Args['chart']}: ${Args['version']}</p>"
            currentBuild.description += "<p>${env.GIT_COMMIT_AUTHOR}</p>"
        }
    }
}


/* Stage for Check Design Rules. Use:
- VARs:
    NEXUS_URL
    NEXUS_DESIGN_REPORT_PATH
- Job's ENVs:
    CHART_NAME
    GLOBAL_HELM_FOLDER
    DESIGN_RULES_FILE
- Args:
    project(require): type String; Name of the project
    upload: type Boolean; If true to upload the result to Nexus server; default is false
*/
def DesignRuleCheck(Map Args) {
    String stageName = 'DesignRuleCheck'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    upload: [value: Args['upload'], type: 'bool', require: false]]


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['upload'] = Args.containsKey('upload') ? Args['upload'] : false


    stage('Design Rule Check') {
        dir(Args['project']) {
            String comm
            int status = 0
            String dockerImage = getDockerImagePath('helm-dr-check')
            String chartPath
            String designRules = '-DhelmDesignRule.feature.dependency=0'
            String rulesParams = ''
            String valuesParams = ''


            println('Remove Old Docker image...')
            comm = 'docker rmi -f ' + dockerImage
            sh(comm)

            println('Check design rule file...')
            if(env.DESIGN_RULES_FILE && fileExists(env.DESIGN_RULES_FILE)) {
                readFile(env.DESIGN_RULES_FILE).split('\n').each {
                    if(it.size() > 2 && it[0..1] == '-D') {
                        rulesParams += ' ' + it
                    }
                }
            }

            for(String chart in env.CHART_NAME.split(',')) {
                Integer drStatus
                chart = chart.replaceAll(' ', '')
                chartPath = env.GLOBAL_HELM_FOLDER + '/' + chart + '/*.tgz'

                println('Check chart values file for ' + chart +'...')
                filePath = 'charts/' + chart + '.yaml'
                if(fileExists(filePath)) {
                    valuesParams = '--values-file ' + filePath
                }

                println('Run Design Rule Check for ' + chart + '...')
                comm = """docker run --init --rm \\
                          | --volume ${WORKSPACE}/${Args['project']}:/app:rw \\
                          | --workdir /app \\
                          | --user \$(id -u):\$(id -g) \\
                          | ${dockerImage} \\
                          | helm-dr-check \\
                          | ${valuesParams} \\
                          | ${designRules} \\
                          | ${rulesParams} \\
                          | --helm-chart ${chartPath} \\
                          | -output ./""".stripMargin()
                drStatus = sh(script: comm, returnStatus: true).toInteger()

                println('Run post-test steps for ' + chart + '...')
                comm = """mv --verbose \\
                          | design-rule-check-report.html \\
                          | ${chart}-design-rule-report.html""".stripMargin()
                sh(comm)
                status = status != 0 ? status : drStatus
            }

            println('Archive reports...')
            archiveArtifacts(artifacts: '*-design-rule-report.html')

            println('Publish HTML report...')
            publishHTML(target: [ includes: '*-design-rule-report.html',
                                  reportDir: './',
                                  reportFiles: '*-design-rule-report.html',
                                  reportName: 'Design Rule Check'])

            if(Args['upload']) {
                println('Upload Design rule check report(s) to Nexus...')
                for(String chart in env.CHART_NAME.split(',')) {
                    chart = chart.replaceAll(' ', '')

                    uploadToNexus(name: chart,
                                  dir: '.',
                                  url: env.NEXUS_URL,
                                  path: env.NEXUS_DESIGN_REPORT_PATH,
                                  type: 'html',
                                  artifact: chart + '-design-rule-report.html',
                                  this)
                }
            }

            if(status != 0 && !Args['upload']) {
                sh('exit 1')
            }
        }
    }
}


/* Stage for Package Dependency into Helm Chart. Use:
- VARs:
    DEPENDENCY_CHART_LOCATION
- Job's ENVs:
    CHART_NAME
    CHART_VERSION
    GLOBAL_HELM_FOLDER
- Args:
    project(require): type String; project of the project
    chart: type String; Name of the Helm Chart; default is env.CHART_NAME
    skip: type Boolean; If true to skip the current stage; default is false
    stage: type String; Name of the stage; default is 'Package Dependency Helm Chart'
*/
def PackageDependency(Map Args) {
    String stageName = 'PackageDependency'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    chart: [value: Args['chart'], type: 'string', require: false],
                    skip: [value: Args['skip'], type: 'bool', require: false],
                    stage: [value: Args['stage'], type: 'string', require: false]]
    String comm
    String chartName
    String dockerImage = getDockerImagePath('bob-adp')
    String filePath
    String tempFile


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['chart'] = Args.containsKey('chart') ? Args['chart'] : env.CHART_NAME
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false
    Args['stage'] = Args.containsKey('stage') ? Args['stage'] : 'Package Dependency Helm Chart'


    stage(Args['stage']) {
        if(Args['skip']) {
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            dir(Args['project']) {
                chartName = Args['chart'] + '-dependency'

                switch(Args['project']) {
                    case 'am-common-wfs-ui':
                        filePath = env.DEPENDENCY_CHART_LOCATION + '/values.yaml'
                        if(fileExists(filePath)) {
                            println('Update BackEnd Docker images in values.yaml file...')
                            tempFile = readFile(file: filePath).trim()
                            tempFile = tempFile.replaceAll('repoPath:.*proj-am/.*', "repoPath: \"proj-am/${env.DOCKER_REPO}\"")
                            writeFile(text: tempFile, file: filePath)
                        }
                    break
                }

                println('Packaging Dependency to Helm chart...')
                comm = """docker run --init --rm \\
                          | --volume ${WORKSPACE}/${Args['project']}:/app:rw \\
                          | --workdir /app \\
                          | --user \$(id -u):\$(id -g) \\
                          | ${dockerImage} \\
                          | helm-package \\
                          | --folder ${env.DEPENDENCY_CHART_LOCATION} \\
                          | --output ${env.GLOBAL_HELM_FOLDER}/${chartName} \\
                          | --version ${env.CHART_VERSION}""".stripMargin()
                sh(comm)
            }
        }
    }
}


/* Stage for Update Helm Chart. Use:
- Job's ENVs:
    PROJECT_NAME
- Args:
    name(require): type String; Name of the project
    skip: type Boolean; If true to skip the current stage; default is false
*/
def UpdateHelmChart(Map Args) {
    String stageName = 'UpdateHelmChart'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    skip: [value: Args['skip'], type: 'bool', require: false]]
    String comm


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false


    stage('Update Helm Chart') {
        if(Args['skip']) {
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            dir(Args['name']) {
                println('Update Helm repository...')
                dir('am-integration-charts/charts/eric-eo-evnfm') {
                    comm = '''  helm3 repo add eric-eo-evnfm-helm \\
                              |https://arm.seli.gic.ericsson.se/artifactory/proj-eo-evnfm-helm/
                              | helm3 repo add eric-data-document-database-pg \\
                              |https://arm.sero.gic.ericsson.se/artifactory/proj-adp-gs-all-helm/
                              | helm3 repo update'''.stripMargin()
                    sh(comm)
                }

                println('Update Helm repository dependency...')
                dir('am-integration-charts/charts') {
                    comm = """  ls -lsh
                              | helm3 dep update eric-eo-evnfm
                              | rm -f eric-eo-evnfm/charts/${env.PROJECT_NAME}-[0-9].*.tgz
                              | cp ${WORKSPACE}/${Args['name']}/helm-target/${env.PROJECT_NAME}/*.tgz eric-eo-evnfm/charts/
                              | ls -ls eric-eo-evnfm/charts/
                              | helm3 package eric-eo-evnfm""".stripMargin()
                    sh(comm)
                }

                println('Update Helm root dependency...')
                comm = "helm3 dependency update charts/${env.PROJECT_NAME}"
                sh(comm)
            }
        }
    }
}


/* Stage for Validate Helm Chart Schema. Use:
- Job's ENVs:
    PROJECT_NAME
*/
def ValidateChartSchema(String project) {
    stage('Validate Helm Chart Schema') {
        dir(project) {
            String comm
            String dockerImage = getDockerImagePath('schema-validator')


            println('Run Validate...')
            comm = """docker run --init --rm \\
                          | --volume ${WORKSPACE}/${project}:/app:rw \\
                          | --workdir /app \\
                          | --user \$(id -u):\$(id -g) \\
                          | ${dockerImage} \\
                          | compile \\
                          | -s charts/${env.PROJECT_NAME}/values.schema.json""".stripMargin()
            sh(comm)
        }
    }
}


/* Stage to Uplift Dependency into Helm Chart. Use:
- Args:
    name(require): type String; Name of the dependency
    version(require): type String; Version of the dependency
    project(require): type String; Name of the updating project
    file(require): type String; Path of the updating file
    repository: type String; Repository of the dependency; default is ''
    skip: type Boolean; If true to skip the current stage; default is false
*/
def UpliftDependency(Map Args) {
    String stageName = 'UpliftDependency'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    version: [value: Args['version'], type: 'string'],
                    project: [value: Args['project'], type: 'string'],
                    file: [value: Args['file'], type: 'string'],
                    repository: [value: Args['repository'], type: 'string', require: false],
                    skip: [value: Args['skip'], type: 'bool', require: false]]
    String key


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['repository'] = Args.containsKey('repository') ? Args['repository'] : ''
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false


    stage('Uplift dependency into ' + Args['project']) {
        if(Args['skip']) {
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            dir(Args['project']) {
                switch(true) {
                    case Args['name'] == 'eric-eo-vnfm-helm-executor':
                        println('INFO: Update ' + Args['name'] + ' in ' + Args['file'] + ' file...')
                        if(Args['repository']) {
                            key = 'images.helmExecutor.repoPath'
                            overwriteYaml(file: Args['file'],
                                          key: key,
                                          value: Args['repository'],
                                          this)
                        }

                        key = 'images.helmExecutor.tag'
                        overwriteYaml(file: Args['file'],
                                      key: key,
                                      value: Args['version'],
                                      this)
                    break
                    case Args['name'] == 'eric-am-onboarding' && Args['project'] == 'am-common-wfs-ui':
                    case Args['name'] == 'eric-vnfm-orchestrator' && Args['project'] == 'am-common-wfs-ui':
                        key = Args['name'] + '.version'

                        println('INFO: Update property in pom file...')
                        comm = """mvn versions:set-property -q \\
                                  | --file ${Args['file']} \\
                                  | -Dproperty=${key} \\
                                  | -DnewVersion=${Args['version']}""".stripMargin()
                        sh(comm)
                    break
                    default:
                        Integer pos = 0
                        for(def chart in readYaml(file: Args['file'])['dependencies']) {
                            if(chart.name == Args['name']) {
                                break
                            }
                            pos++
                        }

                        key = 'dependencies[' + pos + '].version'
                        println('INFO: Update version to ' + Args['version'] + ' ...')
                        overwriteYaml(file: Args['file'],
                                      key: key,
                                      value: Args['version'],
                                      eof: true,
                                      this)
                    break
                }
            }
        }
    }
}


/* Stage for download Helm Chart. Use:
- Args:
    dir(require): type String; Download directory
    project: type String; Name of the downloading helm chart; default is 'eric-eo-helmfile'
    version: type String; Version of the helm chart; default is '2.20.0-177'
*/
def DownloadHelmChart(Map Args) {
    String stageName = 'Download Helm Chart'
    Map argsList = [dir: [value: Args['dir'], type: 'string'],
                    project: [value: Args['project'], type: 'string', require: false],
                    version: [value: Args['version'], type: 'string', require: false]]
    String comm
    String repository
    String credId
    String name


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['project'] = Args.containsKey('project') ? Args['project'] : 'eric-eo-helmfile'
    Args['version'] = Args.containsKey('version') ? Args['version'] : '2.20.0-177'


    stage('Download Helm Chart') {
        dir(Args['dir']) {
            repository = getHelmRepository(Args['project'])
            name = Args['project'] + '-' + Args['version']

            println('Download Helm Chart...')
            credId = getCredential('ldap')
            withCredentials([usernamePassword(credentialsId: credId,
                                              usernameVariable: 'ARTIFACTORY_USER',
                                              passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
                comm = """curl --fail -u '${ARTIFACTORY_USER}':'${ARTIFACTORY_PASSWORD}' \\
                           | ${repository}/${Args['project']}/${name}.tgz  \\
                           | -o ${name}.tgz """.stripMargin()
                sh(comm)
            }
        }
    }
}


/* Stage for Check Design Rules CNF. Use:
- Job's ENVs:
    PROJECT_NAME
    GLOBAL_HELM_FOLDER
    DESIGN_RULES_FILE
- Args:
    project(require): type String; Name of the project
*/
def DesignRuleCheckCNF(Map Args) {
    String stageName = 'DesignRuleCheckCNF'
    Map argsList = [project: [value: Args['project'], type: 'string']]
    String comm

    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Design Rule Check CNF') {
        dir(Args['project']) {

            if (env.VNFD_DIR == '') {
                println('Skip design rule check for all packages...')
            } else {
                int status = 0
                String dockerImage = getDockerImagePath('helm-dr-check')
                String chartPath = ''
                String designRules = '-DhelmDesignRule.feature.dependency=0'
                String rulesParams = ''
                String valuesParams = ''
                def chartName

                comm = "find ${env.VNFD_DIR} -name '*.tgz' -type f -exec basename {} \\; "
                chartName = sh(script: comm, returnStdout: true).trim()
                chartName = chartName.split('\n')

                println('Remove Old Docker image...')
                comm = 'docker rmi -f ' + dockerImage
                sh(comm)

                println('Check design rule file...')
                if (env.DESIGN_RULES_FILE && fileExists(env.DESIGN_RULES_FILE)) {
                    rulesParams = readFile(env.DESIGN_RULES_FILE)
                        .trim()
                        .split('\n')
                        .join(' ')
                }


                if (chartName.size() == 0) {
                    println("Can't find CNF packages in the directory ${env.VNFD_DIR} ")
                    sh "exit 1"
                }

                println("Create report directory...")
                sh "if [ ! -d reports ]; then  mkdir reports; fi"

                println("Loop charts...")
                chartName.each { item ->

                    println('Check chart values file...')
                    filePath = 'charts/' + item + '.yaml'
                    if (fileExists(filePath)) {
                        valuesParams = '--values-file ' + filePath
                    }

                    chartPath = env.VNFD_DIR + item

                    println("Get chart ${chartPath}...")

                    println('Run Design Rule Check...')
                    comm = """docker run --init --rm \\
                             | --volume ${WORKSPACE}/${Args['project']}:/app:rw \\
                             | --workdir /app \\
                             | --user \$(id -u):\$(id -g) \\
                             | ${dockerImage} \\
                             | helm-dr-check \\
                             | ${valuesParams} \\
                             | ${designRules} \\
                             | ${rulesParams} \\
                             | --helm-chart ${chartPath} \\
                             | -output ./""".stripMargin()
                    sh(script: comm, returnStatus: true).toInteger()

                    println('Move design-rule-check-report.html to report dir...')
                    if (fileExists('design-rule-check-report.html')) {
                        comm = """mv design-rule-check-report.html \\
                                 |reports/${item}-design-rule-check-report.html""".stripMargin()
                        sh(comm)
                    }

                    println('Publish HTML report...')
                    publishHTML(target: [includes   : '*design-rule-check-report*',
                                         reportDir  : 'reports',
                                         reportFiles: "reports/${item}-design-rule-check-report.html",
                                         reportName : "Design Rule Check ${item}"]
                    )
                }

                println('Archive reports...')
                archiveArtifacts(artifacts: 'reports/*.html')
            }
        }
    }
}


/* Stage is for Upload CNF Charts. Use:
- Args:
    dir(require): type String; Build directory
*/
def UploadAllChartsCNF(Map Args) {
    String stageName = 'UploadChartsCNF'
    Map argsList = [dir: [value: Args['dir'], type: 'string']]
    String comm
    String charts
    String versions
    String basePath = 'charts'


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Upload All Charts') {
        dir(Args['dir']){
            println('Get the list of charts...')
            comm = "find ${basePath} -maxdepth 1 -mindepth 1 -type d"
            charts = sh(script: comm, returnStdout: true).trim()

            withCredentials([usernamePassword(credentialsId: getCredential('ldap'),
                             usernameVariable: 'NEXUS_USER',
                             passwordVariable: 'NEXUS_PASSWORD')]) {
                charts.tokenize().each { chart ->
                    println("Get the list of ${chart} versions...")
                    comm = "find ${chart} -maxdepth 1 -mindepth 1 -type d"
                    versions = sh(script: comm, returnStdout: true).trim()

                    versions.tokenize().each { version ->
                        version = version.split('/')[-1]
                        chart = chart.split('/')[-1]

                        println("Upload chart ${chart}:${version}")
                        comm = """scripts/upload_chart.py \\
                                 |upload --chart-name=${chart} \\
                                 |--chart-version=${version} \\
                                 |--login=\$NEXUS_USER \\
                                 |--password=\$NEXUS_PASSWORD""".stripMargin()
                        sh(script: comm, returnStdout: true)
                    }
                }
            }
        }
    }
}

return this