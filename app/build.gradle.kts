import com.android.build.api.dsl.AaptOptions
import com.android.build.gradle.internal.api.BaseVariantOutputImpl

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.mihuashi.paybyfinger"
    compileSdk = 34
    val buildTime = System.currentTimeMillis()
    defaultConfig {
        applicationId = "com.mihuashi.paybyfinger"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        dependenciesInfo.includeInApk = false
        ndk.abiFilters += arrayOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        buildConfigField("long", "BUILD_TIME", "$buildTime")
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
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
    }
    kotlin.jvmToolchain(21)
    applicationVariants.all {
        outputs.all {
            (this as BaseVariantOutputImpl).outputFileName = "Mhs Pay-$versionName-$versionCode-$name-$buildTime.apk"
        }
    }
    androidResources {
        // 设置额外的资源路径
        additionalParameters ("--allow-reserved-package-id", "--package-id", "0x65")
    }
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:8.3.4")
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.ezxhelper)
    compileOnly(libs.xposed)
    implementation(libs.xkt)
    implementation(libs.dsp)
    implementation(libs.androidx.biometric.ktx)
    implementation(libs.cardSlider)
    implementation(libs.modernandroidpreferences)
}