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
String jobDir = 'jenkins/evnfm-rbac'
def jobsList = listJobConfig(jobDir + '/jobs.yaml', this)
String gerritProject = 'OSS/com.ericsson.orchestration.mgmt/evnfm-rbac'


def jobs = [:]
// Configs for Gerrit Unit Tests Job
jobs['gerritUnitTests'] = jobsList['gerritUnitTests']
jobs['gerritUnitTests']['gerritProject'] = gerritProject
// Configs for Gerrit Quality Checks Job
jobs['gerritQualityChecks'] = jobsList['gerritQualityChecks']
jobs['gerritQualityChecks']['gerritProject'] = gerritProject
// Configs for Submit-To-Pipeline Job
jobs['submitToPipeline'] = jobsList['submitToPipeline']
jobs['submitToPipeline']['gerritProject'] = gerritProject
// Configs for Release Flow job
jobs['releaseFlow'] = jobsList['releaseFlow']
// Configs for Post-Merge Job
jobs['postMerge'] = jobsList['postMerge']


def jobGerritQualityChecks = new SCMGerritPipelineJob(jobs['gerritQualityChecks'])
def jobGerritUnitTests = new SCMGerritPipelineJob(jobs['gerritUnitTests'])
def jobSubmitToPipeline = new SCMGerritPipelineJob(jobs['submitToPipeline'])
def jobReleaseFlow = new SCMPipelineJob(jobs['releaseFlow'])
def jobPostMerge = new SCMCronPipelineJob(jobs['postMerge'])


[ jobGerritUnitTests,
  jobGerritQualityChecks,
  jobSubmitToPipeline,
  jobReleaseFlow,
  jobPostMerge]*.create(this as DslFactory)