package com.sdomashchuk.mathclicker.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * `mathclicker.android.compose` — enables Compose, applies the Kotlin Compose compiler plugin
 * (replacing the removed `composeOptions.kotlinCompilerExtensionVersion` DSL), and applies the
 * Compose Multiplatform plugin so `:app`'s dependencies resolve against the CMP BOM.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("org.jetbrains.compose")

        extensions.configure<CommonExtension> {
            buildFeatures.compose = true
        }
    }
}
