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
import static com.ericsson.orchestration.mgmt.libs.DslJenkins.getEnv
import com.ericsson.orchestration.mgmt.jobs.SeedJob


// Load Jobs configs
def jobsList = listJobConfig('jenkins/main-jobs.yaml', this)


def seedJobs = [:]
// Main seed Job
seedJobs['main'] = jobsList['main']
seedJobs['main']['branchName'] = getEnv('BRANCH', 'master')
// ADP seed Job
seedJobs['adp'] = jobsList['adp']
seedJobs['adp']['branchName'] = getEnv('ADP_BRANCH','master')
// AM-CI-FLOW seed Job
seedJobs['am-ci-flow'] = jobsList['am-ci-flow']
seedJobs['am-ci-flow']['branchName'] = getEnv('AM-CI-FLOW_BRANCH','master')
// AM-COMMON-WFS seed Job
seedJobs['am-common-wfs'] = jobsList['am-common-wfs']
seedJobs['am-common-wfs']['branchName'] = getEnv('AM-COMMON-WFS_BRANCH','master')
// AM-COMMON-WFS-UI seed Job
seedJobs['am-common-wfs-ui'] = jobsList['am-common-wfs-ui']
seedJobs['am-common-wfs-ui']['branchName'] = getEnv('AM-COMMON-WFS-UI_BRANCH','master')
// AM-ONBOARDING-SERVICE seed Job
seedJobs['am-onboarding-service'] = jobsList['am-onboarding-service']
seedJobs['am-onboarding-service']['branchName'] = getEnv('AM-ONBOARDING-SERVICE_BRANCH','master')
// AM-PACKAGE-MANAGER seed Job
seedJobs['am-package-manager'] = jobsList['am-package-manager']
seedJobs['am-package-manager']['branchName'] = getEnv('AM-PACKAGE-MANAGER_BRANCH','master')
// AM-SANDBOX seed Job
seedJobs['am-sandbox'] = jobsList['am-sandbox']
seedJobs['am-sandbox']['branchName'] = getEnv('AM-SANDBOX_BRANCH','master')
// AM-SHARED-UTILITIES seed Job
seedJobs['am-shared-utilities'] = jobsList['am-shared-utilities']
seedJobs['am-shared-utilities']['branchName'] = getEnv('AM-SHARED-UTILITIES_BRANCH','master')
// BATCH-MANAGER seed Job
seedJobs['batch-manager'] = jobsList['batch-manager']
seedJobs['batch-manager']['branchName'] = getEnv('BATCH-MANAGER_BRANCH','master')
// CVNFM-ENM-CLI-STUB seed Job
seedJobs['cvnfm-enm-cli-stub'] = jobsList['cvnfm-enm-cli-stub']
seedJobs['cvnfm-enm-cli-stub']['branchName'] = getEnv('CVNFM-ENM-CLI-STUB_BRANCH','master')
// CVNFMCTL seed Job
seedJobs['cvnfmctl'] = jobsList['cvnfmctl']
seedJobs['cvnfmctl']['branchName'] = getEnv('CVNFMCTL_BRANCH','master')
// DREW seed Job
seedJobs['drew'] = jobsList['drew']
seedJobs['drew']['branchName'] = getEnv('DREW_BRANCH','master')
// ERIC-EO-EVNFM seed Job
seedJobs['eric-eo-evnfm'] = jobsList['eric-eo-evnfm']
seedJobs['eric-eo-evnfm']['branchName'] = getEnv('ERIC-EO-EVNFM_BRANCH','master')
// ERIC-EO-EVNFM-CRYPTO seed Job
seedJobs['eric-eo-evnfm-crypto'] = jobsList['eric-eo-evnfm-crypto']
seedJobs['eric-eo-evnfm-crypto']['branchName'] = getEnv('ERIC-EO-EVNFM-CRYPTO_BRANCH','master')
// ERIC-EO-EVNFM-LIBRARY-CHART seed Job
seedJobs['eric-eo-evnfm-library-chart'] = jobsList['eric-eo-evnfm-library-chart']
seedJobs['eric-eo-evnfm-library-chart']['branchName'] = getEnv('ERIC-EO-EVNFM-LIBRARY-CHART_BRANCH','master')
// ERIC-EO-EVNFM-NBI seed Job
seedJobs['eric-eo-evnfm-nbi'] = jobsList['eric-eo-evnfm-nbi']
seedJobs['eric-eo-evnfm-nbi']['branchName'] = getEnv('ERIC-EO-EVNFM-NBI_BRANCH','master')
// ERIC-EO-LM-CONSUMER seed Job
seedJobs['eric-eo-lm-consumer'] = jobsList['eric-eo-lm-consumer']
seedJobs['eric-eo-lm-consumer']['branchName'] = getEnv('ERIC-EO-LM-CONSUMER_BRANCH','master')
// ERIC-FUNCTION-ORCHESTRATION seed Job
seedJobs['eric-function-orchestration'] = jobsList['eric-function-orchestration']
seedJobs['eric-function-orchestration']['branchName'] = getEnv('ERIC-FUNCTION-ORCHESTRATION_BRANCH','master')
// EVENT-TO-FI seed Job
seedJobs['event-to-fi'] = jobsList['event-to-fi']
seedJobs['event-to-fi']['branchName'] = getEnv('EVENT-TO-FI_BRANCH','master')
// E-VNFM seed Job
seedJobs['evnfm'] = jobsList['evnfm']
seedJobs['evnfm']['branchName'] = getEnv('EVNFM_BRANCH','master')
// EVNFM-RBAC seed Job
seedJobs['evnfm-rbac'] = jobsList['evnfm-rbac']
seedJobs['evnfm-rbac']['branchName'] = getEnv('EVNFM-RBAC_BRANCH','master')
// E-VNFM Test seed Job
seedJobs['evnfm-test'] = jobsList['evnfm-test']
seedJobs['evnfm-test']['branchName'] = getEnv('EVNFM-TEST_BRANCH','master')
// GR-CONTROLLER seed Job
seedJobs['gr-controller'] = jobsList['gr-controller']
seedJobs['gr-controller']['branchName'] = getEnv('GR-CONTROLLER_BRANCH','master')
// JENKINS-SHARED-LIBS seed Job
seedJobs['jenkins-shared-libs'] = jobsList['jenkins-shared-libs']
seedJobs['jenkins-shared-libs']['branchName'] = getEnv('JENKINS-SHARED-LIBS_BRANCH','master')
// MASTER seed Job
seedJobs['master'] = jobsList['master']
seedJobs['master']['branchName'] = getEnv('MASTER_BRANCH','master')
// Security seed Job
seedJobs['security'] = jobsList['security']
seedJobs['security']['branchName'] = getEnv('SECURITY_BRANCH', 'master')
// Services seed Job
seedJobs['services'] = jobsList['services']
seedJobs['services']['branchName'] = getEnv('SERVICES_BRANCH', 'master')
// SIGNATURE-VALIDATION-LIB seed Job
seedJobs['signature-validation-lib'] = jobsList['signature-validation-lib']
seedJobs['signature-validation-lib']['branchName'] = getEnv('SIGNATURE-VALIDATION-LIB_BRANCH','master')
// TOOLS seed Job
seedJobs['tools'] = jobsList['tools']
seedJobs['tools']['branchName'] = getEnv('TOOLS_BRANCH','master')
// VNFM-HELM-EXECUTOR seed Job
seedJobs['vnfm-helm-executor'] = jobsList['vnfm-helm-executor']
seedJobs['vnfm-helm-executor']['branchName'] = getEnv('VNFM-HELM-EXECUTOR_BRANCH','master')
// VNFM-ORCHESTRATOR seed Job
seedJobs['vnfm-orchestrator'] = jobsList['vnfm-orchestrator']
seedJobs['vnfm-orchestrator']['branchName'] = getEnv('VNFM-ORCHESTRATOR_BRANCH','master')
// VNFSDK-PKGTOOLS seed Job
seedJobs['vnfsdk-pkgtools'] = jobsList['vnfsdk-pkgtools']
seedJobs['vnfsdk-pkgtools']['branchName'] = getEnv('VNFSDK-PKGTOOLS_BRANCH','master')


