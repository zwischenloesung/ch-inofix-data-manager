# ch-inofix-data-manager

Ingest, manage and store data from web-enabled sensors.

## IMPORTANT NOTICE ##

ch-inofix-data-manager development targets Liferay CE 7.0.4-ga5. 

Make sure your liferay workspace environment is configured accordingly: 

In $LIFERAY_WORKSPACE/gradle.properties set: 

    liferay.workspace.bundle.url=https://cdn.lfrs.sl/releases.liferay.com/portal/7.0.4-ga5/liferay-ce-portal-tomcat-7.0-ga5-20171018150113838.zip

In $LIFERAY_WORKSPACE/settings.gradle set: 

    classpath group: "com.liferay", name: "com.liferay.gradle.plugins.workspace", version: "1.8.0"
    
(see the discussion at https://github.com/liferay/liferay-npm-build-tools/issues/56) 

## How To Build
1. Install blade: `curl https://raw.githubusercontent.com/liferay/liferay-blade-cli/master/installers/local | sh`
1. Create a liferay workspace: `WORKSPACE=my-workspace; blade init $WORKSPACE`
1. Checkout data-manager sources to the workspace's module directory: `cd $WORKSPACE/modules; git clone https://github.com/inofix/ch-inofix-data-manager.git`
1. Run ServiceBuilder: `gradle buildService`
1. Build: `gradle jar`

## How To Contribute
1. Fork this repository to your individual github account.
1. Clone your personal fork to your local machine.
1. Use feature branches to work on new features or known issues.
1. Merge finished features into your individual master branch and 
1. create pull-requests, to contribute your solutions to the inofix master branch.

**Stay up-to-date**

1. Change the current working directory to your local project.
1. Configure https://github.com/inofix/ch-inofix-data-manager as additional upstream remote (see: https://help.github.com/articles/configuring-a-remote-for-a-fork/)
1. Sync inofix-master with your individual fork (see: https://help.github.com/articles/syncing-a-fork/): 
1. Fetch upstream/master to your local copy: `git fetch upstream` 
1. Check out your fork's local 'master' branch: `git checkout master`
1. Merge the changes from 'upstream/master' into your local master branch. This brings your fork's master branch into sync with the upstream repository, without losing your local changes: `git merge upstream/master`
1. Push merged master to your individual github account and 
1. create a pull-request, to contribute your solution to the inofix master branch.

**Note for Eclipse / Liferay-IDE developers**

If the JSPs of the data-manager-web project aren't validated properly, 

- select the data-manager-web in the Project-Explorer view
- open the context menu with the right mouse key
- select "Configure" and
- choose "Add JSP Validation Support"
- select the data-manager-web in the Project-Explorer view
- press CTRL + F5 to refresh your project

You may have to restart Eclipse, too in order to have your JSPs validated.

## How To Test

TODO

