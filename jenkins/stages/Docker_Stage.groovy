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
import static com.ericsson.orchestration.mgmt.libs.VnfmDocker.buildBaseImage
import static com.ericsson.orchestration.mgmt.libs.VnfmDocker.buildImage
import static com.ericsson.orchestration.mgmt.libs.VnfmDocker.getDockerImagePath
import static com.ericsson.orchestration.mgmt.libs.VnfmDocker.getImageName
import static com.ericsson.orchestration.mgmt.libs.VnfmDocker.tagImage
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkArgs
import static com.ericsson.orchestration.mgmt.libs.VnfmGit.getChartPath


/* Stage is for Build Docker Image. Use:
- VARs:
    DOCKER_DIR
    POSTGRES_VERSION
    EGRESS_MODIFIER_IMAGE_NAME(for gr-controller)
    EGRESS_MODIFIER_DOCKER_DIR(for gr-controller)
    GR_CONTROLLER_IMAGE_NAME(for gr-controller)
    GR_CONTROLLER_DOCKER_DIR(for gr-controller)
    STUB_IMAGE_NAME(for gr-controller)
    STUB_DOCKER_DIR(for gr-controller)
- Job's ENVs:
    IMAGE_REPO
    IMAGE_NAME
    IMAGE_VERSION
    IMAGE_DEV_BACKEND_NAME
    CBO_VERSION
    PSQL_BASE_IMAGE
    PROJECT_NAME
    PROJECT_VERSION
    GIT_HASH
    KUBECTL_BASE_IMAGE(for eric-eo-evnfm-crypto)
    KUBECTL_VERSION(for eric-eo-evnfm-crypto)
- Args:
    name(require): type String; Name of the image
    stage: type String; Stage custom name; default is 'Build Image'
*/
def BuildImage(Map Args) {
    String stageName = 'BuildImage'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    stage: [value: Args['stage'], type: 'string', require: false]]
    String filePath = env.DOCKER_DIR + '/Dockerfile'
    String fullName = env.IMAGE_REPO + '/' + env.IMAGE_NAME
    String baseImage
    String buildArgs
    String comm
    String buildDir
    String ldapUser
    String ldapPassword


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['stage'] = Args.containsKey('stage') ? Args['stage'] : 'Build Image'


    stage(Args['stage']) {
        dir(Args['name']) {
            // Prepare Base images
            switch(Args['name']) {
                case 'eric-eo-batch-manager':
                case 'am-common-wfs':
                case 'am-onboarding-service':
                case 'eric-eo-lm-consumer':
                case 'vnfm-orchestrator':
                    baseImage = getImageName(file: filePath, this)[0]
                    println('Base image name: ' + baseImage)

                    println('Prepare base image...')
                    buildBaseImage( name: baseImage,
                                    tag: env.IMAGE_VERSION,
                                    cbo_tag: env.CBO_VERSION,
                                    this)

                    println('Build PSQL base image...')
                    buildBaseImage( name: env.PSQL_BASE_IMAGE,
                                    tag: env.IMAGE_VERSION,
                                    cbo_tag: env.CBO_VERSION,
                                    postgres_version: env.POSTGRES_VERSION,
                                    this)
                break
                case 'eric-eo-evnfm-crypto':
                    baseImage = getImageName(file: filePath, this)[0]
                    println('Base image name: ' + baseImage)

                    println('Prepare base image...')
                    buildBaseImage( name: baseImage,
                                    tag: env.IMAGE_VERSION,
                                    cbo_tag: env.CBO_VERSION,
                                    this)

                    println('Build Kubectl base image...')
                    buildBaseImage( name: env.KUBECTL_BASE_IMAGE,
                                    tag: env.IMAGE_VERSION,
                                    cbo_tag: env.CBO_VERSION,
                                    kubectlVersion: env.KUBECTL_VERSION,
                                    this)

                    println('Build Crypto Migration image...')
                    buildArgs = """ --file eric-eo-evnfm-crypto-migration/Dockerfile \\
                                  | --build-arg REPOSITORY=${env.PROJECT_NAME} \\
                                  | --build-arg APP_VERSION=${env.PROJECT_VERSION} \\
                                  | --build-arg BASE_IMAGE_VERSION=${env.IMAGE_VERSION} \\
                                  | --build-arg COMMIT=${env.GIT_HASH} \\
                                  | --build-arg PRODUCT_REVISION=false \\
                                  | --build-arg CURRENT_DATE=\$(date +%d-%b-%Y)""".stripMargin()
                    buildImage( name: fullName + '-migration',
                                tag: env.IMAGE_VERSION,
                                args: buildArgs,
                                dir: 'eric-eo-evnfm-crypto-migration',
                                this)
                break
                case 'gr-controller':
                    if(!fileExists('BASE_OS_VERSION')) {
                        filePath = env.GR_CONTROLLER_DOCKER_DIR + '/Dockerfile'
                        baseImage = getImageName(file: filePath, this)[0]
                        println('INFO: Base image name is ' + baseImage)

                        println('INFO: Build GR-Controller Base Docker image...')
                        buildBaseImage( name: baseImage,
                                        tag: env.CBO_VERSION,
                                        cbo_tag: env.CBO_VERSION,
                                        this)
                    }
                break
                case 'vnfsdk-pkgtools':
                case 'eric-eo-evnfm-mb':
                case 'cvnfm-enm-cli-stub':
                case 'eric-eo-vnfm-helm-executor':
                break
                case 'am-integration-charts':
                    /*- Use VARs:
                          HELM_MIGRATION_TOOL_DIR
                    */
                    if(fileExists(env.HELM_MIGRATION_TOOL_DIR)) {
                        dir(env.HELM_MIGRATION_TOOL_DIR) {
                            baseImage = getImageName(file: filePath, this)[0]
                            println('INFO: Base image name for Helm Migration tool is ' + baseImage)

                            println('INFO: Prepare Helm Migration Tool base image...')
                            buildBaseImage( name: baseImage,
                                            tag: env.IMAGE_VERSION,
                                            cbo_tag: env.CBO_VERSION,
                                            this)
                        }
                    }
                break
                default:
                    baseImage = getImageName(file: filePath, this)[0]
                    println('Base image name: ' + baseImage)

                    println('Prepare base image...')
                    buildBaseImage( name: baseImage,
                                    tag: env.IMAGE_VERSION,
                                    cbo_tag: env.CBO_VERSION,
                                    this)
                break
            }

            if(fileExists('Dockerfile-pg')) {
                println('INFO: Build Postgres Docker image...')
                buildArgs = """ --file Dockerfile-pg \\
                              | --build-arg BASE_IMAGE=${env.PSQL_BASE_IMAGE} \\
                              | --build-arg BASE_IMAGE_VERSION=${env.IMAGE_VERSION}""".stripMargin()
                buildImage( name: fullName + '-pg',
                            tag: env.IMAGE_VERSION,
                            args: buildArgs,
                            this)
            }

            switch(Args['name']) {
                case 'am-common-wfs-ui':
                    println('Build BackEnd Docker image...')
                    buildArgs = """ --file Dockerfile-dev-backend \\
                                  | --build-arg BASE_IMAGE_VERSION=${env.IMAGE_VERSION}""".stripMargin()
                    buildImage( name: env.IMAGE_REPO + '/' + env.IMAGE_DEV_BACKEND_NAME,
                                tag: env.IMAGE_VERSION,
                                args: buildArgs,
                                this)

                    buildArgs = """ --build-arg APP_VERSION=${env.PROJECT_VERSION} \\
                                  | --build-arg BASE_IMAGE_VERSION=${env.IMAGE_VERSION} \\
                                  | --build-arg GIT_COMMIT=${env.GIT_HASH} \\
                                  | --build-arg BUILD_TIME=\$(date +%Y-%m-%dT%T.%3NZ)""".stripMargin()

                    println('Build Docker image...')
                    buildImage( name: fullName,
                                tag: env.IMAGE_VERSION,
                                args: buildArgs,
                                dir: env.DOCKER_DIR,
                                this)
                break
                case 'eric-eo-evnfm-crypto':
                    buildArgs = """ --build-arg REPOSITORY=${env.PROJECT_NAME} \\
                                  | --build-arg APP_VERSION=${env.PROJECT_VERSION} \\
                                  | --build-arg BASE_IMAGE_VERSION=${env.IMAGE_VERSION} \\
                                  | --build-arg GIT_COMMIT=${env.GIT_HASH} \\
                                  | --build-arg BUILD_TIME=\$(date +%Y-%m-%dT%T.%3NZ)""".stripMargin()

                    println('Build Docker image...')
                    buildImage( name: fullName,
                                tag: env.IMAGE_VERSION,
                                args: buildArgs,
                                dir: env.DOCKER_DIR,
                                this)
                break
                case 'eric-eo-vnfm-helm-executor':
                    buildArgs = " --build-arg BASE_IMAGE_VERSION=${env.CBO_VERSION}"

                    println('Build Docker image...')
                    buildImage( name: fullName,
                                tag: env.IMAGE_VERSION,
                                args: buildArgs,
                                dir: env.DOCKER_DIR,
                                this)
                break
                case 'gr-controller':
                    String baseVersion = env.CBO_VERSION
                    String emDockerDir = env.EGRESS_MODIFIER_DOCKER_DIR
                    String grDockerDir = env.GR_CONTROLLER_DOCKER_DIR
                    String stubDockerDir = env.STUB_DOCKER_DIR

                    // Check BASE_OS_VERSION for old images
                    if(fileExists('BASE_OS_VERSION')) {
                        baseVersion = readFile('BASE_OS_VERSION').trim()
                    }

                    // Egress Modifier
                    println('INFO: Check Egress Modifier Docker path...')
                    if(fileExists('egress-modifier-hooks/egress-modifier/Dockerfile')) {
                        emDockerDir = 'egress-modifier-hooks/egress-modifier'
                    }

                    println('INFO: Build Egress Modifier Docker image...')
                    env.IMAGE_NAME = env.EGRESS_MODIFIER_IMAGE_NAME
                    buildArgs = "--build-arg BASE_OS_VERSION=${baseVersion}"
                    fullName = env.IMAGE_REPO + '/' + env.EGRESS_MODIFIER_IMAGE_NAME
                    buildImage( name: fullName,
                                tag: env.IMAGE_VERSION,
                                args: buildArgs,
                                dir: emDockerDir,
                                this)

                    // GR-Controller
                    println('INFO: Check GR-Controller Docker path...')
                    if(fileExists('Dockerfile')) {
                        grDockerDir = '.'
                    }

                    println('INFO: Copy artifact to docker directory...')
                    comm = """cp -v \\
                              |target/gr-controller-${env.PROJECT_VERSION}.jar \\
                              |${grDockerDir}/gr-controller.jar""".stripMargin()
                    sh(comm)

                    println('INFO: Build GR-Controller Docker image...')
                    env.IMAGE_NAME += ', ' + env.GR_CONTROLLER_IMAGE_NAME
                    buildArgs = """ --build-arg BASE_OS_VERSION=${baseVersion} \\
                                  | --build-arg VERSION=${env.PROJECT_VERSION}""".stripMargin()
                    fullName = env.IMAGE_REPO + '/' + env.GR_CONTROLLER_IMAGE_NAME
                    buildImage( name: fullName,
                                tag: env.IMAGE_VERSION,
                                args: buildArgs,
                                dir: grDockerDir,
                                this)

                    // Stub
                    println('INFO: Check Stub Docker path...')
                    if(fileExists('stubs/Dockerfile')) {
                        stubDockerDir = 'stubs'
                    }

                    println('INFO: Build Stub Docker image...')
                    env.IMAGE_NAME += ', ' + env.STUB_IMAGE_NAME
                    buildArgs = """ --build-arg BASE_OS_VERSION=${baseVersion} \\
                                  | --build-arg STUB_VERSION=${env.STUB_VERSION} \\
                                  | --build-arg VERSION=${env.STUB_VERSION}""".stripMargin()
                    fullName = env.IMAGE_REPO + '/' + env.STUB_IMAGE_NAME
                    buildImage( name: fullName,
                                tag: env.IMAGE_VERSION,
                                args: buildArgs,
                                dir: stubDockerDir,
                                this)
                break
                case 'am-integration-charts':
                    /*- Use VARs:
                          HELM_MIGRATION_TOOL_DIR
                      - Jenkins variables:
                          GERRIT_HTTP_URL
                    */
                    if(fileExists(env.HELM_MIGRATION_TOOL_DIR)) {
                        String imageName
                        String chartPath = getChartPath(project: Args['name'],
                                                        url: GERRIT_HTTP_URL,
                                                        branch: 'master',
                                                        this)

                        println('INFO: Get image name for Helm Migration Tool...')
                        filePath = chartPath + '/eric-product-info.yaml'
                        imageName = readYaml(file: filePath)['images']['helmMigration']['name']

                        dir(env.HELM_MIGRATION_TOOL_DIR) {
                            println('INFO: Set build image parameters for Helm Migration Tool...')
                            fullName = env.IMAGE_REPO + '/' + env.REGISTRY_MIGRATION_IMAGE_NAME
                            buildArgs = "--build-arg BASE_IMAGE_VERSION=${env.IMAGE_VERSION}"

                            println('INFO: Build Helm Migration Tool Docker image...')
                            buildImage( name: fullName,
                                        tag: env.IMAGE_VERSION,
                                        args: buildArgs,
                                        dir: env.DOCKER_DIR,
                                        this)
                        }
                    } else {
                        println('INFO: Nothing to build')
                    }
                break
                default:
                    buildArgs = """ --build-arg APP_VERSION=${env.PROJECT_VERSION} \\
                                  | --build-arg BASE_IMAGE_VERSION=${env.IMAGE_VERSION} \\
                                  | --build-arg GIT_COMMIT=${env.GIT_HASH} \\
                                  | --build-arg BUILD_TIME=\$(date +%Y-%m-%dT%T.%3NZ)""".stripMargin()

                    println('Build Docker image...')
                    wrap([$class: 'MaskPasswordsBuildWrapper',
                          varPasswordPairs: [ [var: 'LDAP_USER', password: ldapUser],
                                              [var: 'LDAP_PASSWORD', password: ldapPassword]]]) {
                        buildImage( name: fullName,
                                    tag: env.IMAGE_VERSION,
                                    args: buildArgs,
                                    dir: env.DOCKER_DIR,
                                    this)
                    }
                break
            }
        }
    }
}


