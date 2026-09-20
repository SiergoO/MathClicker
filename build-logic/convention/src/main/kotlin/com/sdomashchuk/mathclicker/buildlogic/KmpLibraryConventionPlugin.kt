package com.sdomashchuk.mathclicker.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * `mathclicker.kmp.library` — the convention for pure-logic KMP modules: AGP's KMP-native
 * Android library plugin (`com.android.kotlin.multiplatform.library`, not the
 * `com.android.library` + `org.jetbrains.kotlin.multiplatform` pairing AGP 9 forbids), an Android
 * host-test target plus `iosArm64`/`iosSimulatorArm64`, and `kotlinx-coroutines-core`/`kotlin.test`
 * wired into `commonMain`/`commonTest`.
 *
 * `KotlinMultiplatformAndroidLibraryTarget` is configured via `extensions.configure`, not the
 * `kotlin { android { ... } }` sugar: that sugar is a Gradle Kotlin DSL type-safe accessor
 * synthesized per `.gradle.kts` script, not a compiled extension function a plugin class can call.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        pluginManager.apply("mathclicker.quality")

        extensions.configure<KotlinMultiplatformExtension> {
            extensions.configure(KotlinMultiplatformAndroidLibraryTarget::class.java) {
                namespace = "com.sdomashchuk.mathclicker.game"
                compileSdk = 36
                minSdk = 24
                withHostTest {}
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }

            iosArm64()
            iosSimulatorArm64()

            sourceSets.commonMain.dependencies {
                implementation(libs.library("kotlinx-coroutines-core"))
            }
            sourceSets.commonTest.dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
