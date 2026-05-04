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
    iosArm64()
    macosArm64()

    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.4.0")
            api(project(":fastutil"))
            implementation(project(":openmap"))
            implementation(project(":shims"))
            implementation(project(":serialization"))
            implementation(libs.jetbrains.annotations)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.core)
        }
        all {
            languageSettings.enableLanguageFeature("ContextParameters")
            languageSettings.optIn("kotlin.concurrent.atomics.ExperimentalAtomicApi")
        }
    }
}