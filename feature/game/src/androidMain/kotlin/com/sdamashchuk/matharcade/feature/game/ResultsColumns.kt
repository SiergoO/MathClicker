package com.sdamashchuk.matharcade.feature.game

import androidx.compose.ui.unit.dp

// Shared between ResultsTable's header row and ResultRow's data rows so the two stay aligned -
// duplicating the weights in each file would let one drift out of sync with the other silently.
internal object ResultsColumns {
    val STAR_WIDTH = 24.dp
    const val DATE_WEIGHT = 1.4f
    const val LEVEL_WEIGHT = 0.8f
    const val SCORE_WEIGHT = 1f
}
