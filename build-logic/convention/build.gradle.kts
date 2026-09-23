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
    }
}
