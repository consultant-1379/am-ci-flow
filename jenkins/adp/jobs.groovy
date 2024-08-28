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


// Load Jobs configs
String jobDir = 'jenkins/adp'
def jobsList = listJobConfig(jobDir + '/jobs.yaml', this)


def jobs = [:]
// Configs for Tosca-O Job
jobs['toscaoVersion'] = jobsList['toscaoVersion']


def jobsToscaoVersion = new SCMCronPipelineJob(jobs['toscaoVersion'])


// [ jobsToscaoVersion]*.create(this as DslFactory)