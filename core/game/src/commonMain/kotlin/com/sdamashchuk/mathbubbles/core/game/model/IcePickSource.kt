package com.sdamashchuk.mathbubbles.core.game.model

/**
 * Which control is holding an armed ice pick, so the UI knows where to pulse it.
 */
sealed interface IcePickSource {
    data object FireButton : IcePickSource

    data class StashSlot(
        val index: Int,
    ) : IcePickSource
}
