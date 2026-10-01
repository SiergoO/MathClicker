package com.sdamashchuk.mathbubbles.core.game.helper

// Subtraction has one axis that matters, not PreparationProfile's four tap-cost buckets: whether a
// target is a trap at all (see SubtractionTrapCost). trapPercent must be within 0..100.
internal data class SubtractionTrapProfile(
    val trapPercent: Int,
) {
    init {
        require(trapPercent in 0..PERCENT_TOTAL)
    }

    companion object {
        const val PERCENT_TOTAL = 100
    }
}