/* Stage is for additional Tag Docker Image. Use:
- Job's ENVs:
    IMAGE_REPO
    IMAGE_NAME
    IMAGE_VERSION
- Args:
    project(require): type String; Gerrit project name
    tag_new(require): type String; Additional Docker image tag, default is 'latest'
    stage: type String; Stage custom name; default is 'Tag Image'
*/
def TagImage(Map Args) {
    String stageName = 'TagImage'
    Map argsList = [project: [value: Args['project'], type: 'string'],
                    tag_new: [value: Args['tag_new'], type: 'string', require: false],
                    stage: [value: Args['stage'], type: 'string', require: false]]
    String fullName = env.IMAGE_REPO + '/' + env.IMAGE_NAME


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['tag_new'] = Args.containsKey('tag_new') ? Args['tag_new'] : 'latest'
    Args['stage'] = Args.containsKey('stage') ? Args['stage'] : 'Tag Image'


    stage(Args['stage']) {
        dir(Args['project']) {
            println('Tag Docker image ...')
            tagImage( name: fullName,
                      tag: env.IMAGE_VERSION,
                      tag_new: Args['tag_new'],
                      this)
        }
    }
}


/* Stage for Checking Docker Image Design Rules. Use:
- VARs:
    IMAGE_DESIGN_RULES_FILE
    EGRESS_MODIFIER_IMAGE_NAME(for gr-controller)
    GR_CONTROLLER_IMAGE_NAME(for gr-controller)
    STUB_IMAGE_NAME(for gr-controller)
- Job's ENVs:
    IMAGE_REPO
    IMAGE_NAME
- Args:
    name(require): type String; Name of the image
*/
def DesignRuleCheck(Map Args) {
    String stageName = 'DesignRuleCheck'
    Map argsList = [name: [value: Args['name'], type: 'string']]
    String dockerImage = getDockerImagePath('image-dr-check')
    String comm
    String testComm
    int status = 0
    String rulesParams = ''


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Image Design Rule Check') {
        dir(Args['name']) {
            println('Remove Old Docker image...')
            comm = 'docker rmi -f ' + dockerImage
            sh(comm)

            println('Check design rule file...')
            if(env.IMAGE_DESIGN_RULES_FILE && fileExists(env.IMAGE_DESIGN_RULES_FILE)) {
                rulesParams = readFile(env.IMAGE_DESIGN_RULES_FILE)
                                                                  .trim()
                                                                  .split('\n')
                                                                  .join(' ')
            }

            // Prepare base test command
            testComm = """docker run --init --rm \\
                          | \$(for x in \$(id -G); do printf " --group-add %s" "\$x"; done) \\
                          | --volume /var/run/docker.sock:/var/run/docker.sock \\
                          | --volume ${WORKSPACE}/${Args['name']}:/app:rw \\
                          | --workdir /app \\
                          | --user \$(id -u):\$(id -g) \\
                          | ${dockerImage} \\
                          | image-dr-check \\
                          | --image ${env.IMAGE_REPO}/_name_:${env.IMAGE_VERSION} \\
                          | ${rulesParams} \\
                          | --remote \\
                          | --private-build false \\
                          | -output ./""".stripMargin()

            switch(Args['name']) {
                case 'gr-controller':
                    Integer drStatus = 0

                    println('Test Egress Modifier Docker image...')
                    comm = testComm.replace('_name_', env.EGRESS_MODIFIER_IMAGE_NAME)
                    drStatus = sh(script: comm, returnStatus: true).toInteger()

                    println('Run post-test steps...')
                    comm = """mv --verbose \\
                              | image-design-rule-check-report.html \\
                              | ${env.EGRESS_MODIFIER_IMAGE_NAME}-image-dr-report.html""".stripMargin()
                    sh(comm)
                    status = status != 0 ? status : drStatus

                    println('Test GR-Controller Docker image...')
                    comm = testComm.replace('_name_', env.GR_CONTROLLER_IMAGE_NAME)
                    drStatus = sh(script: comm, returnStatus: true).toInteger()

                    println('Run post-test steps...')
                    comm = """mv --verbose \\
                              | image-design-rule-check-report.html \\
                              | ${env.GR_CONTROLLER_IMAGE_NAME}-image-dr-report.html""".stripMargin()
                    sh(comm)
                    status = status != 0 ? status : drStatus

                    println('Test Stub Docker image...')
                    comm = testComm.replace('_name_', env.STUB_IMAGE_NAME)
                    drStatus = sh(script: comm, returnStatus: true).toInteger()

                    println('Run post-test steps...')
                    comm = """mv --verbose \\
                              | image-design-rule-check-report.html \\
                              | ${env.STUB_IMAGE_NAME}-image-dr-report.html""".stripMargin()
                    sh(comm)
                    status = status != 0 ? status : drStatus
                break
                default:
                    println('Run Design Rule Check...')
                    comm = testComm.replaceAll('_name_', env.IMAGE_NAME)
                    status = sh(script: comm, returnStatus: true).toInteger()
                break
            }

            println('Archive reports...')
            archiveArtifacts(artifacts: '*image-*-report.html')

            println('Publish HTML report...')
            publishHTML(target: [ includes: '*image-*-report.html',
                                  reportDir: './',
                                  reportFiles: '*image-*-report.html',
                                  reportName: 'Image Design Rule Check'])

            if(status != 0) {
                sh('exit 1')
            }
        }
    }
}


