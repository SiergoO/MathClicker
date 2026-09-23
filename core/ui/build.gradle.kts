plugins {
    id("mathclicker.kmp.library")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

// This module has no components and no DI, so mathclicker.kmp.feature (Decompose/Essenty, Koin)
// would hand it dependencies it never uses — Compose is declared directly instead.
compose {
    resources {
        packageOfResClass = "com.sdamashchuk.matharcade.core.ui.resources"
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.ui)
            implementation(libs.compose.material)
            implementation(libs.compose.foundation)
            implementation(libs.compose.foundation.layout)
            implementation(libs.compose.components.resources)
        }
    }
}
