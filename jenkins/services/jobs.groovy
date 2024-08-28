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
import com.ericsson.orchestration.mgmt.jobs.SCMCronPipelineJob
import com.ericsson.orchestration.mgmt.jobs.SCMPipelineJob


// Load Jobs configs
String jobDir = 'jenkins/services'
def jobsList = listJobConfig(jobDir + '/jobs.yaml', this)
List<String> clusterPodLimitCluster = [ 'hart066',
                                        'hart070',
                                        'haber002']


def jobs = [:]
// Configs for Jobs Discover Job
jobs['discover'] = jobsList['discover']
// Configs for Cleanup Cluster Job
jobs['cleanupCluster'] = jobsList['cleanupCluster']
// Configs for Load Cluster Report Job
jobs['loadClusterReport'] = jobsList['loadClusterReport']
// Configs for Agents Cleanup Job
jobs['agentsCleanup'] = jobsList['agentsCleanup']
// Configs for Build Images Job
jobs['buildImages'] = jobsList['buildImages']
// Configs for Reload Pipelines Job
jobs['reloadPipelines'] = jobsList['reloadPipelines']
// Configs for Reload Spinnaker Pipelines Job
jobs['reloadSpinnakerPipelines'] = jobsList['reloadSpinnakerPipelines']
// Configs for Setup Jenkins Job
jobs['setupJenkins'] = jobsList['setupJenkins']
// Configs for Setup Max Pod Limit Job
jobs['clusterPodLimit'] = jobsList['clusterPodLimit']
// Configs for Gerrit Tests Job
jobs['gerritTests'] = jobsList['gerritTests']


def jobsParams = [:]
// Agents Cleanup Job Parameters
jobsParams['agentsCleanup'] = jobsList['agentsCleanup']['parameters']
// Setup Max Pod Limit Job Parameters
jobsParams['clusterPodLimit'] = jobsList['clusterPodLimit']['parameters']
jobsParams['clusterPodLimit']['CLUSTER'] += [value: clusterPodLimitCluster]
// Gerrit Tests Job Parameters
jobsParams['gerritTests'] = jobsList['gerritTests']['parameters']


def jobsDiscover = new SCMPipelineJob(jobs['discover'])
def jobsCleanupCluster = new SCMCronPipelineJob(jobs['cleanupCluster'])
def jobsLoadClusterReport = new SCMCronPipelineJob(jobs['loadClusterReport'])
def jobsAgentsCleanup = new SCMCronPipelineJob(jobs['agentsCleanup'], jobsParams['agentsCleanup'])
def jobsBuildImages = new SCMCronPipelineJob(jobs['buildImages'])
def jobsReloadPipelines = new SCMCronPipelineJob(jobs['reloadPipelines'])
def jobsReloadSpinnakerPipelines = new SCMCronPipelineJob(jobs['reloadSpinnakerPipelines'])
def jobsSetupJenkins = new SCMCronPipelineJob(jobs['setupJenkins'])
def jobsClusterPodLimit = new SCMPipelineJob(jobs['clusterPodLimit'], jobsParams['clusterPodLimit'])
def jobsGerritTests = new SCMPipelineJob(jobs['gerritTests'], jobsParams['gerritTests'])


[ jobsDiscover,
  jobsCleanupCluster,
  jobsLoadClusterReport,
  jobsAgentsCleanup,
  jobsBuildImages,
  jobsReloadPipelines,
  jobsReloadSpinnakerPipelines,
  jobsSetupJenkins,
  jobsClusterPodLimit,
  jobsGerritTests]*.create(this as DslFactory)