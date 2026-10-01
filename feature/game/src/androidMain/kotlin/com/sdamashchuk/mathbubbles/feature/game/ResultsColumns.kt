package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.ui.Alignment

// Shared between ResultsTable's header row and ResultRow's data rows so the two stay aligned -
// duplicating the weights in each file would let one drift out of sync with the other silently.
internal object ResultsColumns {
    const val DATE_WEIGHT = 2f
    const val LEVEL_WEIGHT = 0.8f
    const val SCORE_WEIGHT = 1f

    val DATE_ALIGNMENT: Alignment = Alignment.CenterStart
    val NUMBER_ALIGNMENT: Alignment = Alignment.CenterEnd
}
