package com.sdamashchuk.mathbubbles.buildlogic

import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

@OptIn(ExperimentalRoborazziApi::class)
class RoborazziConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("io.github.takahirom.roborazzi")

            extensions.configure<RoborazziExtension> {
                outputDir.set(layout.projectDirectory.dir("src/androidHostTest/roborazzi"))
                // Gradle 9 fails on a shared roborazziImageInput when several verify tasks run in one build (roborazzi#830).
                separateOutputDirs.set(true)
            }

            tasks.named("check").configure {
                dependsOn("verifyRoborazzi")
            }
        }
}
