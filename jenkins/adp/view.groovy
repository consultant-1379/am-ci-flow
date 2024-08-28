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
import javaposse.jobdsl.dsl.views.jobfilter.MatchType
import javaposse.jobdsl.dsl.views.jobfilter.Status
import static com.ericsson.orchestration.mgmt.libs.DslJenkins.getEnv


String PROJECT_NAME = getEnv('JOB_NAME')


listView('ADP Components') {
    description('Jobs for ' + PROJECT_NAME)
    jobs {
        name(PROJECT_NAME)
        regex(PROJECT_NAME + '_.*')
    }
    columns {
        status()
        weather()
        name()
        lastDuration()
        lastSuccess()
        lastBuildConsole()
        buildButton()
        configureProject()
        disableProject()
    }
}