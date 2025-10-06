@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
}

kotlin {
    compilerOptions.freeCompilerArgs.add("-Xwasm-kclass-fqn")

    wasmJs {
        browser()
        binaries.executable()
    }
    jvm()

    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.4.0")
            api(project(":fastutil"))
            implementation(project(":openmap"))
            implementation(project(":shims"))
            implementation(project(":serialization"))
            implementation("org.jetbrains:annotations:26.0.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")
        }
        all {
            languageSettings.enableLanguageFeature("ContextParameters")
            languageSettings.optIn("kotlin.concurrent.atomics.ExperimentalAtomicApi")
        }
    }
}