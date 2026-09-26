package com.sdamashchuk.matharcade.feature.game

import com.sdamashchuk.matharcade.core.model.GAME_COLUMN_COUNT

// MC-81: lanes converge toward the row's horizontal centre the higher a target sits in its fall,
// so the board reads as receding into depth. 0.80 of full spacing at the top, full spacing at the
// bottom. Shared by TargetButton (the hit box) and Field's guide-line Canvas so both agree on
// where a lane actually is - a draw-only translation in either place would let the tap target
// drift away from the paint.
private const val LANE_SPACING_AT_TOP = 0.80f
private const val LANE_SPACING_AT_BOTTOM = 1f

/**
 * A lane's horizontal offset from the row's centre, given a fractional column index and how far
 * through its fall the lane's occupant is (0 at the top, 1 at the bottom).
 */
internal fun laneOffsetX(
    laneIndexFraction: Float,
    columnWidth: Int,
    fallFraction: Float,
): Float {
    val distanceFromRowCenter = columnWidth * (laneIndexFraction - GAME_COLUMN_COUNT / 2f)
    val spacingFraction = LANE_SPACING_AT_TOP + (LANE_SPACING_AT_BOTTOM - LANE_SPACING_AT_TOP) * fallFraction
    return distanceFromRowCenter * spacingFraction
}
