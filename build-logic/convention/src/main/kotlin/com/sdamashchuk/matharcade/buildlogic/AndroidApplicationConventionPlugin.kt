package com.sdamashchuk.matharcade.buildlogic

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

/**
 * `matharcade.android.application` — the launcher module convention: Android application
 * plugin, Kotlin, the shared compile/lint config, and the app's `targetSdk`.
 *
 * Kotlin support is AGP 9's built-in default now that android.builtInKotlin is no longer forced
 * off; applying org.jetbrains.kotlin.android alongside it is a hard error.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("matharcade.quality")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = SdkVersions.TARGET
                packaging { }
            }

            val androidComponents = extensions.getByType<ApplicationAndroidComponentsExtension>()
            androidComponents.onVariants(androidComponents.selector().withBuildType("debug")) { variant ->
                val checkAppLaunch =
                    tasks.register<AppLaunchCheckTask>("checkAppLaunch") {
                        apkFolder.set(variant.artifacts.get(SingleArtifact.APK))
                        builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
                        applicationId.set(variant.applicationId)
                        adbExecutable.set(androidComponents.sdkComponents.adb)
                        requestedSerial.set(providers.environmentVariable("ANDROID_SERIAL"))
                    }
                tasks.named<Task>("check") { dependsOn(checkAppLaunch) }
            }
        }
}
