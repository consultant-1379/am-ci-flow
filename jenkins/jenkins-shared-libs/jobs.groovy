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


// Load Jobs configs
String jobDir = 'jenkins/jenkins-shared-libs'
def jobsList = listJobConfig(jobDir + '/jobs.yaml', this)
String gerritProject = 'OSS/com.ericsson.orchestration.mgmt/am-ci-flow/jenkins-shared-libs'


def jobs = [:]
// Configs for Gerrit Quality Checks Job
jobs['gerritQualityChecks'] = jobsList['gerritQualityChecks']
jobs['gerritQualityChecks']['gerritProject'] = gerritProject
// Configs for Post-Merge Job
jobs['postMerge'] = jobsList['postMerge']
jobs['postMerge']['gerritProject'] = gerritProject


def jobsParams = [:]
// Post-Merge Job Parameters
jobsParams['postMerge'] = jobsList['postMerge']['parameters']


def jobGerritQualityChecks = new SCMGerritPipelineJob(jobs['gerritQualityChecks'])
def jobPostMerge = new SCMGerritPipelineJob(jobs['postMerge'], jobsParams['postMerge'])


[ jobGerritQualityChecks,
  jobPostMerge]*.create(this as DslFactory)