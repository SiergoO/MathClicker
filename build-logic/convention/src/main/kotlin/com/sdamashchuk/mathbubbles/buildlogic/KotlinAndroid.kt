package com.sdamashchuk.mathbubbles.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Shared Android + Kotlin setup for every module: compileSdk/minSdk, JVM 17 toolchain, and a
 * declared lint baseline path (MC-4.3 wires baseline generation and the `check` gate).
 */
internal fun Project.configureKotlinAndroid(extension: CommonExtension) {
    extension.compileSdk = SdkVersions.COMPILE
    extension.defaultConfig.minSdk = SdkVersions.MIN
    extension.compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    extension.compileOptions.targetCompatibility = JavaVersion.VERSION_17
    extension.lint.baseline = file("lint-baseline.xml")

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
