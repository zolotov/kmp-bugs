@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.multiplatform)
}

kotlin {
    wasmJs {
        browser()
        binaries.executable()
    }
    jvm()
    iosArm64()
    macosArm64()
}