def jobsParams = [:]
// Main seed Job Parameters
jobsParams['main'] = [:]
jobsParams['main']['BRANCH'] = [value: seedJobs['main']['branchName']]
jobsParams['main']['ADP_BRANCH'] = [value: seedJobs['adp']['branchName']]
jobsParams['main']['AM-CI-FLOW_BRANCH'] = [value: seedJobs['am-ci-flow']['branchName']]
jobsParams['main']['AM-COMMON-WFS_BRANCH'] = [value: seedJobs['am-common-wfs']['branchName']]
jobsParams['main']['AM-COMMON-WFS-UI_BRANCH'] = [value: seedJobs['am-common-wfs-ui']['branchName']]
jobsParams['main']['AM-ONBOARDING-SERVICE_BRANCH'] = [value: seedJobs['am-onboarding-service']['branchName']]
jobsParams['main']['AM-PACKAGE-MANAGER_BRANCH'] = [value: seedJobs['am-package-manager']['branchName']]
jobsParams['main']['AM-SANDBOX_BRANCH'] = [value: seedJobs['am-sandbox']['branchName']]
jobsParams['main']['AM-SHARED-UTILITIES_BRANCH'] = [value: seedJobs['am-shared-utilities']['branchName']]
jobsParams['main']['BATCH-MANAGER_BRANCH'] = [value: seedJobs['batch-manager']['branchName']]
jobsParams['main']['CVNFM-ENM-CLI-STUB_BRANCH'] = [value: seedJobs['cvnfm-enm-cli-stub']['branchName']]
jobsParams['main']['CVNFMCTL_BRANCH'] = [value: seedJobs['cvnfmctl']['branchName']]
jobsParams['main']['DREW_BRANCH'] = [value: seedJobs['drew']['branchName']]
jobsParams['main']['ERIC-EO-EVNFM_BRANCH'] = [value: seedJobs['eric-eo-evnfm']['branchName']]
jobsParams['main']['ERIC-EO-EVNFM-CRYPTO_BRANCH'] = [value: seedJobs['eric-eo-evnfm-crypto']['branchName']]
jobsParams['main']['ERIC-EO-EVNFM-LIBRARY-CHART_BRANCH'] = [value: seedJobs['eric-eo-evnfm-library-chart']['branchName']]
jobsParams['main']['ERIC-EO-EVNFM-NBI_BRANCH'] = [value: seedJobs['eric-eo-evnfm-nbi']['branchName']]
jobsParams['main']['ERIC-EO-LM-CONSUMER_BRANCH'] = [value: seedJobs['eric-eo-lm-consumer']['branchName']]
jobsParams['main']['ERIC-FUNCTION-ORCHESTRATION_BRANCH'] = [value: seedJobs['eric-function-orchestration']['branchName']]
jobsParams['main']['EVENT-TO-FI_BRANCH'] = [value: seedJobs['event-to-fi']['branchName']]
jobsParams['main']['EVNFM_BRANCH'] = [value: seedJobs['evnfm']['branchName']]
jobsParams['main']['EVNFM-RBAC_BRANCH'] = [value: seedJobs['evnfm-rbac']['branchName']]
jobsParams['main']['EVNFM-TEST_BRANCH'] = [value: seedJobs['evnfm-test']['branchName']]
jobsParams['main']['GR-CONTROLLER_BRANCH'] = [value: seedJobs['gr-controller']['branchName']]
jobsParams['main']['JENKINS-SHARED-LIBS_BRANCH'] = [value: seedJobs['jenkins-shared-libs']['branchName']]
jobsParams['main']['MASTER_BRANCH'] = [value: seedJobs['master']['branchName']]
jobsParams['main']['SECURITY_BRANCH'] = [value: seedJobs['security']['branchName']]
jobsParams['main']['SERVICES_BRANCH'] = [value: seedJobs['services']['branchName']]
jobsParams['main']['SIGNATURE-VALIDATION-LIB_BRANCH'] = [value: seedJobs['signature-validation-lib']['branchName']]
jobsParams['main']['TOOLS_BRANCH'] = [value: seedJobs['tools']['branchName']]
jobsParams['main']['VNFM-HELM-EXECUTOR_BRANCH'] = [value: seedJobs['vnfm-helm-executor']['branchName']]
jobsParams['main']['VNFM-ORCHESTRATOR_BRANCH'] = [value: seedJobs['vnfm-orchestrator']['branchName']]
jobsParams['main']['VNFSDK-PKGTOOLS_BRANCH'] = [value: seedJobs['vnfsdk-pkgtools']['branchName']]
// ADP seed Job Parameters
jobsParams['adp'] = [:]
jobsParams['adp']['BRANCH'] = [value: seedJobs['adp']['branchName']]
// AM-CI-FLOW seed Job Parameters
jobsParams['am-ci-flow'] = [:]
jobsParams['am-ci-flow']['BRANCH'] = [value: seedJobs['am-ci-flow']['branchName']]
// AM-COMMON-WFS seed Job Parameters
jobsParams['am-common-wfs'] = [:]
jobsParams['am-common-wfs']['BRANCH'] = [value: seedJobs['am-common-wfs']['branchName']]
// AM-COMMON-WFS-UI seed Job Parameters
jobsParams['am-common-wfs-ui'] = [:]
jobsParams['am-common-wfs-ui']['BRANCH'] = [value: seedJobs['am-common-wfs-ui']['branchName']]
// AM-ONBOARDING-SERVICE seed Job Parameters
jobsParams['am-onboarding-service'] = [:]
jobsParams['am-onboarding-service']['BRANCH'] = [value: seedJobs['am-onboarding-service']['branchName']]
// AM-PACKAGE-MANAGER seed Job Parameters
jobsParams['am-package-manager'] = [:]
jobsParams['am-package-manager']['BRANCH'] = [value: seedJobs['am-package-manager']['branchName']]
// AM-SANDBOX seed Job Parameters
jobsParams['am-sandbox'] = [:]
jobsParams['am-sandbox']['BRANCH'] = [value: seedJobs['am-sandbox']['branchName']]
// AM-SHARED-UTILITIES seed Job Parameters
jobsParams['am-shared-utilities'] = [:]
jobsParams['am-shared-utilities']['BRANCH'] = [value: seedJobs['am-shared-utilities']['branchName']]
// BATCH-MANAGER seed Job Parameters
jobsParams['batch-manager'] = [:]
jobsParams['batch-manager']['BRANCH'] = [value: seedJobs['batch-manager']['branchName']]
// CVNFM-ENM-CLI-STUB seed Job Parameters
jobsParams['cvnfm-enm-cli-stub'] = [:]
jobsParams['cvnfm-enm-cli-stub']['BRANCH'] = [value: seedJobs['cvnfm-enm-cli-stub']['branchName']]
// CVNFMCTL seed Job Parameters
jobsParams['cvnfmctl'] = [:]
jobsParams['cvnfmctl']['BRANCH'] = [value: seedJobs['cvnfmctl']['branchName']]
// DREW seed Job Parameters
jobsParams['drew'] = [:]
jobsParams['drew']['BRANCH'] = [value: seedJobs['drew']['branchName']]
// ERIC-EO-EVNFM seed Job Parameters
jobsParams['eric-eo-evnfm'] = [:]
jobsParams['eric-eo-evnfm']['BRANCH'] = [value: seedJobs['eric-eo-evnfm']['branchName']]
// ERIC-EO-EVNFM-CRYPTO seed Job Parameters
jobsParams['eric-eo-evnfm-crypto'] = [:]
jobsParams['eric-eo-evnfm-crypto']['BRANCH'] = [value: seedJobs['eric-eo-evnfm-crypto']['branchName']]
// ERIC-EO-EVNFM-LIBRARY-CHART seed job parameters
jobsParams['eric-eo-evnfm-library-chart'] = [:]
jobsParams['eric-eo-evnfm-library-chart']['BRANCH'] = [value: seedJobs['eric-eo-evnfm-library-chart']['branchName']]
// ERIC-EO-EVNFM-NBI seed Job Parameters
jobsParams['eric-eo-evnfm-nbi'] = [:]
jobsParams['eric-eo-evnfm-nbi']['BRANCH'] = [value: seedJobs['eric-eo-evnfm-nbi']['branchName']]
// ERIC-EO-LM-CONSUMER seed Job Parameters
jobsParams['eric-eo-lm-consumer'] = [:]
jobsParams['eric-eo-lm-consumer']['BRANCH'] = [value: seedJobs['eric-eo-lm-consumer']['branchName']]
// ERIC-FUNCTION-ORCHESTRATION seed Job Parameters
jobsParams['eric-function-orchestration'] = [:]
jobsParams['eric-function-orchestration']['BRANCH'] = [value: seedJobs['eric-function-orchestration']['branchName']]
// EVENT-TO-FI seed Job Parameters
jobsParams['event-to-fi'] = [:]
jobsParams['event-to-fi']['BRANCH'] = [value: seedJobs['event-to-fi']['branchName']]
// E-VNFM seed Job Parameters
jobsParams['evnfm'] = [:]
jobsParams['evnfm']['BRANCH'] = [value: seedJobs['evnfm']['branchName']]
// EVNFM-RBAC seed Job Parameters
jobsParams['evnfm-rbac'] = [:]
jobsParams['evnfm-rbac']['BRANCH'] = [value: seedJobs['evnfm-rbac']['branchName']]
// E-VNFM Test seed Job Parameters
jobsParams['evnfm-test'] = [:]
jobsParams['evnfm-test']['BRANCH'] = [value: seedJobs['evnfm-test']['branchName']]
// GR-CONTROLLER seed Job Parameters
jobsParams['gr-controller'] = [:]
jobsParams['gr-controller']['BRANCH'] = [value: seedJobs['gr-controller']['branchName']]
// JENKINS-SHARED-LIBS seed Job Parameters
jobsParams['jenkins-shared-libs'] = [:]
jobsParams['jenkins-shared-libs']['BRANCH'] = [value: seedJobs['jenkins-shared-libs']['branchName']]
// MASTER seed Job Parameters
jobsParams['master'] = [:]
jobsParams['master']['BRANCH'] = [value: seedJobs['master']['branchName']]
// Security seed Job Parameters
jobsParams['security'] = [:]
jobsParams['security']['BRANCH'] = [value: seedJobs['security']['branchName']]
// Services seed Job Parameters
jobsParams['services'] = [:]
jobsParams['services']['BRANCH'] = [value: seedJobs['services']['branchName']]
// SIGNATURE-VALIDATION-LIB seed Job Parameters
jobsParams['signature-validation-lib'] = [:]
jobsParams['signature-validation-lib']['BRANCH'] = [value: seedJobs['signature-validation-lib']['branchName']]
// TOOLS seed Job Parameters
jobsParams['tools'] = [:]
jobsParams['tools']['BRANCH'] = [value: seedJobs['tools']['branchName']]
// VNFM-HELM-EXECUTOR seed Job Parameters
jobsParams['vnfm-helm-executor'] = [:]
jobsParams['vnfm-helm-executor']['BRANCH'] = [value: seedJobs['vnfm-helm-executor']['branchName']]
// VNFM-ORCHESTRATOR seed Job Parameters
jobsParams['vnfm-orchestrator'] = [:]
jobsParams['vnfm-orchestrator']['BRANCH'] = [value: seedJobs['vnfm-orchestrator']['branchName']]
// VNFSDK-PKGTOOLS seed Job Parameters
jobsParams['vnfsdk-pkgtools'] = [:]
jobsParams['vnfsdk-pkgtools']['BRANCH'] = [value: seedJobs['vnfsdk-pkgtools']['branchName']]


