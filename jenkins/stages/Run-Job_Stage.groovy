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
import groovy.json.JsonOutput
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkArgs
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkNotBlank
import static com.ericsson.orchestration.mgmt.libs.VnfmJenkins.createJobParameter
import static com.ericsson.orchestration.mgmt.libs.VnfmJenkins.getCredential
import static com.ericsson.orchestration.mgmt.libs.VnfmJenkins.setJobParameter


/* Stage for Run Local Job. Use:
- VARs:
    ARTIFACTS_DIR
- Args:
    name(require): type String; Name of the job
    stage: type String; Stage custom name; default is ''
    params: type List; Parameters of the job; default is []
    artifact: type String; Name of the triggred job artifact; default is ''
    artifactName: type String; Name of the saved artifact; default is ''
    skip: type Boolean; if true to skip the current stage; default is false
    wait: type Boolean; Wait the result of job or not; default is true
    isFail: type Boolean; Fail the stage if the job is failed; default is true
*/
def LocalJob(Map Args) {
    String stageName = 'LocalJob'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    stage: [value: Args['stage'], type: 'string', require: false],
                    params: [value: Args['params'], type: 'list', require: false],
                    artifact: [value: Args['artifact'], type: 'string', require: false],
                    artifactName: [value: Args['artifactName'], type: 'string', require: false],
                    skip: [value: Args['skip'], type: 'bool', require: false],
                    wait: [value: Args['wait'], type: 'bool', require: false],
                    isFail: [value: Args['isFail'], type: 'bool', require: false]]


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['stage'] = Args.containsKey('stage') ? Args['stage'] : Args['name']
    Args['params'] = Args.containsKey('params') ? Args['params'] : []
    Args['artifact'] = Args.containsKey('artifact') ? Args['artifact'] : ''
    Args['artifactName'] = Args.containsKey('artifactName') ? Args['artifactName'] : ''
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false
    Args['wait'] = Args.containsKey('wait') ? Args['wait'] : true
    Args['isFail'] = Args.containsKey('isFail') ? Args['isFail'] : true


    stage(Args['stage']) {
        // Set default stage status
        env[STAGE_NAME] = 'failed'

        if(Args['skip']) {
            env[STAGE_NAME] = 'aborted'
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            List<ParameterValue> jobParams = new ArrayList<ParameterValue>()
            def job
            String jobUrl
            String archivePath
            String httpCode
            String message


            println('INFO: Set parameters for Job...')
            for(Map par in Args['params']) {
                def jobParam = createJobParameter(name: par['name'],
                                                  value: par['value'],
                                                  this)
                jobParams.add(jobParam)
            }

            println('INFO: Parameters for the running Job...')
            println(jobParams)

            println('INFO: Run Job...')
            job = build(job: Args['name'],
                        parameters: jobParams,
                        wait: Args['wait'],
                        propagate: false)

            if(Args['artifact']) {
                println('INFO: Set artifact archivePath value...')
                switch(true) {
                    case checkNotBlank(Args['artifactName']):
                        archivePath = env.ARTIFACTS_DIR + '/' + Args['artifactName']
                    break
                    case Args['artifact'][-5..-1] == '.html':
                        archivePath = env.ARTIFACTS_DIR + '/' + Args['artifact']
                    break
                    default:
                        archivePath = env.ARTIFACTS_DIR + '/' + Args['stage'] + '.artifact'
                    break
                }

                println('INFO: Set jobUrl value...')
                jobUrl = job.getAbsoluteUrl().toString()
                jobUrl = jobUrl[-1] == '/' ? jobUrl[0..-2] : jobUrl

                println('INFO: Create Artifacts directory...')
                comm = 'mkdir -p ' + env.ARTIFACTS_DIR
                comm += ' || echo "WARNING: Failed to create directory"'
                sh(comm)

                println('INFO: Get artifact...')
                withCredentials([usernamePassword(credentialsId: getCredential('ldap'),
                                                  usernameVariable: 'LDAP_USER',
                                                  passwordVariable: 'LDAP_PASSWORD')]) {
                    comm = """curl -u '${LDAP_USER}:${LDAP_PASSWORD}' \\
                              | ${jobUrl}/artifact/${Args['artifact']} \\
                              | -w "%{http_code}" \\
                              | -o '${archivePath}'""".stripMargin()
                    httpCode = sh(script: comm, returnStdout: true).trim()
                }

                if(httpCode == '200') {
                    println('INFO: Archive artifact...')
                    archiveArtifacts( artifacts: archivePath,
                                      allowEmptyArchive: true,
                                      onlyIfSuccessful: false)
                }
            }

            // Check the Job status
            switch(true) {
                case !Args['wait']:
                    message = 'INFO: Checking the status of Job "' + Args['name'] + '" was skipped'
                    println(message)

                    // Set stage status
                    env[STAGE_NAME] = 'success'
                break
                case job.getResult() == 'UNSTABLE':
                    message = 'WARNING: Job "' + Args['name'] + '" was unstable'
                    unstable(message: message)

                    // Set stage status
                    env[STAGE_NAME] = 'unstable'
                break
                case Args['isFail'] && job.getResult() == 'FAILURE':
                    message = 'ERROR: Job "' + Args['name'] + '" failed'
                    error(message: message)
                break
                case job.getResult() == 'FAILURE':
                    message = 'WARNING: Job "' + Args['name'] + '" failed'
                    unstable(message: message)

                    // Set stage status
                    env[STAGE_NAME] = 'unstable'
                break
                case Args['isFail'] && checkNotBlank(Args['artifact']) && httpCode != '200':
                    message = 'ERROR: Failed to get artifacts from build "' + jobUrl + '"'
                    error(message: message)
                break
                default:
                    message = 'INFO: Job "' + Args['name'] + '" status was ' + job.getResult()
                    println(message)

                    // Set stage status
                    env[STAGE_NAME] = 'success'
                break
            }
        }
    }
}


