plugins {
    id("mathclicker.kmp.feature")
}

kotlin {
    androidLibrary {
        // Off by default for KMP android libraries (unlike plain android libraries); this module
        // ships the game's raw/values resources (lottie assets, strings) that :app used to own.
        androidResources {
            enable = true
        }
    }

    sourceSets {
        // Every composable here reads BackHandler, LocalResources or androidx.lifecycle — real
        // Android Compose APIs with no Compose Multiplatform equivalent today, so the whole
        // feature lives in androidMain rather than behind an expect/actual with no honest iOS side.
        androidMain.dependencies {
            implementation(projects.core.model)
            implementation(projects.core.game)
            implementation(projects.core.database)
            implementation(projects.core.ui)
            implementation(projects.core.component)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.compose.animation.core)
            implementation(libs.compottie)
            implementation(libs.compottie.dot)
            implementation(libs.kotlinx.collections.immutable)
        }
        androidHostTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
