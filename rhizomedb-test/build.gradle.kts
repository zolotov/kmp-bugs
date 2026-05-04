@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
    id("org.jetbrains.kotlinx.benchmark") version "0.4.14"
    kotlin("plugin.allopen") version "2.0.20"
}

kotlin {
    wasmJs {
        nodejs()
        browser()
        binaries.executable()
    }
    jvm()
    iosArm64()
    macosArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":rhizomedb"))
            implementation(libs.kotlinx.benchmark.runtime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

benchmark {
    targets {
        register("jvm")
        register("wasmJs")
        register("macosArm64")
    }
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}