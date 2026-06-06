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
        google()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ChangeCut"

include(":app")
include(":core")
include(":common")
include(":domain")
include(":data")
include(":feature-auth")
include(":feature-home")
include(":feature-editor")
include(":feature-export")
