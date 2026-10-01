package com.sdamashchuk.mathbubbles.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * `mathbubbles.kmp.library` — the convention for pure-logic KMP modules: AGP's KMP-native
 * Android library plugin (`com.android.kotlin.multiplatform.library`, not the
 * `com.android.library` + `org.jetbrains.kotlin.multiplatform` pairing AGP 9 forbids), an Android
 * host-test target plus `iosArm64`/`iosSimulatorArm64`, and `kotlinx-coroutines-core`/`kotlin.test`
 * wired into `commonMain`/`commonTest`, plus `kotlinx-coroutines-test` in `commonTest` only.
 *
 * `KotlinMultiplatformAndroidLibraryTarget` is configured via `extensions.configure`, not the
 * `kotlin { android { ... } }` sugar: that sugar is a Gradle Kotlin DSL type-safe accessor
 * synthesized per `.gradle.kts` script, not a compiled extension function a plugin class can call.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            pluginManager.apply("mathbubbles.quality")

            extensions.configure<KotlinMultiplatformExtension> {
                extensions.configure(KotlinMultiplatformAndroidLibraryTarget::class.java) {
                    namespace = namespaceFromPath
                    compileSdk = SdkVersions.COMPILE
                    minSdk = SdkVersions.MIN
                    // Without this, a host test that resolves an Android resource throws
                    // ActivityNotFoundException instead of rendering it.
                    withHostTest {
                        isIncludeAndroidResources = true
                    }
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_17)
                    }
                    // No lint block on purpose. AGP 9.2.1's KMP library plugin creates only
                    // lintAnalyzeAndroidHostTest for this module — no production analysis task and no
                    // report — so a lint { } here is accepted, configures nothing that runs, and reads
                    // as coverage that does not exist. :core therefore has no Android lint; detekt and
                    // spotless do cover it. MC-6.6 re-checks whether a later AGP creates the tasks.
                }

                iosArm64()
                iosSimulatorArm64()

                // kotlin.test only. Coroutines are not every module's business — pushing them in
                // here put kotlinx-coroutines-core into :core:model, whose whole reason to exist is
                // depending on nothing, and a convention that hands out runtime libraries makes each
                // module's real dependencies unreadable from its own build file.
                sourceSets.commonTest.dependencies {
                    implementation(kotlin("test"))
                }
            }
        }
}
