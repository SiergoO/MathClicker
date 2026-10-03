package com.sdamashchuk.mathbubbles.core.game.firespam

data class FireSpamRunResult(
    val seed: Long,
    val survivalGameTimeMs: Long,
    val survivalRealTimeMs: Long,
    val levelReached: Int,
    val score: Int,
    val peakMultiplier: Int,
    val pressesMade: Int,
    val endReason: RunEndReason,
)
