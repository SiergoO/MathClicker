package com.sdomashchuk.mathclicker.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * `mathclicker.android.compose` — enables Compose and applies the Kotlin Compose compiler
 * plugin, replacing the removed `composeOptions.kotlinCompilerExtensionVersion` DSL.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<CommonExtension> {
            buildFeatures.compose = true
        }
    }
}
