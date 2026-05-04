plugins {
    alias(libs.plugins.multiplatform)
}

kotlin {
    iosArm64()
    macosArm64 {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    sourceSets {
        nativeMain.dependencies {
            implementation(libs.jetbrains.annotations)
        }
    }
}