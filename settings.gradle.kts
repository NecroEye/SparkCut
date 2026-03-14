pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SparkCut"
include(":app")
include(":core")
include(":feature")
include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:data")
include(":core:database")
include(":core:media")
include(":core:templateengine")
include(":core:ads")
include(":feature:onboarding")
include(":feature:home")
include(":feature:template")
include(":feature:create")
include(":feature:editor")
include(":feature:projects")
include(":feature:settings")
include(":feature:export")