/* Stage for Build Base Image. Use:
- Args:
    name(require): type String; Docker image name
    version(require): type String; Docker image version
    repository(require): type String; Docker repository path
    file: type String; Docker file path; default is "Dockerfile"
    directory: type String; Docker build directory; default is "."
    arguments: type Map; Build image arguments; default is [:]
*/
def BuildBaseImage(Map Args) {
    String stageName = 'BuildBaseImage'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    version: [value: Args['version'], type: 'string'],
                    repository: [value: Args['repository'], type: 'string'],
                    file: [value: Args['file'], type: 'string', require: false],
                    directory: [value: Args['directory'], type: 'string', require: false],
                    arguments: [value: Args['arguments'], type: 'map', require: false]]
    String comm
    String fullName
    String buildArgs


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['file'] = Args.containsKey('file') ? Args['file'] : 'Dockerfile'
    Args['directory'] = Args.containsKey('directory') ? Args['directory'] : '.'
    Args['arguments'] = Args.containsKey('arguments') ? Args['arguments'] : [:]


    stage('Build Image: ' + Args['name']) {
        // Set build arguments string
        buildArgs = " -f ${Args['directory']}/${Args['file']}"
        for(String arguments in Args['arguments']) {
            buildArgs += " --build-arg ${arguments}"
        }

        println('Image name: ' + Args['name'])
        println('Image version: ' + Args['version'])
        println('Image repository: ' + Args['repository'])
        println('Image file: ' + Args['file'])
        println('Build directory: ' + Args['directory'])
        println('Image arguments: ' + buildArgs)

        fullName = Args['repository'] + '/' + Args['name']
        buildImage( name: fullName,
                    tag: Args['version'],
                    args: buildArgs,
                    file: Args['file'],
                    dir: Args['directory'],
                    this)
    }
}

return this