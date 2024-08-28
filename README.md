# Requirements

Job DSL plugin is installed in jenkins

# Jenkins Pipelines
[Pipelines](/jenkins/README.md)

# EVNFM deploy site values file
[evnfm-deploy site values file](/evnfm-deploy/README.md)


# Update Copyright Year

To update the copyright year in the license header for the files in each repo:

1. Run 'mvn -Plicense license:remove' to remove the current license headers with the old year in the repo.
2. In the pom.xml file for each repo, edit the <license.year> property for the maven license plugin to the new year.
3. Run 'mvn -Plicense license:format' to add the license headers to the files again with the specified license year.

Note:

There is also a manual update to the 'eric-eo-evnfm-sol-agent repo':
* In the file 'eric-eo-evnfm-sol-agent\eric-eo-evnfm-nbi-server\src\main\resources\application.yaml', the year will need to be updated for the
'info.app.legal' variable.
