package com.sdomashchuk.mathclicker.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import dev.detekt.gradle.extensions.DetektExtension
import dev.detekt.gradle.extensions.FailOnSeverity
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * `mathclicker.quality` — applied by every library and application convention, so no module
 * can opt out once it is wired.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("dev.detekt")
        pluginManager.apply("com.diffplug.spotless")

        extensions.configure<DetektExtension> {
            source.setFrom(files("src/main/java", "src/main/kotlin", "src/test/java", "src/test/kotlin"))
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
