pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "kmp"

include(":questionType")
include(":illegalCast")
include(":fastutil")
include(":openmap")
include(":rhizomedb")
include(":rhizomedb-test")
include(":serialization")
include(":shims")
