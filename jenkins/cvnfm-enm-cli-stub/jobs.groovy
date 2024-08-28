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
import com.ericsson.orchestration.mgmt.jobs.SCMGerritPipelineJob
import com.ericsson.orchestration.mgmt.jobs.SCMPipelineJob


// Load Jobs configs
String jobDir = 'jenkins/cvnfm-enm-cli-stub'
def jobsList = listJobConfig(jobDir + '/jobs.yaml', this)
String gerritProject = 'OSS/com.ericsson.orchestration.mgmt/cvnfm-enm-cli-stub'


def jobs = [:]
// Configs for Gerrit Deployment Job
jobs['gerritDeployment'] = jobsList['gerritDeployment']
jobs['gerritDeployment']['gerritProject'] = gerritProject
// Configs for Gerrit Quality Checks Job
jobs['gerritQualityChecks'] = jobsList['gerritQualityChecks']
jobs['gerritQualityChecks']['gerritProject'] = gerritProject
// Configs for Release Flow Job
jobs['releaseFlow'] = jobsList['releaseFlow']
// Configs for Submit-To-Pipeline Job
jobs['submitToPipeline'] = jobsList['submitToPipeline']
jobs['submitToPipeline']['gerritProject'] = gerritProject


def jobGerritDeployment = new SCMGerritPipelineJob(jobs['gerritDeployment'])
def jobGerritQualityChecks = new SCMGerritPipelineJob(jobs['gerritQualityChecks'])
def jobReleaseFlow = new SCMPipelineJob(jobs['releaseFlow'])
def jobSubmitToPipeline = new SCMGerritPipelineJob(jobs['submitToPipeline'])


[ jobGerritDeployment,
  jobGerritQualityChecks,
  jobReleaseFlow,
  jobSubmitToPipeline]*.create(this as DslFactory)