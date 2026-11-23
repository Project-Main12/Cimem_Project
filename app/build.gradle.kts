plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.digitalkalkan.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.digitalkalkan.app"
        minSdk = 29          // Android 10
        targetSdk = 36
        versionCode = 6
        versionName = "2.0.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    // Telefon (arm64-v8a) ve emülatör (x86_64) için ayrı, küçük APK'lar
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }
    androidResources {
        noCompress += "tflite"   // LiteRT modeli bellek eşlemeli okunabilsin
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Robolectric'in yeni JDK sürümlerinde çalışması için gerekli
            it.jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Veli–çocuk bağlantısı (Supabase) ve anlık bildirim (Firebase)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.functions)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Yerel olay kaydı (metin YOK), ekran OCR, BERT (LiteRT), ses tanıma (Vosk)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.litert)
    implementation(libs.vosk.android)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Ekran görüntüsü testi (telefon olmadan ana ekranı PNG olarak üretir)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
