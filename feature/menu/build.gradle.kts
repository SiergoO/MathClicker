plugins {
    id("mathclicker.kmp.feature")
}

kotlin {
    androidLibrary {
        // Off by default for KMP android libraries (unlike plain android libraries); this module
        // ships the logo and how-to-play resources (drawables, strings) that :app used to own.
        androidResources {
            enable = true
        }
    }

    sourceSets {
        // MenuScreen builds its layout with androidx.constraintlayout.compose, which has no
        // Compose Multiplatform equivalent today, so the feature lives in androidMain rather
        // than behind an expect/actual with no honest iOS side.
        androidMain.dependencies {
            implementation(projects.core.ui)
            implementation(projects.core.component)
            implementation(libs.androidx.constraintlayout.compose)
        }
    }
}
