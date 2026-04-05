pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // PREFER_PROJECT allows the Kotlin/WasmJs plugin to add its own distribution
    // repos (Node.js, Yarn, Binaryen) without manual Ivy configuration
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NeoMud"

include(":shared")
include(":server")
include(":client")
