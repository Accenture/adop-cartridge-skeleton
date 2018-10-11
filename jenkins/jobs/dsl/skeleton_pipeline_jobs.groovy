
import pluggable.scm.*;
import adop.cartridge.properties.*;

SCMProvider scmProvider = SCMProviderHandler.getScmProvider("${SCM_PROVIDER_ID}", binding.variables)
CartridgeProperties cartridgeProperties = new CartridgeProperties("${CARTRIDGE_CUSTOM_PROPERTIES}");

// Folders
def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "${PROJECT_NAME}"
def projectScmNamespace = "${SCM_NAMESPACE}"

// Variables
// **The git repo variables can be changed to the users' git repositories when loading the cartridge and populating the CARTRIDGE_CUSTOM_PROPERTIES with "scm.code.repo.name" and "scm.test.repo.name" properties.
def skeletonAppgitRepo = cartridgeProperties.getProperty("scm.code.repo.name", "YOUR_APPLICATION_REPO");
def regressionTestGitRepo = cartridgeProperties.getProperty("scm.test.repo.name", "YOUR_REGRESSION_TEST_REPO");

// ** The logrotator variables should be changed to meet your build archive requirements
def logRotatorDaysToKeep = 7
def logRotatorBuildNumToKeep = 7
def logRotatorArtifactsNumDaysToKeep = 7
def logRotatorArtifactsNumToKeep = 7

// Jobs
def buildAppJob = freeStyleJob(projectFolderName + "/Skeleton_Application_Build")
def unitTestJob = freeStyleJob(projectFolderName + "/Skeleton_Application_Unit_Tests")
def codeAnalysisJob = freeStyleJob(projectFolderName + "/Skeleton_Application_Code_Analysis")
def deployJob = freeStyleJob(projectFolderName + "/Skeleton_Application_Deploy")
def regressionTestJob = freeStyleJob(projectFolderName + "/Skeleton_Application_Regression_Tests")

// Views
def pipelineView = buildPipelineView(projectFolderName + "/Skeleton_Application")

pipelineView.with{
    title('Skeleton Application Pipeline')
    displayedBuilds(5)
    selectedJob(projectFolderName + "/Skeleton_Application_Build")
    showPipelineParameters()
    showPipelineDefinitionHeader()
    refreshFrequency(5)
}

// All jobs are tied to build on the Jenkins slave
// The functional build steps for each job have been left empty
// A default set of wrappers have been used for each job
// New jobs can be introduced into the pipeline as required

buildAppJob.with{
  description("Skeleton application build job.")
  logRotator {
    daysToKeep(logRotatorDaysToKeep)
    numToKeep(logRotatorBuildNumToKeep)
    artifactDaysToKeep(logRotatorArtifactsNumDaysToKeep)
    artifactNumToKeep(logRotatorArtifactsNumToKeep)
  }
  scm scmProvider.get(projectScmNamespace, skeletonAppgitRepo, "*/master", "adop-jenkins-master", null)
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  label("docker")
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  triggers scmProvider.trigger(projectScmNamespace, skeletonAppgitRepo, "master")
  steps {
    shell('''## YOUR BUILD STEPS GO HERE'''.stripMargin())
  }
  publishers{
    downstreamParameterized{
      trigger(projectFolderName + "/Skeleton_Application_Unit_Tests"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          predefinedProp("B",'${BUILD_NUMBER}')
          predefinedProp("PARENT_BUILD", '${JOB_NAME}')
        }
      }
    }
  }
}

unitTestJob.with{
  description("This job runs unit tests on our skeleton application.")
  parameters{
    stringParam("B",'',"Parent build number")
    stringParam("PARENT_BUILD","Skeleton_Application_Build","Parent build name")
  }
  logRotator {
    daysToKeep(logRotatorDaysToKeep)
    numToKeep(logRotatorBuildNumToKeep)
    artifactDaysToKeep(logRotatorArtifactsNumDaysToKeep)
    artifactNumToKeep(logRotatorArtifactsNumToKeep)
  }
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  label("docker")
  steps {
  }
  steps {
    shell('''## YOUR UNIT TESTING STEPS GO HERE'''.stripMargin())
  }
  publishers{
    downstreamParameterized{
      trigger(projectFolderName + "/Skeleton_Application_Code_Analysis"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          predefinedProp("B",'${B}')
          predefinedProp("PARENT_BUILD",'${PARENT_BUILD}')
        }
      }
    }
  }
}

codeAnalysisJob.with{
  description("This job runs code quality analysis for our skeleton application using SonarQube.")
  parameters{
    stringParam("B",'',"Parent build number")
    stringParam("PARENT_BUILD","Skeleton_Application_Build","Parent build name")
  }
  logRotator {
    daysToKeep(logRotatorDaysToKeep)
    numToKeep(logRotatorBuildNumToKeep)
    artifactDaysToKeep(logRotatorArtifactsNumDaysToKeep)
    artifactNumToKeep(logRotatorArtifactsNumToKeep)
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  label("docker")
  steps {
    shell('''## YOUR CODE ANALYSIS STEPS GO HERE'''.stripMargin())
  }
  publishers{
    downstreamParameterized{
      trigger(projectFolderName + "/Skeleton_Application_Deploy"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          predefinedProp("B",'${B}')
          predefinedProp("PARENT_BUILD", '${PARENT_BUILD}')
        }
      }
    }
  }
}

deployJob.with{
  description("This job deploys the skeleton application to the CI environment")
  parameters{
    stringParam("B",'',"Parent build number")
    stringParam("PARENT_BUILD","Skeleton_Application_Build","Parent build name")
    stringParam("ENVIRONMENT_NAME","CI","Name of the environment.")
  }
  logRotator {
    daysToKeep(logRotatorDaysToKeep)
    numToKeep(logRotatorBuildNumToKeep)
    artifactDaysToKeep(logRotatorArtifactsNumDaysToKeep)
    artifactNumToKeep(logRotatorArtifactsNumToKeep)
  }
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  label("docker")
  steps {
    shell('''## YOUR DEPLOY STEPS GO HERE'''.stripMargin())
  }
  publishers{
    downstreamParameterized{
      trigger(projectFolderName + "/Skeleton_Application_Regression_Tests"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          predefinedProp("B",'${B}')
          predefinedProp("PARENT_BUILD", '${PARENT_BUILD}')
          predefinedProp("ENVIRONMENT_NAME", '${ENVIRONMENT_NAME}')
        }
      }
    }
  }
}

regressionTestJob.with{
  description("This job runs regression tests on the deployed skeleton application")
  parameters{
    stringParam("B",'',"Parent build number")
    stringParam("PARENT_BUILD","Skeleton_Application_Build","Parent build name")
    stringParam("ENVIRONMENT_NAME","CI","Name of the environment.")
  }
  logRotator {
    daysToKeep(logRotatorDaysToKeep)
    numToKeep(logRotatorBuildNumToKeep)
    artifactDaysToKeep(logRotatorArtifactsNumDaysToKeep)
    artifactNumToKeep(logRotatorArtifactsNumToKeep)
  }
  scm scmProvider.get(projectFolderName, regressionTestGitRepo, "*/master", "adop-jenkins-master", null)
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  label("docker")
  steps {
    shell('''## YOUR REGRESSION TESTING STEPS GO HERE'''.stripMargin())
  }
}
