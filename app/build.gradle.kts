plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.ofarafal.enklawa"
    compileSdk = 35

    // CI podaje numer builda jako versionCode (-PappVersionCode=N); lokalnie 1.
    // Aktualizator porównuje ten numer z tagiem wydania "v<N>".
    val appVersionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1

    defaultConfig {
        applicationId = "dev.ofarafal.enklawa"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = "0.1.$appVersionCode"
        // Tor (libtor.so) jest duży; telefon docelowy (S21) to arm64.
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    // Stały klucz z sekretów CI. Bez niego (build lokalny) release podpisuje się kluczem debug,
    // a taki APK nie zaktualizuje się nad wersją z GitHuba (inny podpis).
    val keystorePath: String? = System.getenv("KEYSTORE_FILE")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (keystorePath != null) signingConfigs.getByName("release")
            else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("info.guardianproject:tor-android:0.4.9.5")
    implementation("info.guardianproject:jtorctl:0.4.5.7")

    testImplementation("junit:junit:4.13.2")
    // android.jar w testach JVM ma tylko atrapy org.json; prawdziwa implementacja do testu parsera wydań.
    testImplementation("org.json:json:20240303")
}
