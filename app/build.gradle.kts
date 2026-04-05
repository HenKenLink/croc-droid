import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.henkenlink.crocdroid"
    compileSdk = 36
    ndkVersion = "26.3.11579264"

    defaultConfig {
        applicationId = "com.henkenlink.crocdroid"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val props = Properties()
                keystorePropertiesFile.inputStream().use { props.load(it) }
                val storeFileProp = props.getProperty("RELEASE_STORE_FILE")
                if (storeFileProp != null && storeFileProp.isNotEmpty()) {
                    storeFile = file(storeFileProp)
                    storePassword = props.getProperty("RELEASE_STORE_PASSWORD") ?: ""
                    keyAlias = props.getProperty("RELEASE_KEY_ALIAS") ?: ""
                    keyPassword = props.getProperty("RELEASE_KEY_PASSWORD") ?: ""
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            ndk {
                abiFilters.add("arm64-v8a")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM — manages all Compose library versions
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)

    // Lifecycle
    implementation(libs.bundles.lifecycle)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DocumentFile (Scoped Storage)
    implementation(libs.androidx.documentfile)

    // Compose Tooling (debug only)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Testing
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Go Bridge
    implementation(files("libs/crocbridge.aar"))
}
