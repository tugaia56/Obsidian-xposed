import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Load keystore credentials from project-root keystore.properties (gitignored)
val keystoreProps = Properties()
val keystoreFile = rootProject.file("keystore.properties")
if (keystoreFile.exists()) keystoreProps.load(keystoreFile.inputStream())

android {
    namespace   = "it.tugaia56.obsidian"
    compileSdk  = 34
    defaultConfig {
        applicationId  = "it.tugaia56.obsidian"
        minSdk         = 31
        targetSdk      = 34
        versionCode    = 1
        versionName    = "1.0.0-test"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { viewBinding = true; buildConfig = true }

    signingConfigs {
        create("release") {
            if (keystoreFile.exists()) {
                storeFile     = file(keystoreProps["storeFile"]     as String)
                storePassword = keystoreProps["storePassword"]      as String
                keyAlias      = keystoreProps["keyAlias"]           as String
                keyPassword   = keystoreProps["keyPassword"]        as String
            }
        }
    }

    buildTypes {
        release {
            signingConfig  = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
dependencies {
    // Xposed API — local jar only, not from Maven
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(libs.android.appcompat)
    implementation(libs.android.material)
    implementation(libs.android.recyclerview)
    implementation(libs.eventbus)
    implementation(libs.colorpicker)
    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
    implementation(libs.remotepreferences)
    implementation(libs.android.documentfile)
}
