package com.sdamashchuk.matharcade.feature.game.model

// id is the signal's sequence, not the targetId: PlayArea keys BurstRing's composition on it, and
// a target can in principle zero more than once across the game were ids ever reused.
data class TargetZeroedBurst(
    val id: Int,
    val position: TargetScreenPosition,
)
