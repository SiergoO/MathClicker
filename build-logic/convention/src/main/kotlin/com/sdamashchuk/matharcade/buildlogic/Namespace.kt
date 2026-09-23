package com.sdamashchuk.matharcade.buildlogic

import org.gradle.api.Project

private const val ROOT_PACKAGE = "com.sdamashchuk.matharcade"

/**
 * Android namespace derived from the Gradle path, e.g. `:core:game` ->
 * `com.sdamashchuk.matharcade.core.game`. Modules that need a different value (`:app` sets its
 * own applicationId-derived namespace) assign it directly and are unaffected.
 */
internal val Project.namespaceFromPath: String
    get() = (listOf(ROOT_PACKAGE) + path.removePrefix(":").split(":")).joinToString(".")
