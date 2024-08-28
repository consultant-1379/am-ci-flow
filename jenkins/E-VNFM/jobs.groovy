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
import javaposse.jobdsl.dsl.DslFactory
import static com.ericsson.orchestration.mgmt.libs.DslUtils.listJobConfig
import com.ericsson.orchestration.mgmt.jobs.SCMPipelineJob
import com.ericsson.orchestration.mgmt.jobs.SCMCronPipelineJob


// Load Jobs configs
String jobDir = 'jenkins/E-VNFM'
def jobsList = listJobConfig(jobDir + '/jobs.yaml', this)
String gerritProject = 'OSS/com.ericsson.orchestration.mgmt/am-integration-charts'
List<String> deployTypes = ['install',
                            'upgrade']
List<String> testNgSlaveLabels = ['testng',
                                  'ews']
List<String> preReleaseType = [ 'pre-release',
                                'pointfix',
                                'pointfix-release']
String preReleaseDesc = '''<dl><dt>Type of the job:</dt>
                          |<dd>- pre-release: Run pre-release tests(Standart flow)</dd>
                          |<dd>- pointfix: Run tests for the pointfix</dd>
                          |<dd>- pointfix-release: Run tests for the pointfix with releasing to helmfile</dd></dl>'''.stripMargin()
List<String> helmDeployType = [ 'deployment',
                                'values-test']
String helmDeployDesc = '''<dl><dt>Type of the job:</dt>
                          |<dd>- deployment: Run standart helmfile deploy steps</dd>
                          |<dd>- values-test: Run helmfile deploy with the testing of a site-values file from oss-integration-ci repository</dd></dl>'''.stripMargin()
List<String> hostnameType = [ 'iccr',
                              'aws']


def jobs = [:]
// Configs for Pre-Release Job
jobs['preRelease'] = jobsList['preRelease']
// Configs for Helmfile Deploy Job
jobs['helmfileDeploy'] = jobsList['helmfileDeploy']
// Configs for Unlock Environment Job
jobs['unlockEnvironment'] = jobsList['unlockEnvironment']
// Configs for TestNG Job
jobs['testNG'] = jobsList['testNG']
// Configs for Helmfile Release Job
jobs['helmfileRelease'] = jobsList['helmfileRelease']
// Configs for CR-Test Job
jobs['testCR'] = jobsList['testCR']


def jobsParams = [:]
// Pre-Release Job parameters
jobsParams['preRelease'] = jobsList['preRelease']['parameters']
jobsParams['preRelease']['JOB_TYPE'] += [value: preReleaseType, description: preReleaseDesc]
jobsParams['preRelease']['TESTNG_SLAVE_LABEL'] += [value: testNgSlaveLabels]
// Helmfile Deploy Job parameters
jobsParams['helmfileDeploy'] = jobsList['helmfileDeploy']['parameters']
jobsParams['helmfileDeploy']['JOB_TYPE'] += [value: helmDeployType, description: helmDeployDesc]
jobsParams['helmfileDeploy']['DEPLOYMENT_TYPE'] += [value: deployTypes]
jobsParams['helmfileDeploy']['HOSTNAME_TYPE'] += [value: hostnameType]
// Unlock Environment Job parameters
jobsParams['unlockEnvironment'] = jobsList['unlockEnvironment']['parameters']
// TestNG Job parameters
jobsParams['testNG'] = jobsList['testNG']['parameters']
jobsParams['testNG']['SLAVE_LABEL'] += [value: testNgSlaveLabels]
// Helmfile Release Job parameters
jobsParams['helmfileRelease'] = jobsList['helmfileRelease']['parameters']
// Pre-Release Job parameters
jobsParams['testCR'] = jobsList['testCR']['parameters']


def jobPreRelease = new SCMPipelineJob(jobs['preRelease'], jobsParams['preRelease'])
def jobHelmfileDeploy = new SCMPipelineJob(jobs['helmfileDeploy'], jobsParams['helmfileDeploy'])
def jobUnlockEnvironment = new SCMPipelineJob(jobs['unlockEnvironment'], jobsParams['unlockEnvironment'])
def jobTestingNG = new SCMPipelineJob(jobs['testNG'], jobsParams['testNG'])
def jobHelmfileRelease = new SCMPipelineJob(jobs['helmfileRelease'], jobsParams['helmfileRelease'])
def jobTestCR = new SCMCronPipelineJob(jobs['testCR'], jobsParams['testCR'])


[ jobPreRelease,
  jobHelmfileDeploy,
  jobUnlockEnvironment,
  jobTestingNG,
  jobHelmfileRelease,
  jobTestCR]*.create(this as DslFactory)