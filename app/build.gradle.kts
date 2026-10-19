plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "be.launcher.niagara"
    compileSdk = 34

    defaultConfig {
        applicationId = "be.launcher.niagara"
        minSdk = 26
        targetSdk = 34
        // In CI telt het versienummer mee met de buildnummer, zodat je in de app ziet welke build je hebt
        val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = buildNumber
        versionName = "0.2.$buildNumber"
    }

    // In CI komt de ondertekeningssleutel uit de cache (zie .github/workflows/build.yml).
    // Lokaal valt het terug op de standaard debug-sleutel van Android Studio.
    val ciKeystore = System.getenv("LAUNCHER_KEYSTORE")
    if (ciKeystore != null && file(ciKeystore).exists()) {
        signingConfigs {
            create("launcher") {
                storeFile = file(ciKeystore)
                storePassword = "android"
                keyAlias = "launcher"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            signingConfigs.findByName("launcher")?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = false
            // Debug-sleutel zodat de release-APK direct installeerbaar is
            signingConfig = signingConfigs.getByName("debug")
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.core:core-ktx:1.13.1")
}
