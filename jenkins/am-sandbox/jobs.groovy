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
String jobDir = 'jenkins/am-sandbox'
def jobsList = listJobConfig(jobDir + '/jobs.yaml', this)
String gerritProject = 'OSS/com.ericsson.orchestration.mgmt/am-sandbox'


def jobs = [:]
// Configs for Gerrit Flow job
jobs['gerritFlow'] = jobsList['gerritFlow']
jobs['gerritFlow']['gerritProject'] = gerritProject
// Configs for Release Flow job
jobs['releaseFlow'] = jobsList['releaseFlow']
// Configs for Submit-To-Pipeline Job
jobs['submitToPipeline'] = jobsList['submitToPipeline']
jobs['submitToPipeline']['gerritProject'] = gerritProject


def jobGerritFlow = new SCMGerritPipelineJob(jobs['gerritFlow'])
def jobReleaseFlow = new SCMPipelineJob(jobs['releaseFlow'])
def jobSubmitToPipeline = new SCMGerritPipelineJob(jobs['submitToPipeline'])


[ jobGerritFlow,
  jobReleaseFlow,
  jobSubmitToPipeline]*.create(this as DslFactory)