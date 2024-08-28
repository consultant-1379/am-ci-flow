# POINT FIX

## What is a Point fix?

Different customers will be on different versions of our software.

Up until now the way of working has been fix the bug on the latest software and tell the customer to upgrade to that version, "fix forward"

Some customers will refuse to upgrade. They want the fix applied to the version of our software that they currently have deployed.

This means going back to the old version of our software and fixing the bug there, then shipping that fix to the customer.

## branch name

The branch name will be the sprint number, for example if the bug was introduced in 20.13 the branch name should be 20.13

## versioning

### code & microservice repositories

#### pre 20.17

There is no proper versioning of these repositories prior to 20.17.
The PATCH number is all that's being updated.

We need to set a build number in the version, i.e. -2 for the first point fix, and increment the build number for every point fix after that.

Example: bug is on version 0.0.156, point fix is on version 0.0.156-2, docker image, and 0.0.156+2, helm chart

#### post 20.17

From 20.17 onward we are applying a new versioning of these repositories.
The minor number of the version (second digit) will be incremented each time functionality is added to the repository.

For a point fix the PATCH number of that release will be updated.

Example: bug is on version 0.1.0, point fix is on version 0.1.1

### integration chart repository

The integration chart repository updates the minor version every sprint so for a point fix the PATCH number will be updated.

Example: bug is on version 2.13.0, point fix is on version 2.13.1

## find commit to branch from

Work backwards from the EO integration chart repository. The EO integration chart version will be known.

* Clone the eo-integration-charts repository and checkout that tag.
* From the requirements.yaml file see what version of EVNFM is there.
* Clone the am-integration-charts repository and search the git log for this version.
```bash

git log -S "<EVNFM version>"

```
* Checkout this commit and see what version of the microservice is in the requirements.yaml
* Clone the microservice repository and search the git log for this version
```bash

git log -S "<microservice version>"

```
* Checkout this commit

## CI Steps

### Git Submodule setup

When you clone a repository the submodules are not cloned by default, or you may have updated the submodule version locally.
Execute the following command to update the submodules to the appropriate commit:

```bash

git submodule update --init --recursive

```

### microservice repository

* branch in microservice
```bash

git checkout -b <branch_name>

```
Please refer to the section about *branch_name* for what branch_name to use

* update version to point fix snapshot & commit
```bash

# with a hyphen, please see versioning section
mvn versions:set -DnewVersion=<new_version>-SNAPSHOT -DgenerateBackupPoms=false
git add
git commit -m "<JIRA ID> <Message>"
git push origin <branch_name>

```

**Implement Fix**

* commit point fix changes
```bash

git add
git commit -m "<JIRA ID> <Message>"

```
* push fix for review

It is going to a different branch, so the git command will be slightly different.

```bash

git push origin HEAD:refs/for/<branch_name>

```

Replace <branch_name> with the branch name

**Once code has been reviewed and approved**

* update version to point fix full version & commit
```bash

# with a hyphen, please see versioning section
mvn versions:set -DnewVersion=<new_version> -DgenerateBackupPoms=false

```
* set project name and version for bob & commit
```bash

./am-ci-flow/bob/bob -r am-ci-flow/rulesets2/project_controls.yaml bob_project_name
./am-ci-flow/bob/bob -r am-ci-flow/rulesets2/project_controls.yaml bob_release_version
git add
git commit -m "<JIRA ID> <Message>"

```
* tag the commit with the version of the repository
```bash

git tag -a $(cat .project_version) -m "Tagging point fix $(cat .project_version)"

```
* push branch
```bash

git push origin <branch_name>
```
* build project
```

mvn clean install

```
* build image
```bash

docker build . --tag armdocker.rnd.ericsson.se/proj-am/releases/$(cat .project_name):$(cat .project_version)

```
* upload image to armdocker
```bash

docker push armdocker.rnd.ericsson.se/proj-am/releases/$(cat .project_name):$(cat .project_version)

```
* update chart
```bash

sed -i s/VERSION/$(cat .project_version)/g charts/*/values.yaml

```

* change to PRA version for chart
```bash

# with a plus, please see versioning section
mvn versions:set -DnewVersion=<new_version> -DgenerateBackupPoms=false

```
* set project name and version for bob
```bash

./am-ci-flow/bob/bob -r am-ci-flow/rulesets2/project_controls.yaml bob_project_name
./am-ci-flow/bob/bob -r am-ci-flow/rulesets2/project_controls.yaml bob_release_version
```

**WARNING:** Do not commit the version changes above back to the branch!

* build chart
```bash

helm package --version $(cat .project_version) charts/<repository_specific_folder_name>

```
* upload chart to artifactory
```bash

# run the container
docker run -it --init --user $(id -u):$(id -g) --rm -v ${PWD}:${PWD} --workdir ${PWD} armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-adp-release-auto:latest bash

```

### code repository

A code repository here is a repository which doesn't create an image or chart.
Two examples are am-shared-utilities and master.

This repository has similar steps to the microservice repository.
Replace build & push image and chart with deploy artifacts.

```bash

mvn clean deploy -DskipTests

```
### integration chart repository

* branch in EVNFM integration chart
```bash

git checkout -b <branch_name>

```
Please refer to the section about *branch_name* for what branch_name to use

* update version to point fix snapshot & commit
```bash

# with a hyphen, please see versioning section
mvn versions:set -DnewVersion=<new_version>-SNAPSHOT -DgenerateBackupPoms=false
git add
git commit -m "<JIRA ID> <Message>"

```
* update version of microservice & commit

open the requirements.yaml file in /charts/eric-eo-evnfm , update it accordingly and save.

```bash

git add
git commit -m "<JIRA ID> <Message>"

```

* update version to point fix full version & commit
```bash

# with a hyphen, please see versioning section
mvn versions:set -DnewVersion=<new_version> -DgenerateBackupPoms=false

```
* set project name and version for bob & commit
```bash

./am-ci-flow/bob/bob -r am-ci-flow/rulesets2/project_controls.yaml bob_project_name
./am-ci-flow/bob/bob -r am-ci-flow/rulesets2/project_controls.yaml bob_release_version
git add
git commit -m "<JIRA ID> <Message>"

```
* tag the commit with the version of the repository
```bash

git tag -a $(cat .project_version) -m "Tagging point fix $(cat .project_version)"

```
* push branch
```bash

git push origin <branch_name>

```
* change to PRA version for chart
```bash

# with a plus, please see versioning section
mvn versions:set -DnewVersion=<new_version> -DgenerateBackupPoms=false

```
* set project name and version for bob
```bash

./am-ci-flow/bob/bob -r am-ci-flow/rulesets2/project_controls.yaml bob_project_name
./am-ci-flow/bob/bob -r am-ci-flow/rulesets2/project_controls.yaml bob_release_version
```

**WARNING:** Do not commit the version changes above back to the branch!

* build chart
```bash
# clear out the charts folder
rm -rf charts/eric-eo-evnfm/charts/*


helm dependency update charts/eric-eo-evnfm

sh 'am-ci-flow/bob/bob -r am-ci-flow/rulesets2/integration_helm_chart.yaml helm-package'

```
* upload chart to artifactory
```bash

sh 'am-ci-flow/bob/bob -r am-ci-flow/rulesets2/integration_helm_chart.yaml arm-upload'

```