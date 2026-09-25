plugins {
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.sqldelight) apply false
}

// build-logic is an included build, so `./gradlew check` does not reach it on its own - the gate
// that governs every module was the one thing no gate governed (MC-28).
tasks.register("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":convention:check"))
}
