plugins {
    id("mathclicker.android.application")
    id("mathclicker.android.compose")
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlin.serialization)
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

sqldelight {
    databases {
        create("MathClickerDatabase") {
            packageName.set("com.sdomashchuk.mathclicker.data.database.local")
        }
    }
}

dependencies {

    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Navigation
    implementation(libs.decompose)
    implementation(libs.decompose.compose)
    implementation(libs.essenty.lifecycle)
    implementation(libs.essenty.instance.keeper)
    implementation(libs.kotlinx.serialization.core)

    // Splash screen
    implementation(libs.androidx.core.splashscreen)

    // Compottie
    implementation(libs.compottie)
    implementation(libs.compottie.dot)

    // Lifecycle
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.extensions)

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

    // Koin
    implementation(libs.koin.android)
    testImplementation(libs.koin.test)

    // SQLDelight
    implementation(libs.sqldelight.android.driver)
    testImplementation(libs.sqldelight.sqlite.driver)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
