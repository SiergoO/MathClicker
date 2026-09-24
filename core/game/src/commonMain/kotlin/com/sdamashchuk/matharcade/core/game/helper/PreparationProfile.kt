package com.sdamashchuk.matharcade.core.game.helper

// Percentage split of preparation cost (taps needed before the current divisor succeeds) across a
// generated board. Four buckets rather than a formula, per MC-60: the curve is tuned by playing, and
// a formula would pretend it had been derived. Percentages must sum to 100.
internal data class PreparationProfile(
    val readyNowPercent: Int,
    val oneTapPercent: Int,
    val twoOrThreeTapsPercent: Int,
    val fourPlusTapsPercent: Int,
) {
    init {
        require(readyNowPercent + oneTapPercent + twoOrThreeTapsPercent + fourPlusTapsPercent == PERCENT_TOTAL)
    }

    companion object {
        const val PERCENT_TOTAL = 100
    }
}
