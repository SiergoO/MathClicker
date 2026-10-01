plugins {
    `kotlin-dsl`
    // Applied directly, not through mathbubbles.quality: that plugin lives in this very module,
    // so applying it here would ask the build to compile itself before it can be configured.
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
}

group = "com.sdamashchuk.mathbubbles.buildlogic"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // compileOnly: the real plugins are provided by the consuming build's classpath
    // (declared `apply false` in the root build). We only need the DSL types to compile.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
    compileOnly(libs.roborazzi.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "mathbubbles.android.library"
            implementationClass = "com.sdamashchuk.mathbubbles.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "mathbubbles.android.application"
            implementationClass = "com.sdamashchuk.mathbubbles.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidCompose") {
            id = "mathbubbles.android.compose"
            implementationClass = "com.sdamashchuk.mathbubbles.buildlogic.AndroidComposeConventionPlugin"
        }
        register("quality") {
            id = "mathbubbles.quality"
            implementationClass = "com.sdamashchuk.mathbubbles.buildlogic.QualityConventionPlugin"
        }
        register("kmpLibrary") {
            id = "mathbubbles.kmp.library"
            implementationClass = "com.sdamashchuk.mathbubbles.buildlogic.KmpLibraryConventionPlugin"
        }
        register("kmpFeature") {
            id = "mathbubbles.kmp.feature"
            implementationClass = "com.sdamashchuk.mathbubbles.buildlogic.KmpFeatureConventionPlugin"
        }
        register("roborazzi") {
            id = "mathbubbles.roborazzi"
            implementationClass = "com.sdamashchuk.mathbubbles.buildlogic.RoborazziConventionPlugin"
        }
    }
}

detekt {
    source.setFrom(files("src/main/kotlin"))
    config.setFrom(files("../../config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    parallel = true
    failOnSeverity = dev.detekt.gradle.extensions.FailOnSeverity.Warning
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }
}
