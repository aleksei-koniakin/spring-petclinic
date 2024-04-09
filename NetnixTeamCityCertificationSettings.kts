import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.projectFeatures.githubIssues
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2023.11"

project {

    vcsRoot(HttpsGithubComFvillaNetnixTeamcityCertificationPetclinicRefsHeadsMain)

    buildType(Deploy)
    buildType(Build)

    features {
        githubIssues {
            id = "PROJECT_EXT_5"
            displayName = "fvilla-netnix/teamcity-certification-petclinic"
            repositoryURL = "https://github.com/fvilla-netnix/teamcity-certification-petclinic"
            authType = accessToken {
                accessToken = "credentialsJSON:bb34dbec-c51c-4df5-9051-0c0873d58213"
            }
            param("tokenId", "")
        }
    }
}

object Build : BuildType({
    name = "Build"

    artifactRules = "+:target"

    vcs {
        root(HttpsGithubComFvillaNetnixTeamcityCertificationPetclinicRefsHeadsMain)
    }

    steps {
        maven {
            name = "Maven Build"
            id = "Maven_Build"
            goals = "package"
        }
    }

    triggers {
        vcs {
        }
    }

    features {
        perfmon {
        }
        commitStatusPublisher {
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = vcsRoot()
            }
        }
        notifications {
            notifierSettings = emailNotifier {
                email = "fvilla@netnix.net"
            }
            branchFilter = "+:main"
            buildFailed = true
            firstFailureAfterSuccess = true
            buildFinishedSuccessfully = true
            buildProbablyHanging = true
        }
        pullRequests {
            provider = github {
                authType = vcsRoot()
                filterAuthorRole = PullRequests.GitHubRoleFilter.MEMBER
            }
        }
    }
})

object Deploy : BuildType({
    name = "Deploy"

    params {
        password("GCLOUD_KEY", "credentialsJSON:dd0f3b7c-d5d9-456c-9784-862cc3102e44")
    }

    steps {
        script {
            name = "Deploy to GCP"
            id = "Deploy"
            scriptContent = """
                (echo %GCLOUD_KEY% | base64 -d) > gcloud-key.json;
                gcloud auth activate-service-account --key-file=gcloud-key.json;
                echo "runtime: java17\ninstance_class: F1" > app.yaml;
                gcloud --project=teamcity-certification app deploy;
            """.trimIndent()
            dockerImage = "gcr.io/google.com/cloudsdktool/google-cloud-cli:latest"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${Build.id}"
            successfulOnly = true
            branchFilter = "+:main"
        }
    }

    features {
        perfmon {
        }
    }

    dependencies {
        dependency(Build) {
            snapshot {
            }

            artifacts {
                artifactRules = "spring-petclinic-3.2.0-SNAPSHOT.jar"
            }
        }
    }
})

object HttpsGithubComFvillaNetnixTeamcityCertificationPetclinicRefsHeadsMain : GitVcsRoot({
    name = "https://github.com/fvilla-netnix/teamcity-certification-petclinic#refs/heads/main"
    url = "https://github.com/fvilla-netnix/teamcity-certification-petclinic"
    branch = "refs/heads/main"
    branchSpec = "refs/heads/main"
    authMethod = password {
        userName = "fvilla-netnix"
        password = "credentialsJSON:faa43394-a0e6-4f77-8959-c424830ddb0c"
    }
})
