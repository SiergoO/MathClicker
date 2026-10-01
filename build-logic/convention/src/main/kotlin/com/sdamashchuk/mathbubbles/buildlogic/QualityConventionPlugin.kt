package com.sdamashchuk.mathbubbles.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import dev.detekt.gradle.extensions.DetektExtension
import dev.detekt.gradle.extensions.FailOnSeverity
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/**
 * `mathbubbles.quality` — applied by every library and application convention, so no module
 * can opt out once it is wired.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("dev.detekt")
            pluginManager.apply("com.diffplug.spotless")

            extensions.configure<DetektExtension> {
                // Ask Kotlin which directories it compiles instead of listing them here. A hardcoded
                // list has gone stale twice: once when :core moved to commonMain, and again when
                // androidMain appeared and detekt reported UP-TO-DATE over unanalysed code. Deriving
                // it means a new source set — iosMain at MC-6.5, feature modules at MC-9 — is covered
                // the day it exists.
                source.setFrom(
                    provider {
                        val buildDir = layout.buildDirectory.get().asFile
                        val fromKotlin =
                            extensions
                                .findByType(KotlinProjectExtension::class.java)
                                ?.sourceSets
                                ?.flatMap { it.kotlin.srcDirs }
                                // SQLDelight (and any future generator) registers its output as a Kotlin
                                // source dir under build/; that is generated code, never ours to lint.
                                ?.filter { it.isDirectory && !it.toPath().startsWith(buildDir.toPath()) }
                                .orEmpty()
                        // AGP 9's built-in Kotlin registers no KotlinProjectExtension, so a module that
                        // drops org.jetbrains.kotlin.android derives nothing and detekt goes NO-SOURCE:
                        // a green gate over code nobody read. That is how :app lost its analysis at
                        // MC-10. src/ is the fallback, and an empty result is now a hard failure rather
                        // than a silent pass, so the next way this goes stale cannot be quiet.
                        val sources = fromKotlin.ifEmpty { listOf(file("src")).filter { it.isDirectory } }
                        check(sources.isNotEmpty()) {
                            "detekt found no source directories for $path: no Kotlin source set and no src/"
                        }
                        sources
                    },
                )
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
                buildUponDefaultConfig.set(true)
                parallel.set(true)
                failOnSeverity.set(FailOnSeverity.Warning)
                // Grandfathers the pre-existing findings from onboarding detekt onto code that predates
                // it (magic numbers, oversized classes, file/type name mismatches unrelated to this
                // task's scope). Every rule stays fully active; only these exact, already-known issues
                // are suppressed. Anything new — including a new issue in an already-baselined file —
                // still fails the gate.
                baseline.set(file("detekt-baseline.xml"))
            }

            extensions.configure<SpotlessExtension> {
                // trimTrailingWhitespace()/endWithNewline() are not needed here: ktlint already applies
                // them from the root .editorconfig (trim_trailing_whitespace, insert_final_newline).
                kotlin {
                    target("src/**/*.kt")
                    ktlint(libs.version("ktlint"))
                }
                kotlinGradle {
                    target("*.gradle.kts")
                    ktlint(libs.version("ktlint"))
                }
            }
        }
}
