plugins {
    `kotlin-dsl`
    // Applied directly, not through matharcade.quality: that plugin lives in this very module,
    // so applying it here would ask the build to compile itself before it can be configured.
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
}

group = "com.sdamashchuk.matharcade.buildlogic"

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
            id = "matharcade.android.library"
            implementationClass = "com.sdamashchuk.matharcade.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "matharcade.android.application"
            implementationClass = "com.sdamashchuk.matharcade.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidCompose") {
            id = "matharcade.android.compose"
            implementationClass = "com.sdamashchuk.matharcade.buildlogic.AndroidComposeConventionPlugin"
        }
        register("quality") {
            id = "matharcade.quality"
            implementationClass = "com.sdamashchuk.matharcade.buildlogic.QualityConventionPlugin"
        }
        register("kmpLibrary") {
            id = "matharcade.kmp.library"
            implementationClass = "com.sdamashchuk.matharcade.buildlogic.KmpLibraryConventionPlugin"
        }
        register("kmpFeature") {
            id = "matharcade.kmp.feature"
            implementationClass = "com.sdamashchuk.matharcade.buildlogic.KmpFeatureConventionPlugin"
        }
        register("roborazzi") {
            id = "matharcade.roborazzi"
            implementationClass = "com.sdamashchuk.matharcade.buildlogic.RoborazziConventionPlugin"
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
