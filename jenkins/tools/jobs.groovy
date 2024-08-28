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


// Load Jobs configs
String jobDir = 'jenkins/tools'
def jobsList = listJobConfig(jobDir + '/jobs.yaml', this)
List<String> chartName = ['eric-eo-helmfile',
                          'eric-eo-evnfm',
                          'eric-cnbase-oss-config',
                          'eric-oss-function-orchestration-common',
                          'eric-cloud-native-base',
                          'eric-cncs-oss-config',
                          'eric-cloud-native-service-mesh',
                          'eric-oss-common-base',
                          'eric-eo-cm',
                          'eric-eo-act-cna',
                          'eric-oss-eo-clm']
List<String> type = ['simple',
                     'eric-eo-helmfile']
List<String> branchActions = ['LOCK',
                              'UNLOCK']
List<String> certsType = ['self-signed']
List<String> versionType = [ 'stable']


def jobs = [:]
// Configs for Unlock Resource Job
jobs['unlockResource'] = jobsList['unlockResource']
// Configs for Lock Resource Job
jobs['lockResource'] = jobsList['lockResource']
// Configs for Tests CSAR Job
jobs['testsCSAR'] = jobsList['testsCSAR']
// Configs for Uplift Version Job
jobs['upliftVersion'] = jobsList['upliftVersion']
// Configs for Build CSARs Job
jobs['buildCSARs'] = jobsList['buildCSARs']
// Configs for Branch Lock Job
jobs['branchlock'] = jobsList['branchlock']
// Uplift Child Chart Job
jobs['upliftChildChart'] = jobsList['upliftChildChart']
// Generate Certificates Job
jobs['generateCertificates'] = jobsList['generateCertificates']
// DR Check Job
jobs['drCheck'] = jobsList['drCheck']
// Configs for Get Chart Version Job
jobs['getChartVersion'] = jobsList['getChartVersion']


def jobsParams = [:]
// Unlock Resource Job Parameters
jobsParams['unlockResource'] = jobsList['unlockResource']['parameters']
// Lock Resource Job Parameters
jobsParams['lockResource'] = jobsList['lockResource']['parameters']
// Tests CSAR Job Parameters
jobsParams['testsCSAR'] = jobsList['testsCSAR']['parameters']
// Build CSARs Job parameters
jobsParams['buildCSARs'] = jobsList['buildCSARs']['parameters']
jobsParams['buildCSARs']['CHART_NAME'] += [value: chartName]
jobsParams['buildCSARs']['TYPE'] += [value: type]
// Branch lock Job Parameters
jobsParams['branchlock'] = jobsList['branchlock']['parameters']
jobsParams['branchlock']['ACTION'] += [value: branchActions]
// Uplift Child Chart Job Parameters
jobsParams['upliftChildChart'] = jobsList['upliftChildChart']['parameters']
// Generate Certificates Job Parameters
jobsParams['generateCertificates'] = jobsList['generateCertificates']['parameters']
jobsParams['generateCertificates']['TYPE'] += [value: certsType]
// DR Check Job Parameters
jobsParams['drCheck'] = jobsList['drCheck']['parameters']
// Get Chart Version Job Parameters
jobsParams['getChartVersion'] = jobsList['getChartVersion']['parameters']
jobsParams['getChartVersion']['VERSION_TYPE'] += [value: versionType]


def jobsUnlockResource = new SCMPipelineJob(jobs['unlockResource'], jobsParams['unlockResource'])
def jobsLockResource = new SCMPipelineJob(jobs['lockResource'], jobsParams['lockResource'])
def jobsTestsCSAR = new SCMPipelineJob(jobs['testsCSAR'], jobsParams['testsCSAR'])
def jobUpliftVersion = new SCMPipelineJob(jobs['upliftVersion'])
def jobBuildCSARs = new SCMPipelineJob(jobs['buildCSARs'], jobsParams['buildCSARs'])
def jobsBranchLock = new SCMPipelineJob(jobs['branchlock'], jobsParams['branchlock'])
def jobsUpliftChildChart = new SCMPipelineJob(jobs['upliftChildChart'], jobsParams['upliftChildChart'])
def jobsGenerateCertificates = new SCMPipelineJob(jobs['generateCertificates'], jobsParams['generateCertificates'])
def jobsDrCheck = new SCMPipelineJob(jobs['drCheck'], jobsParams['drCheck'])
def jobsGetChartVersion = new SCMPipelineJob(jobs['getChartVersion'], jobsParams['getChartVersion'])


[ jobsUnlockResource,
  jobsLockResource,
  jobsTestsCSAR,
  jobUpliftVersion,
  jobBuildCSARs,
  jobsBranchLock,
  jobsUpliftChildChart,
  jobsGenerateCertificates,
  jobsDrCheck,
  jobsGetChartVersion]*.create(this as DslFactory)