def seedJobMain = new SeedJob(seedJobs['main'], jobsParams['main'])
def seedJobADP = new SeedJob(seedJobs['adp'], jobsParams['adp'])
def seedJobAmCiFlow = new SeedJob(seedJobs['am-ci-flow'], jobsParams['am-ci-flow'])
def seedJobAmCommonWFS = new SeedJob(seedJobs['am-common-wfs'], jobsParams['am-common-wfs'])
def seedJobAmCommonWFSUI = new SeedJob(seedJobs['am-common-wfs-ui'], jobsParams['am-common-wfs-ui'])
def seedJobAmOnboardingService = new SeedJob(seedJobs['am-onboarding-service'], jobsParams['am-onboarding-service'])
def seedJobAmPackageManager = new SeedJob(seedJobs['am-package-manager'], jobsParams['am-package-manager'])
def seedJobAmSandbox = new SeedJob(seedJobs['am-sandbox'], jobsParams['am-sandbox'])
def seedJobAmSharedUtilities = new SeedJob(seedJobs['am-shared-utilities'], jobsParams['am-shared-utilities'])
def seedJobBatchManager = new SeedJob(seedJobs['batch-manager'], jobsParams['batch-manager'])
def seedJobDrewManager = new SeedJob(seedJobs['drew'], jobsParams['drew'])
def seedJobCvnfmctl = new SeedJob(seedJobs['cvnfmctl'], jobsParams['cvnfmctl'])
def seedJobCvnfmEnmCliStub = new SeedJob(seedJobs['cvnfm-enm-cli-stub'], jobsParams['cvnfm-enm-cli-stub'])
def seedJobEricEoEvnfm = new SeedJob(seedJobs['eric-eo-evnfm'], jobsParams['eric-eo-evnfm'])
def seedJobEricEoEvnfmCrypto = new SeedJob(seedJobs['eric-eo-evnfm-crypto'], jobsParams['eric-eo-evnfm-crypto'])
def seedJobEricEoEvnfmLibraryChart = new SeedJob(seedJobs['eric-eo-evnfm-library-chart'], jobsParams['eric-eo-evnfm-library-chart'])
def seedJobEricEoEvnfmNBI = new SeedJob(seedJobs['eric-eo-evnfm-nbi'], jobsParams['eric-eo-evnfm-nbi'])
def seedJobEricEoLMConsumer = new SeedJob(seedJobs['eric-eo-lm-consumer'], jobsParams['eric-eo-lm-consumer'])
def seedJobEricFunctionOrchestration = new SeedJob(seedJobs['eric-function-orchestration'], jobsParams['eric-function-orchestration'])
def seedJobEventToFI = new SeedJob(seedJobs['event-to-fi'], jobsParams['event-to-fi'])
def seedJobEVNFM = new SeedJob(seedJobs['evnfm'], jobsParams['evnfm'])
def seedJobEvnfmRBAC = new SeedJob(seedJobs['evnfm-rbac'], jobsParams['evnfm-rbac'])
def seedJobEvnfmTest = new SeedJob(seedJobs['evnfm-test'], jobsParams['evnfm-test'])
def seedJobGRController = new SeedJob(seedJobs['gr-controller'], jobsParams['gr-controller'])
def seedJobJenkinsSharedLibs = new SeedJob(seedJobs['jenkins-shared-libs'], jobsParams['jenkins-shared-libs'])
def seedJobMaster = new SeedJob(seedJobs['master'], jobsParams['master'])
def seedJobSecurity = new SeedJob(seedJobs['security'], jobsParams['security'])
def seedJobServices = new SeedJob(seedJobs['services'], jobsParams['services'])
def seedJobSignatureValidationLib = new SeedJob(seedJobs['signature-validation-lib'], jobsParams['signature-validation-lib'])
def seedJobTools = new SeedJob(seedJobs['tools'], jobsParams['tools'])
def seedJobVnfmHelmExecutor = new SeedJob(seedJobs['vnfm-helm-executor'], jobsParams['vnfm-helm-executor'])
def seedJobVnfmOrchestrator = new SeedJob(seedJobs['vnfm-orchestrator'], jobsParams['vnfm-orchestrator'])
def seedJobVnfsdkPkgtools = new SeedJob(seedJobs['vnfsdk-pkgtools'], jobsParams['vnfsdk-pkgtools'])


[ seedJobMain,
  seedJobADP,
  seedJobAmCiFlow,
  seedJobAmCommonWFS,
  seedJobAmCommonWFSUI,
  seedJobAmOnboardingService,
  seedJobAmPackageManager,
  seedJobAmSandbox,
  seedJobAmSharedUtilities,
  seedJobDrewManager,
  seedJobBatchManager,
  seedJobCvnfmEnmCliStub,
  seedJobCvnfmctl,
  seedJobEricEoEvnfm,
  seedJobEricEoEvnfmCrypto,
  seedJobEricEoEvnfmLibraryChart,
  seedJobEricEoEvnfmNBI,
  seedJobEricEoLMConsumer,
  seedJobEricFunctionOrchestration,
  seedJobEventToFI,
  seedJobEVNFM,
  seedJobEvnfmRBAC,
  seedJobEvnfmTest,
  seedJobGRController,
  seedJobJenkinsSharedLibs,
  seedJobMaster,
  seedJobSecurity,
  seedJobServices,
  seedJobSignatureValidationLib,
  seedJobTools,
  seedJobVnfmHelmExecutor,
  seedJobVnfmOrchestrator,
  seedJobVnfsdkPkgtools]*.create(this as DslFactory)