package com.sdamashchuk.matharcade.feature.game.model

/**
 * The near-miss verdict on this run's score against the player's best across all history - the
 * point of the results screen (see the MC-53 spec), not decoration on top of it.
 */
sealed interface ResultsSummary {
    val score: Int

    data class NewRecord(
        override val score: Int,
    ) : ResultsSummary

    data class ShortOfBest(
        override val score: Int,
        val deltaToBest: Int,
    ) : ResultsSummary

    // Only reachable if getBestClosedField somehow finds nothing for a field that was just closed
    // and persisted - a defensive case, not a state production play ever produces, but one the
    // screen still has to render honestly rather than crash or claim a fabricated record.
    data class NoHistory(
        override val score: Int,
    ) : ResultsSummary
}
