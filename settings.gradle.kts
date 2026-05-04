pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://cache-redirector.jetbrains.com/dl.google.com.android.maven2") // for Android Gradle Plugin
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://cache-redirector.jetbrains.com/dl.google.com.android.maven2") // for Android Gradle Plugin
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "kmp"

include(":android")
include(":download")
include(":fastutil")
include(":illegalCast")
include(":ios")
include(":kfc")
include(":openmap")
include(":questionType")
include(":rhizomedb")
include(":rhizomedb-test")
include(":serialization")
include(":shims")
include(":uid")
