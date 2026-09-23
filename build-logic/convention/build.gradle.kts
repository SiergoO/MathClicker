plugins {
    `kotlin-dsl`
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
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "mathclicker.android.library"
            implementationClass = "com.sdamashchuk.matharcade.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "mathclicker.android.application"
            implementationClass = "com.sdamashchuk.matharcade.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidCompose") {
            id = "mathclicker.android.compose"
            implementationClass = "com.sdamashchuk.matharcade.buildlogic.AndroidComposeConventionPlugin"
        }
        register("quality") {
            id = "mathclicker.quality"
            implementationClass = "com.sdamashchuk.matharcade.buildlogic.QualityConventionPlugin"
        }
        register("kmpLibrary") {
            id = "mathclicker.kmp.library"
            implementationClass = "com.sdamashchuk.matharcade.buildlogic.KmpLibraryConventionPlugin"
        }
        register("kmpFeature") {
            id = "mathclicker.kmp.feature"
            implementationClass = "com.sdamashchuk.matharcade.buildlogic.KmpFeatureConventionPlugin"
        }
    }
}
