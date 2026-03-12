plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
    id("io.github.turansky.kfc.application") version "16.7.0"
    id("org.jetbrains.kotlin.plugin.js-plain-objects") version "2.3.10"

}

kotlin {
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(libs.bundles.wrappers)
        }
    }
}
