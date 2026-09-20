package com.sdomashchuk.mathclicker.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `mathclicker.quality` — applied by every library and application convention, so no module
 * can opt out once it is wired.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // MC-4.3 wires detekt and spotless here
    }
}
