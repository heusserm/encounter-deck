plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    jvm()               // used by the Ktor backend
    androidTarget()     // used by the Android app
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework { baseName = "engine" }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.encounterdeck.engine"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
}
