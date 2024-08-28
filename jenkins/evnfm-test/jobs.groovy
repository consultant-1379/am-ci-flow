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
String jobDir = 'jenkins/evnfm-test'
def jobsList = listJobConfig(jobDir + '/jobs.yaml', this)


def jobs = [:]
// Configs for HA robustness tests Job
jobs['haRobustnessTests'] = jobsList['haRobustnessTests']
// Configs for HA Deploy Job
jobs['haDeploy'] = jobsList['haDeploy']
// Configs for HA robustness E2E Job
jobs['haRobustnessE2E'] = jobsList['haRobustnessE2E']


def jobsParams = [:]
// HA robustness tests Job parameters
jobsParams['haRobustnessTests'] = jobsList['haRobustnessTests']['parameters']
// HA Deploy Job parameters
jobsParams['haDeploy'] = jobsList['haDeploy']['parameters']
// HA robustness E2E Job parameters
jobsParams['haRobustnessE2E'] = jobsList['haRobustnessE2E']['parameters']


def jobHaRobustnessTests = new SCMPipelineJob(jobs['haRobustnessTests'], jobsParams['haRobustnessTests'])
def jobHaDeploy = new SCMPipelineJob(jobs['haDeploy'], jobsParams['haDeploy'])
def jobHaRobustnessE2E = new SCMPipelineJob(jobs['haRobustnessE2E'], jobsParams['haRobustnessE2E'])


[ jobHaRobustnessTests,
  jobHaDeploy,
  jobHaRobustnessE2E]*.create(this as DslFactory)