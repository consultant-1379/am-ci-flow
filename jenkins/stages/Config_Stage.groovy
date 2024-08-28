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
import jenkins.model.Jenkins
import hudson.slaves.EnvironmentVariablesNodeProperty
import org.csanchez.jenkins.plugins.kubernetes.*
import org.csanchez.jenkins.plugins.kubernetes.model.KeyValueEnvVar
import org.csanchez.jenkins.plugins.kubernetes.volumes.*
import static com.ericsson.orchestration.mgmt.libs.VnfmFunctions.checkArgs
import static com.ericsson.orchestration.mgmt.libs.VnfmJenkins.getAgentType


/* Stage is for Setup Kubernetes Plugin. Use:
- Args:
    name(require): type String; Name of the cluster
    params(require): type Map; Parameters of the Kubernetes cluster
*/
def SetupKubernetesPlugin(Map Args) {
    String stageName = 'SetupKubernetesPlugin'
    Map argsList = [name: [value: Args['name'], type: 'string'],
                    params: [value: Args['params'], type: 'map']]


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Setup Kubernetes cluster: ' + Args['name']) {
        def clouds = Jenkins.instance.clouds
        Boolean present = false
        def kubeCloud
        List<PodLabel> labels = new ArrayList<PodLabel>()

        clouds.each {
            if(it.name == Args['name']) {
                present = true
                kubeCloud = it
            }
        }

        if(!present) {
            kubeCloud = new KubernetesCloud(Args['name'])
            Jenkins.instance.clouds.add(kubeCloud)
        }

        Args['params']['labels'].each {
            labels.add(new PodLabel(it.key, it.value))
        }

        kubeCloud.setNamespace(Args['params']['namespace'])
        kubeCloud.setCredentialsId(Args['params']['credential'])
        kubeCloud.setJenkinsTunnel(Args['params']['tunnel'])
        kubeCloud.setConnectTimeout(Args['params']['connectTimeout'])
        kubeCloud.setReadTimeout(Args['params']['readTimeout'])
        kubeCloud.setPodLabels(labels)

        kubeCloud.templates = []
        Args['params']['pods'].each {
            def pod = new PodTemplate()
            def vars = []
            List<ContainerTemplate> contList = new ArrayList<ContainerTemplate>()

            println('Prepare pod template: ' + it.name)
            pod.setName(it.name)
            pod.setLabel(it.labels)
            // Set Concurrency Limit
            if(it.containsKey('limit')) {
                pod.setInstanceCapStr(it.limit)
            }
            // Set Inherit pod template
            if(it.containsKey('inherit')) {
                pod.setInheritFrom(it.inherit)
            }
            // Set Host Network
            if(it.containsKey('hostNetwork')){
                pod.setHostNetwork(it.hostNetwork)
            }

            // Set Pod's envs
            if(it.containsKey('envs')) {
                it.envs.each {
                    var ->
                    vars << new KeyValueEnvVar(var.key, var.value)
                }

                pod.setEnvVars(vars)
            }

            // Set Pod's volumes
            if(it.containsKey('volumes')) {
                List<PodVolume> volumes = []

                it.volumes.each {
                    volume ->

                    if(volume.type == 'emptyDir') {
                        volumes << new EmptyDirVolume(volume.mountPath, false)
                    }
                }

                pod.setVolumes(volumes)
            }


            // Set Containers' configs
            if(it.containsKey('containers')) {
                it.containers.each {
                    item ->
                    ContainerTemplate contTempl = new ContainerTemplate(item.name,
                                                                        item.image)
                    ContainerLivenessProbe probe = new ContainerLivenessProbe("", 0, 0, 0, 0, 0)

                    contTempl.setAlwaysPullImage(item.alwaysPull)
                    contTempl.setWorkingDir(item.directory)
                    contTempl.setCommand(item.command)
                    contTempl.setArgs(item.arguments)
                    contTempl.setTtyEnabled(item.pseudoTTY)
                    contTempl.setPrivileged(item.privilege)
                    contTempl.setResourceRequestCpu(item.requestCPU)
                    contTempl.setResourceRequestMemory(item.requestMemory)
                    contTempl.setResourceLimitCpu(item.limitCPU)
                    contTempl.setResourceLimitMemory(item.limitMemory)

                    // Set Liveness Probe
                    if(item.containsKey('initialDelay')) {
                        probe.setInitialDelaySeconds(item.initialDelay)
                    }
                    contTempl.setLivenessProbe(probe)

                    // Set ports
                    if(item.containsKey('ports')) {
                        List<PortMapping> ports = []

                        item.ports.each {
                            port ->
                            ports.add(new PortMapping(port.name,
                                                      port.containerPort,
                                                      port.hostPort))
                        }
                        contTempl.setPorts(ports)
                    }

                    contList.add(contTempl)
                }
                pod.setContainers(contList)
            }

            kubeCloud.templates << pod
        }

        println(kubeCloud)
        kubeCloud = null
    }
}


/* Stage is for Setup Agent Labels. Use:
- Args:
    labels(require): type List; List of the Jenkins agents labels
*/
def SetupAgentLabels(Map Args) {
    String stageName = 'SetupAgentLabels'
    Map argsList = [labels: [value: Args['labels'], type: 'list']]


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Setup Agent Labels') {
        def slaves = Jenkins.instance.slaves

        // Setup labels for Jenkins agents
        for(def slave in slaves) {
            String newLabels = ''
            String name = slave.getNodeName()
            String type = getAgentType(name)
            String engineLabel = name.replace('-', '_')
            String amLabel = name.replace('process-engine-', 'am_slave_')

            // Skip if agent is not ews type
            if(type != 'vm') {
                continue
            }

            println('INFO: Prepare list of labels for ' + name + ' agent...')
            // Set Engine label if it's not empty
            if(engineLabel != name) {
                newLabels += engineLabel + ' '
            }
            // Set AM label if it's not empty
            if(amLabel != name) {
                newLabels += amLabel + ' '
            }

            // Set labels from input list
            Args['labels'].each {
                if(name in it.agents) {
                    newLabels += it.name + ' '
                }
            }
            // Remove space from end
            newLabels = newLabels[0..-2]

            println('New agent labels: ' + newLabels)

            println('INFO: Setup labels for ' + name + ' agent...')
            slave.setLabelString(newLabels)
        }
    }
}


/* Stage is for Setup Global Environment Variables. Use:
- Args:
    vars(require): type List; List of the Variables
*/
def SetupGlobalVariables(Map Args) {
    String stageName = 'SetupGlobalVariables'
    Map argsList = [vars: [value: Args['vars'], type: 'list']]


    // Checking Arguments
    checkArgs(argsList, stageName, this)


    stage('Setup Global Environment Variables') {
        Jenkins instance = Jenkins.get()
        def nodeProps = instance.getGlobalNodeProperties()
        def envVarsList = nodeProps.getAll(EnvironmentVariablesNodeProperty.class)
        def envVars

        println('INFO: Initialise envVars...')
        envVars = envVarsList.get(0).getEnvVars()

        println('INFO: Add environments to list...')
        for(def var in Args['vars']) {
            println('INFO: Add variable "' + var['name'] + '"...')
            envVars.put(var['name'], var['value'])
        }

        println('INFO: Update global enviroment node properties...')
        instance.save()
    }
}

return this