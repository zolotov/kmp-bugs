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
    jvm()
    iosArm64()
    macosArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":serialization"))
            implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.4.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")
        }
        all {
            languageSettings.optIn("kotlin.concurrent.atomics.ExperimentalAtomicApi")
        }
    }

}
