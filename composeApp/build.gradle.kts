plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    androidTarget()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain = maybeCreate("commonMain").apply {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.materialIconsExtended)
                implementation("media.kamel:kamel-image:0.9.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
                implementation("io.ktor:ktor-client-core:2.3.8")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.8")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.8")
            }
        }
        
        maybeCreate("androidMain").apply {
            dependencies {
                implementation("androidx.activity:activity-compose:1.8.2")
                implementation("androidx.appcompat:appcompat:1.6.1")
                implementation("io.ktor:ktor-client-okhttp:2.3.8")
            }
        }
        
        // Safely find or create iosMain and apply dependencies
        val iosMain = maybeCreate("iosMain").apply {
            dependsOn(commonMain)
            dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.8")
            }
        }
        
        // Manually link the specific iOS architectures to the shared iosMain
        maybeCreate("iosX64Main").dependsOn(iosMain)
        maybeCreate("iosArm64Main").dependsOn(iosMain)
        maybeCreate("iosSimulatorArm64Main").dependsOn(iosMain)
    }
}

android {
    namespace = "com.vastu.compass"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vastu.compass"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
