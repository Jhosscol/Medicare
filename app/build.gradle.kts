plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"

    // ✅ IMPORTANTE: Aplicar DESPUÉS de android plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.medicare"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.medicare"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GEMINI_API_KEY", "\"${project.findProperty("GEMINI_API_KEY") ?: ""}\"")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            pickFirsts += "**/*.so"  // Para las librerías nativas duplicadas
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx.v291)
    implementation(libs.androidx.lifecycle.livedata.ktx.v291)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.androidx.foundation.android)
    implementation(libs.play.services.appindexing)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // TensorFlow Lite
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.metadata)
    implementation(libs.tensorflow.lite.gpu)

    // Permisos
    implementation(libs.easypermissions)

    // Imágenes
    implementation(libs.glide)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android.v173)
    implementation(libs.kotlinx.coroutines.play.services)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.text.recognition)

    // ✅ FIREBASE - LIMPIO Y ORDENADO
    // Firebase BoM (maneja versiones automáticamente)
    implementation(platform(libs.firebase.bom))

    // Firebase Services (SIN especificar versión, el BoM las maneja)

    implementation("com.google.firebase:firebase-analytics-ktx:22.1.2")
    implementation("com.google.firebase:firebase-auth-ktx:23.1.0")
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.1")

    // Google Sign-In
    implementation(libs.play.services.auth)

    // FirebaseUI Auth
    implementation("com.firebaseui:firebase-ui-auth:8.0.2")
}
