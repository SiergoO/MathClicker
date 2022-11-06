plugins {
    kotlin("android")
    kotlin("kapt")
    id("com.android.application")
    // If there will be problems with that plugin delete it + sync + rebuild
    id("dagger.hilt.android.plugin")
    id("kotlinx-serialization")
    id("kotlin-parcelize")
}

android {
    namespace = "com.sdomashchuk.mathclicker"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.sdomashchuk.mathclicker"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

        kotlinOptions {
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
        }
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildToolsVersion = "33.0.0"
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.1.1"
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {

    implementation(project(":domain"))
    implementation(project(":data"))

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("androidx.activity:activity-compose:1.6.1")

    //Navigation
    implementation("com.google.accompanist:accompanist-navigation-animation:0.27.0")

    // Splash screen
    implementation("androidx.core:core-splashscreen:1.0.0")

    //Lottie
    implementation("com.airbnb.android:lottie-compose:5.2.0")

    //Lifecycle
    implementation("androidx.lifecycle:lifecycle-service:2.5.1")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")

    //Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

    //ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")

    //Compose
    implementation("androidx.compose.ui:ui:1.3.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.3.0")
    implementation("androidx.compose.material:material:1.3.0")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")

    //Hilt
    implementation("com.google.dagger:hilt-android-gradle-plugin:2.41")
    implementation("com.google.dagger:hilt-android:2.40")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")
    kapt("com.google.dagger:hilt-android-compiler:2.40")
    kapt("androidx.hilt:hilt-compiler:1.0.0")
}