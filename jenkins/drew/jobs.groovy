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
String jobDir = 'jenkins/drew'
def jobsList = listJobConfig(jobDir + '/jobs.yaml', this)
String gerritProject = 'OSS/com.ericsson.orchestration.mgmt/am-cvnfm-utils'


def jobs = [:]
// Configs for Gerrit Unit tests Job
jobs['gerritUnitTests'] = jobsList['gerritUnitTests']
jobs['gerritUnitTests']['gerritProject'] = gerritProject


def jobGerritUnitTests = new SCMGerritPipelineJob(jobs['gerritUnitTests'])


[ jobGerritUnitTests]*.create(this as DslFactory)