/* Stage for Run Local Job. Use:
- VARs:
    ARTIFACTS_DIR
- Args:
    name(require): type String; Name of the job
    server(require): type String; Name of the Jenkins server
    params(require): type String; Parameters of the job
    stage: type String; Stage custom name; default is 'server:name'
    artifact: type String; default is ''
    skip: type Boolean; if true to skip the current stage; default is false
*/
def RemoteJob(Map Args) {
    String stageName = 'RemoteJob'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    server: [value: Args['server'], type: 'string'],
                    params: [value: Args['params'], type: 'string'],
                    stage: [value: Args['stage'], type: 'string', require: false],
                    artifact: [value: Args['artifact'], type: 'string', require: false],
                    skip: [value: Args['skip'], type: 'bool', require: false]]


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['stage'] = Args.containsKey('stage') ? Args['stage'] : Args['server'] + ':' + Args['name']
    Args['artifact'] = Args.containsKey('artifact') ? Args['artifact'] : ''
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false


    stage(Args['stage']) {
        if(Args['skip']) {
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            String comm
            def remoteJob
            String jobUrl
            String paramsFile = Args['stage'] + '.params'
            String archivePath = env.ARTIFACTS_DIR + '/' + Args['stage'] + '.artifact'


            println('Prepare params file...')
            writeFile(file: paramsFile, text: Args['params'])

            println('Run Job...')
            remoteJob = triggerRemoteJob( job: Args['name'],
                                          remoteJenkinsName: Args['server'],
                                          blockBuildUntilComplete: true,
                                          shouldNotFailBuild: false,
                                          preventRemoteBuildQueue: true,
                                          pollInterval: 30,
                                          loadParamsFromFile: true,
                                          parameterFile: paramsFile)

            println('Delete tempory files from workspace...')
            comm = "rm -rf ${paramsFile}"
            sh(comm)

            if(Args['artifact']) {
                println('Set jobUrl value...')
                jobUrl = remoteJob.getBuildUrl().toString()
                jobUrl = jobUrl[-1] == '/' ? jobUrl[0..-2] : jobUrl

                println('Create Artifacts directory...')
                comm = "mkdir -p ${env.ARTIFACTS_DIR} || echo 'INFO: Failed to create directory'"
                sh(comm)

                println('Get artifact...')
                withCredentials([usernamePassword(credentialsId: getCredential('ldap'),
                                                  usernameVariable: 'LDAP_USER',
                                                  passwordVariable: 'LDAP_PASSWORD')]) {
                    comm = """curl -u '${LDAP_USER}:${LDAP_PASSWORD}' \\
                              | ${jobUrl}/artifact/${Args['artifact']} \\
                              | -o '${archivePath}'""".stripMargin()
                    sh(comm)
                }

                println('Archive artifact...')
                archiveArtifacts( artifacts: archivePath,
                                  allowEmptyArchive: true,
                                  onlyIfSuccessful: false)
            }
        }
    }
}


