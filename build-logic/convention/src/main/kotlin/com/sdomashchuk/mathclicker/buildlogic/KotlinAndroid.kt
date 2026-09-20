package com.sdomashchuk.mathclicker.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Shared Android + Kotlin setup for every module: compileSdk/minSdk, JVM 17 toolchain, and a
 * declared lint baseline path (MC-4.3 wires baseline generation and the `check` gate).
 *
 * Uses `lintOptions`, not the newer `lint` property: `com.android.build.api.dsl.Lint` is
 * referenced by `CommonExtension.getLint()` but its class is absent from the published AGP
 * 9.2.1 jars, so resolving `.lint` fails to compile. `lintOptions` is the same feature under
 * its still-shipped deprecated name.
 */
internal fun Project.configureKotlinAndroid(extension: CommonExtension) {
    extension.compileSdk = 36
    extension.defaultConfig.minSdk = 24
    extension.compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    extension.compileOptions.targetCompatibility = JavaVersion.VERSION_17
    extension.lintOptions.baseline(file("lint-baseline.xml"))

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
