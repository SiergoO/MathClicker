plugins {
    id("mathbubbles.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // api, not implementation: Game/GameSessionHelper expose Field, Target and
            // OperationSign on their own public API, so callers need :core:model resolvable
            // at their own compile time, not just this module's.
            api(projects.core.model)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
