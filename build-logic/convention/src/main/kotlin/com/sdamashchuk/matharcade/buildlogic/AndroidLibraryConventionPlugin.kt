package com.sdamashchuk.matharcade.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * `mathclicker.android.library` — the base convention for every Android library module.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        // Kotlin support is AGP 9's built-in default now that android.builtInKotlin is no longer
        // forced off; applying org.jetbrains.kotlin.android alongside it is a hard error.
        pluginManager.apply("com.android.library")
        pluginManager.apply("mathclicker.quality")

        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
        }
    }
}
