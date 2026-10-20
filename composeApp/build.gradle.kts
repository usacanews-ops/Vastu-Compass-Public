plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.materialIconsExtended) // Fixes all unresolved icon references
            implementation(compose.ui)
            implementation(compose.components.resources)

            // Serialization & Ktor
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            implementation("io.ktor:ktor-client-core:2.3.9")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.9")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.9")

            // Kamel Image Loader
            implementation("media.kamel:kamel-image:0.9.4")
        }

        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.8.2")
            implementation("io.ktor:ktor-client-okhttp:2.3.9")
        }
    }
}

android {
    namespace = "com.iws.vastucompass"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.iws.vastucompass"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            // This signs the release APK so Android allows installation:
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
