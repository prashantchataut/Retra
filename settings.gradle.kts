
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Retra"
include(":app")
include(":core:model")
include(":core:rom")
include(":emulation:api")

include(":core:emulation")
include(":core:patching")
include(":core:cheats")
include(":core:download")
include(":core:catalog")

include(":emulation:native")

include(":core:achievements")
include(":core:social")
include(":core:multiplayer")
