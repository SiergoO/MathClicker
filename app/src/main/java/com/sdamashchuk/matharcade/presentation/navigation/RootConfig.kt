package com.sdamashchuk.matharcade.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * The destinations [RootComponent] drives — Splash, then Menu, then Game — serialized so the
 * current destination survives process death.
 */
@Serializable
sealed interface RootConfig {
    @Serializable
    data object Splash : RootConfig

    @Serializable
    data object Menu : RootConfig

    @Serializable
    data object Game : RootConfig
}
