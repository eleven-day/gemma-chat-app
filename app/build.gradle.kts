plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}

android {
    namespace = "com.example.gemmachat" // Changed to = for consistency
    compileSdk = 34 // Changed to = for consistency

    defaultConfig {
        applicationId = "com.example.gemmachat" // Changed to = for consistency
        minSdk = 24 // Changed to = for consistency
        targetSdk = 34 // Changed to = for consistency
        versionCode = 1 // Changed to = for consistency
        versionName = "1.0" // Changed to = for consistency

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" // Changed to = for consistency
        vectorDrawables {
            useSupportLibrary = true // Changed to = for consistency
        }
    }

    buildFeatures {
        compose = true // Changed to = for consistency
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1" // Changed to = for consistency
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Changed to isMinifyEnabled for clarity
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            ) // Changed to use () for clarity
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17 // Changed to = for consistency
        targetCompatibility = JavaVersion.VERSION_17 // Changed to = for consistency
    }

    kotlinOptions {
        jvmTarget = "17" // Changed to = for consistency
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}" // Changed to += for consistency
        }
    }
}

dependencies {
    // Android Core Libraries
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Compose UI
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // ViewModel + Compose Integration
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // MediaPipe LLM Inference
    implementation("com.google.mediapipe:tasks-genai:0.10.22")

    // Networking and Asynchronous Operations
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Testing Libraries
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}