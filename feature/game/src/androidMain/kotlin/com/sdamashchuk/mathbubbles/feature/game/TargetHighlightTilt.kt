package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.ui.geometry.Offset

// One light above the water's centre, so bubbles mirrored across the centre get mirrored highlights.
internal object TargetHighlightTilt {
    private const val X_FRACTION_BASE = -0.34f
    private const val Y_FRACTION_BASE = -0.33f
    private const val TILT_X_FRACTION = 0.22f
    private const val TILT_Y_FRACTION = 0.05f
    private const val ANGLE_DEGREES = 18f
    private const val FULL_RANGE_SCALE = 2f
    private const val COLUMN_CENTER_FRACTION = 0.5f

    fun horizontalFraction(
        columnId: Int,
        columnCount: Int,
    ): Float =
        if (columnCount <= 0) {
            0f
        } else {
            ((columnId + COLUMN_CENTER_FRACTION) / columnCount) * FULL_RANGE_SCALE - 1f
        }

    fun baseOffsetFraction(horizontalFraction: Float): Offset =
        Offset(x = X_FRACTION_BASE + TILT_X_FRACTION * horizontalFraction, y = Y_FRACTION_BASE)

    fun fallOffsetDelta(fallFraction: Float): Offset = Offset(x = 0f, y = TILT_Y_FRACTION * fallFraction)

    fun angleDegrees(horizontalFraction: Float): Float = ANGLE_DEGREES * horizontalFraction
}
