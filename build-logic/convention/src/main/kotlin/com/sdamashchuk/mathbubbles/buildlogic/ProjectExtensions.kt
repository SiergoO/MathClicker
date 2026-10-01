package com.sdamashchuk.mathbubbles.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/**
 * The `libs` version catalog, accessible from convention plugin code.
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** Required version string for a catalog version alias (e.g. "agp"). */
internal fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

/** Dependency provider for a catalog library alias (e.g. "kotlinx-coroutines-core"). */
internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> = findLibrary(alias).get()
