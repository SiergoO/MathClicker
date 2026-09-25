package com.sdamashchuk.matharcade.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * The destinations [RootComponent] drives — Splash, then Menu, then Game or Settings — serialized
 * so the current destination survives process death.
 */
@Serializable
sealed interface RootConfig {
    @Serializable
    data object Splash : RootConfig

    @Serializable
    data object Menu : RootConfig

    @Serializable
    data object Game : RootConfig

    @Serializable
    data object Settings : RootConfig
}
