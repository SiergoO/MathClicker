package com.sdamashchuk.mathbubbles.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * `mathbubbles.kmp.feature` — the convention for `:feature:*` modules: everything
 * `mathbubbles.kmp.library` provides, plus Compose Multiplatform, Decompose/Essenty navigation
 * and Koin in `commonMain`, mirroring the dependencies `app/build.gradle.kts` wires by hand today.
 */
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("mathbubbles.kmp.library")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply("org.jetbrains.compose")

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.dependencies {
                    implementation(libs.library("compose-ui"))
                    implementation(libs.library("compose-foundation"))
                    implementation(libs.library("compose-foundation-layout"))
                    implementation(libs.library("compose-material"))
                    implementation(libs.library("decompose"))
                    implementation(libs.library("decompose-compose"))
                    implementation(libs.library("essenty-lifecycle"))
                    implementation(libs.library("essenty-instance-keeper"))
                    implementation(libs.library("koin-core"))
                }
            }
        }
}