/* Stage for Trigger Spinnaker Job
*/
def TriggerSpinnaker(String name) {
    stage('Call Spinnaker Webhook') {
        def json = JsonOutput.toJson(params)
        def post = new URL("https://spinnaker-api.rnd.gic.ericsson.se/webhooks/webhook/${name}").openConnection()
        post.setRequestMethod("POST")
        post.setDoOutput(true)
        post.setRequestProperty("Content-Type", "application/json")
        post.getOutputStream().write(json.getBytes("UTF-8"))

        def postRC = post.getResponseCode()
        if(postRC.equals(200)) {
            println(post.getInputStream().getText())
        }
        else {
            println(postRC)
        }
    }
}


/* Stage for Trigger Spinnaker Job. Use:
- Args:
    hook(require): type String; Name of the Spinnaker webhook
    params(require): type Map; Parameters of the Spinnaker job
    skip: type Boolean; if true to skip the current stage; default false
*/
def SpinnakerJob(Map Args) {
    String stageName = 'SpinnakerJob'
    Map argsList = [hook: [value: Args['hook'], type: 'string'],
                    params: [value: Args['params'], type: 'map', require: false],
                    skip: [value: Args['skip'], type: 'bool', require: false]]
    String url
    def hookParams


    // Checking Arguments
    checkArgs(argsList, stageName, this)
    Args['skip'] = Args.containsKey('skip') ? Args['skip'] : false


    stage('Trigger Spinnaker job: ' + Args['hook']) {
        if(Args['skip']) {
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            url = 'https://spinnaker-api.rnd.gic.ericsson.se/webhooks/webhook/' + Args['hook']
            println('Hook URL: ' + url)

            def post = new URL(url).openConnection()
            post.setRequestMethod("POST")
            post.setDoOutput(true)
            post.setRequestProperty("Content-Type", "application/json")
            // Set parameters for the Spinnaker's hook
            hookParams = JsonOutput.toJson(Args['params'])
            post.getOutputStream().write(hookParams.getBytes("UTF-8"))

            def postRC = post.getResponseCode()
            if(postRC.equals(200)) {
                println(post.getInputStream().getText())
            }
            else {
                println(postRC)
            }
        }
    }
}


/* Stage for Run Seed Job. Use:
- Args:
    name(require): type String; Name of the seed job
    params(require): type Map; Job parameters
*/
def RunSeedJob(Map Args) {
    String stageName = 'RunSeedJob'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    params: [value: Args['params'], type: 'map']]
    List<ParameterValue> jobParams


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Run ' + Args['name'] + ' seed job') {
        println("Set Job's parameters...")
        jobParams = setJobParameter(name: Args['name'],
                                    params: Args['params'],
                                    this)

        println('Run Job...')
        build(job: Args['name'],
              parameters: jobParams,
              propagate: false,
              wait: true)
    }
}


return this