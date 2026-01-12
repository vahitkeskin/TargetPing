plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.vahitkeskin.targetping"
    compileSdk = 34 // Android 14 (veya 35)

    defaultConfig {
        applicationId = "com.vahitkeskin.targetping"
        minSdk = 26 // Android 8.0+ (Foreground Service ve Notification Channels için ideal minSdk)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Jetpack Compose için vektör çizim desteği
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Proguard aktif etmek isterseniz true yapın
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Modern Java versiyonu (Kotlin ile uyumlu)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Compose özelliğini aktif et
    buildFeatures {
        compose = true
    }

    // Compose Compiler versiyonunu Kotlin versiyonuna göre ayarla
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8" // Kotlin 1.9.22 ile uyumlu
    }

    // Hilt için paketleme ayarları
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // --- Core Android & Lifecycle ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // --- Jetpack Compose (UI) ---
    // BOM (Bill of Materials) sayesinde tüm compose kütüphaneleri uyumlu versiyonlarda çalışır
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Compose Navigasyon
    implementation(libs.androidx.navigation.compose)

    // --- Dependency Injection (Hilt) ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler) // KSP ile derleme (Hızlandırılmış)
    implementation(libs.androidx.hilt.navigation.compose) // ViewModel'leri Compose içinde hiltViewModel() ile çağırmak için

    // --- Local Database (Room) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx) // Coroutines desteği
    ksp(libs.androidx.room.compiler) // KSP Annotation Processor

    // --- Maps & Location (Harita ve Konum) ---
    implementation(libs.play.services.location) // GPS ve Konum servisleri
    implementation(libs.maps.compose) // Google Maps için resmi Compose kütüphanesi

    // --- Permissions (İzin Yönetimi) ---
    implementation(libs.accompanist.permissions) // Kolay izin yönetimi

    // --- Background Tasks (WorkManager) ---
    implementation(libs.androidx.work.runtime.ktx)

    // --- Testing (Opsiyonel ama önerilir) ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.material.icons.extended)

    implementation(libs.accompanist.systemuicontroller) // rememberSystemUiController için
    implementation(libs.kotlinx.serialization.json)     // @Serializable için

    // Lifecycle Service
    implementation(libs.androidx.lifecycle.service)

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)

    // Biometric Authentication
    implementation(libs.androidx.biometric)
}