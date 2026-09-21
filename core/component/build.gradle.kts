plugins {
    id("mathclicker.kmp.library")
}

kotlin {
    sourceSets {
        // api, not implementation: ComponentViewModel exposes InstanceKeeper.Instance and a
        // protected CoroutineScope, and viewModel() is inline/reified over ComponentContext and
        // inlines a call to instanceKeeper.getOrCreate — every caller needs all three resolvable
        // at its own compile time, not just this module's.
        commonMain.dependencies {
            api(libs.decompose)
            api(libs.essenty.instance.keeper)
            api(libs.kotlinx.coroutines.core)
        }
    }
}
