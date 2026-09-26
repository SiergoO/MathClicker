package com.sdamashchuk.matharcade.feature.game

import androidx.compose.ui.Alignment

// Shared between ResultsTable's header row and ResultRow's data rows so the two stay aligned -
// duplicating the weights in each file would let one drift out of sync with the other silently.
//
// MC-92: the timestamp reads left and every number reads right. Centring every column is what made
// this a pile rather than a table - digits of different widths never line up, so the eye has no
// column to run down. The star column is gone with it: it was empty on nine rows in ten and cost the
// width the timestamp now needs to carry a time of day, and the best run is marked by its own colour.
internal object ResultsColumns {
    const val DATE_WEIGHT = 2f
    const val LEVEL_WEIGHT = 0.8f
    const val SCORE_WEIGHT = 1f

    // Box alignments, not Alignment.Horizontal: the header and the data rows both lay a cell out as
    // a Box, and storing the resolved value here is what stops the two from drifting apart.
    val DATE_ALIGNMENT: Alignment = Alignment.CenterStart
    val NUMBER_ALIGNMENT: Alignment = Alignment.CenterEnd
}
