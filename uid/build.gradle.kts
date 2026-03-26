@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
}

kotlin {
    compilerOptions.freeCompilerArgs = listOf(
        "-Xjvm-default=all",
        "-Xlambdas=class",
        "-Xconsistent-data-class-copy-visibility",
        "-Xcontext-parameters",
        "-XXLanguage:+AllowEagerSupertypeAccessibilityChecks",
        "-progressive",
    )

    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }

}