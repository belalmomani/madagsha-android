plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
// Firebase يُفعَّل فقط عند وجود google-services.json (لا نُنشئ ملفًا مزيفًا)
if (file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")

android {
    namespace = "discussion.com.paper"
    compileSdk = 35
    defaultConfig {
        applicationId = "discussion.com.paper"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "STARTIO_APP_ID", "\"${providers.gradleProperty("STARTIO_APP_ID").orElse("").get()}\"")
    }
    signingConfigs {
        // مفتاح ثابت من متغيرات البيئة / GitHub Secrets. لا يوجد أي مفتاح داخل المستودع.
        val ks = System.getenv("KEYSTORE_PATH")
        if (!ks.isNullOrBlank() && file(ks).exists()) {
            create("release") {
                storeFile = file(ks)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(project(":core"))
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.startapp:inapp-sdk:${providers.gradleProperty("STARTIO_SDK_VERSION").orElse("5.+").get()}")
}
