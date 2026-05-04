import org.gradle.internal.declarativedsl.parsing.main

plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.multiplatform)
}

val generateBootstrapResources = tasks.register<Sync>("asd") {
    from(layout.buildDirectory.dir("../bootstrap"))
    into(project.layout.buildDirectory.dir("bootstrap"))
}
tasks.named("preBuild") {
    dependsOn(generateBootstrapResources)
}


android {
    namespace = "com.jetbrains.android.kmp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jetbrains.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    sourceSets {
        named("main") {
            resources.srcDirs(generateBootstrapResources.map { it.destinationDir })
        }
    }
    packaging {
        resources {

        }
    }
}


kotlin {
    androidTarget {

    }
    sourceSets {
        androidMain {
            resources.srcDirs(generateBootstrapResources.map { it.destinationDir })
        }
    }
}