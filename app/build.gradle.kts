@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val buildTime = System.currentTimeMillis()


android {
    namespace = "com.mihuashi.paybyfinger"
    compileSdk = 37
    val localProperties = Properties()
    if (rootProject.file("local.properties").canRead())
        localProperties.load(rootProject.file("local.properties").inputStream())


    defaultConfig {
        applicationId = "com.mihuashi.paybyfinger"
        minSdk = 28
        versionCode = 7
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        dependenciesInfo.includeInApk = false
        ndk.abiFilters += arrayOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64","armeabi","mips", "mips64")
        buildConfigField("long", "BUILD_TIME", "$buildTime")
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val config = localProperties.getProperty("androidStoreFile")?.let {
        signingConfigs.create("config") {
            storeFile = file(it)
            storePassword = localProperties.getProperty("androidStorePassword")
            keyAlias = localProperties.getProperty("androidKeyAlias")
            keyPassword = localProperties.getProperty("androidKeyPassword")
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        all {
            signingConfig = config ?: signingConfigs["debug"]
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "**"
        }
        dex {
            useLegacyPackaging = true
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
    androidResources {
        // 设置额外的资源路径
        additionalParameters ("--allow-reserved-package-id", "--package-id", "0x65")
    }
}

base {
    archivesName.set("Rice Painter Fingerprint Pay-${android.defaultConfig.versionName}${android.defaultConfig.versionNameSuffix ?: ""}-$buildTime")
}

dependencies {
    implementation(libs.shadow.gradle.plugin)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.database.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.ezxhelper.core)
    implementation(libs.ezxhelper.api82)
    implementation(libs.ezxhelper.android.utils)
    compileOnly(libs.xposed)
    implementation(libs.xkt)
    implementation(libs.dsp)
    implementation(libs.androidx.biometric.ktx)
    implementation(libs.cardSlider)
    implementation(libs.modernandroidpreferences)
    implementation(libs.hyperfocusapi)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.0")
    // 可选：添加 miuix-preference 以获取 Preference 组件
    implementation("top.yukonga.miuix.kmp:miuix-preference:0.9.0")
    // 可选：添加 miuix-icons 以获取更多图标
    implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.0")
    // 可选：添加 miuix-shapes 以获取平滑圆角
    implementation("top.yukonga.miuix.kmp:miuix-shapes:0.9.0")
    // 可选：添加 miuix-navigation3-ui 以获取 Navigation3 支持
    implementation("top.yukonga.miuix.kmp:miuix-navigation3-ui:0.9.0")
}