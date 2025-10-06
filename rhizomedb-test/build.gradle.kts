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

    sourceSets {
        commonMain.dependencies {
            implementation(project(":rhizomedb"))
            implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.14")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")
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
    }
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}