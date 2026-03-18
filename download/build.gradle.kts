@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
}

kotlin {
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation("io.ktor:ktor-client-core:3.4.1")
            implementation("io.ktor:ktor-client-cio:3.4.1")
            implementation("io.ktor:ktor-client-logging:3.4.1")
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.bundles.wrappers)
            implementation(npm("fflate", "0.8.2"))
        }
    }

}