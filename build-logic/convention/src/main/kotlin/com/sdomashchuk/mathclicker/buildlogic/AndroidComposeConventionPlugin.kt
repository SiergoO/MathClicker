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
            // Compose 1.3.2 bundles a lint.jar that AGP 9.2.1 cannot run: this detector dies with
            // an NPE in visitMethodCall and takes lintAnalyzeDebug down with it. Disabling one
            // broken third-party detector is not abortOnError = false — every other check still
            // runs and still fails the build. It lives here rather than in the shared Android
            // config because a module without Compose reports the id as unknown. Remove at MC-6.5,
            // which bumps Compose.
            lintOptions.disable("MutableCollectionMutableState")
        }
    }
}
