plugins {
    id("mathclicker.android.application")
    id("mathclicker.android.compose")
    kotlin("kapt")
    // If there will be problems with that plugin delete it + sync + rebuild
    alias(libs.plugins.hilt.android)
    id("kotlinx-serialization")
    id("kotlin-parcelize")
}

android {
    namespace = "com.sdomashchuk.mathclicker"

    defaultConfig {
        applicationId = "com.sdomashchuk.mathclicker"
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {

    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Navigation
    implementation(libs.accompanist.navigation.animation)

    // Splash screen
    implementation(libs.androidx.core.splashscreen)

    // Lottie
    implementation(libs.lottie.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.extensions)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Compose
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material)
    // Declared explicitly: transitive resolution only exposes this on the runtime classpath
    // (via other libraries' implementation-scoped edges), never on the compile classpath,
    // so statusBarsPadding/navigationBarsPadding are unresolved without it.
    implementation(libs.compose.foundation)
    implementation(libs.compose.foundation.layout)
    implementation(libs.compose.animation)
    implementation(libs.compose.animation.core)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.activity.compose)

    // Immutable collections
    implementation(libs.kotlinx.collections.immutable)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    kapt(libs.hilt.android.compiler)
    kapt(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.room.ktx)
    implementation(libs.room.runtime)
    kapt(libs.room.compiler)
    // Room's kapt stub reads Kotlin metadata with an older reader than the 2.4.0 compiler emits.
    kapt(libs.kotlin.metadata.jvm)
}
