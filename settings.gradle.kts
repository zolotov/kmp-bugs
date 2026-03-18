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

include(":download")
include(":fastutil")
include(":illegalCast")
include(":kfc")
include(":openmap")
include(":questionType")
include(":rhizomedb")
include(":rhizomedb-test")
include(":serialization")
include(":shims")
