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
import com.ericsson.orchestration.mgmt.jobs.SCMGerritPipelineJob
import com.ericsson.orchestration.mgmt.jobs.SCMPipelineJob


// Load Jobs configs
String jobDir = 'jenkins/event-to-fi'
def jobsList = listJobConfig(jobDir + '/jobs.yaml', this)
String gerritProject = 'oss/com.ericsson.oss.ro/eric-eo-fh-event-to-alarm-adapter'


def jobs = [:]
// Configs for Gerrit Unit tests Job
jobs['gerritUnitTests'] = jobsList['gerritUnitTests']
jobs['gerritUnitTests']['gerritProject'] = gerritProject
// Configs for Gerrit Deployment Job
jobs['gerritDeployment'] = jobsList['gerritDeployment']
jobs['gerritDeployment']['gerritProject'] = gerritProject
// Configs for Gerrit Quality Checks Job
jobs['gerritQualityChecks'] = jobsList['gerritQualityChecks']
jobs['gerritQualityChecks']['gerritProject'] = gerritProject
// Configs for Pre-Release Job
jobs['preRelease'] = jobsList['preRelease']
// Configs for Submit-To-Pipeline Job
jobs['submitToPipeline'] = jobsList['submitToPipeline']
jobs['submitToPipeline']['gerritProject'] = gerritProject
// Configs for Release Flow Job
jobs['releaseFlow'] = jobsList['releaseFlow']
// Configs for Post-Merge Job
jobs['postMerge'] = jobsList['postMerge']
// Configs for Uplift Version Job
jobs['upliftVersion'] = jobsList['upliftVersion']


def jobsParams = [:]
// Release Flow Job Parameters
jobsParams['releaseFlow'] = jobsList['releaseFlow']['parameters']


def jobGerritUnitTests = new SCMGerritPipelineJob(jobs['gerritUnitTests'])
def jobGerritDeployment = new SCMGerritPipelineJob(jobs['gerritDeployment'])
def jobGerritQualityChecks = new SCMGerritPipelineJob(jobs['gerritQualityChecks'])
def jobPreRelease = new SCMPipelineJob(jobs['preRelease'])
def jobSubmitToPipeline = new SCMGerritPipelineJob(jobs['submitToPipeline'])
def jobReleaseFlow = new SCMPipelineJob(jobs['releaseFlow'], jobsParams['releaseFlow'])
def jobPostMerge = new SCMCronPipelineJob(jobs['postMerge'])
def jobUpliftVersion = new SCMPipelineJob(jobs['upliftVersion'])


[ jobGerritUnitTests,
  jobGerritDeployment,
  jobGerritQualityChecks,
  jobPreRelease,
  jobSubmitToPipeline,
  jobReleaseFlow,
  jobPostMerge,
  jobUpliftVersion]*.create(this as DslFactory)