plugins {
    id("mathbubbles.kmp.library")
    id("mathbubbles.roborazzi")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

// This module has no components and no DI, so mathbubbles.kmp.feature (Decompose/Essenty, Koin)
// would hand it dependencies it never uses — Compose is declared directly instead.
compose {
    resources {
        packageOfResClass = "com.sdamashchuk.mathbubbles.core.ui.resources"
    }
}

kotlin {
    androidLibrary {
        // Off by default for KMP android libraries (unlike plain android libraries); this module
        // now ships the MC-56 sound samples (res/raw) alongside its existing Compose resources.
        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.ui)
            implementation(libs.compose.material)
            implementation(libs.compose.foundation)
            implementation(libs.compose.foundation.layout)
            implementation(libs.compose.components.resources)
        }
        // SoundPool, SharedPreferences and AudioManager have no Compose Multiplatform or KMP
        // equivalent, so the sound player lives in androidMain rather than behind an expect/actual
        // with no honest iOS side yet (see the MC-6 KMP migration direction in CLAUDE.md).
        androidMain.dependencies {
            implementation(libs.koin.core)
        }
        androidHostTest.dependencies {
            implementation(libs.junit)
            implementation(libs.roborazzi)
            implementation(libs.roborazzi.compose)
            implementation(libs.robolectric)
            implementation(libs.compose.ui.test.junit4)
            implementation(libs.androidx.compose.ui.test.manifest)
        }
    }
}
