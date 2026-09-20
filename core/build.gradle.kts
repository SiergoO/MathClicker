plugins {
    id("mathclicker.android.library")
    kotlin("kapt")
    // If there will be problems with that plugin delete it + sync + rebuild
}

android {
    namespace = "com.sdomashchuk.mathclicker.game"

    defaultConfig {
        // targetSdk is not a library defaultConfig member on the AGP 9 DSL; only applications declare it.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)

    //Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    kapt(libs.androidx.hilt.compiler)
}
