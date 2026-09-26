package com.sdamashchuk.matharcade.feature.game

import android.util.Size
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdamashchuk.matharcade.core.model.Target
import com.sdamashchuk.matharcade.core.ui.theme.DarkGray
import com.sdamashchuk.matharcade.core.ui.theme.Red200
import com.sdamashchuk.matharcade.core.ui.theme.Red500

// MC-54: telegraphs the final 15% of a fall (Target.isTelegraphingBreakout) so a breakout is never
// a surprise. Deliberately not colour-coded - isProfitable already owns that channel below, and a
// second meaning on the same channel would make both unreadable - so the cue is a size pulse plus a
// ring, both legible independent of hue. Verified on device: see the MC-54 task report.
// A target fills most of its column but never touches the column's edges.
private const val TARGET_DIAMETER_FRACTION = 0.8
private const val TELEGRAPH_PULSE_SCALE = 1.15f
private const val TELEGRAPH_PULSE_MS = 300
private const val TELEGRAPH_BORDER_WIDTH_DP = 3

// MC-61: readiness hint. isReady is already the policy's own decision (see shouldShowReadinessHint)
// - this composable only animates the colour, Red200 to Red500 over ~200ms rather than an instant
// swap, which on a 4-26 target board reads as a flicker whenever the divisor changes and half the
// board flips at once. Grey (isProfitable) still wins outright: the lerp only ever runs inside the
// profitable branch below, so an unprofitable target is never mid-fade toward red.
private const val READINESS_TRANSITION_MS = 200

// MC-81: depth cue for the fall. 0.72 is a hard floor, not a taste call - the target is
// TARGET_DIAMETER_FRACTION (0.8) of a column, and a column is (screenWidthDp - 3) / 4; on a 411dp
// phone that is 81.6dp. Android's minimum touch target is 48dp, i.e. 0.59 of that near size. 0.72
// keeps margin above 0.59 for a shaky finger - do not lower it without redoing this math.
private const val DEPTH_SCALE_AT_TOP = 0.72f
private const val DEPTH_SCALE_AT_BOTTOM = 1f

// MC-81: a target sits at the midpoint of its column - the fractional column index its lane
// convergence (see laneOffsetX) is computed from.
private const val LANE_CENTER_FRACTION = 0.5f

// MC-81: sphere shading. One radial gradient whose centre is offset toward the light rather than
// three flat bands - the far side of that offset is naturally the farthest point from the light,
// which is what reads as "deepening to a darker red at the lower-right" without a second shape.
private const val GRADIENT_LIGHT_CENTER_X_FRACTION = -0.3f
private const val GRADIENT_LIGHT_CENTER_Y_FRACTION = -0.3f
private const val GRADIENT_RADIUS_FRACTION = 1.8f
private const val GRADIENT_LIGHT_TINT_FRACTION = 0.5f
private const val GRADIENT_MID_STOP = 0.55f
private const val GRADIENT_DARK_SHADE_FRACTION = 0.55f

// MC-81: specular highlight blob, layered on top of the base shading above rather than folded into
// its gradient stops, so its position and falloff can be tuned independently.
private const val HIGHLIGHT_CENTER_X_FRACTION = -0.36f
private const val HIGHLIGHT_CENTER_Y_FRACTION = -0.44f
private const val HIGHLIGHT_RADIUS_FRACTION = 0.6f
private const val HIGHLIGHT_ALPHA = 0.55f

@Composable
fun TargetButton(
    target: Target,
    gameColumnSize: Size,
    isReady: Boolean,
    gameTimeMs: Long,
    onTargetClicked: (id: Int) -> Unit,
) {
    val fallFraction = target.position(gameTimeMs)
    val targetButtonYOffset = fallFraction * gameColumnSize.height
    if (target.isActive && targetButtonYOffset.dp > 0.dp) {
        val telegraphTransition = rememberInfiniteTransition(label = "breakoutTelegraph")
        val telegraphScale by
            telegraphTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (target.isTelegraphingBreakout(gameTimeMs)) TELEGRAPH_PULSE_SCALE else 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(TELEGRAPH_PULSE_MS, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "breakoutTelegraphScale",
            )
        val readinessFraction by
            animateFloatAsState(
                targetValue = if (isReady) 1f else 0f,
                animationSpec = tween(READINESS_TRANSITION_MS),
                label = "readinessFraction",
            )
        val baseColor =
            if (target.isProfitable) lerp(Red200, Red500, readinessFraction) else Color.LightGray
        val depthScale = DEPTH_SCALE_AT_TOP + (DEPTH_SCALE_AT_BOTTOM - DEPTH_SCALE_AT_TOP) * fallFraction
        val laneIndexFraction = target.columnId + LANE_CENTER_FRACTION
        // Delta from where Box(TopCenter) already centres the target within its own column - not
        // an absolute position - because that placement is the fallFraction = 1 (full spacing)
        // baseline this offset shifts away from.
        val convergenceOffsetXDp =
            laneOffsetX(laneIndexFraction, gameColumnSize.width, fallFraction) -
                laneOffsetX(laneIndexFraction, gameColumnSize.width, fallFraction = 1f)
        Button(
            modifier =
                Modifier
                    .width((gameColumnSize.width * TARGET_DIAMETER_FRACTION).dp)
                    .height((gameColumnSize.width * TARGET_DIAMETER_FRACTION).dp)
                    .offset {
                        // The lambda form, not offset(x.dp, y.dp): this moves the hit box itself,
                        // so a converging lane never lets the player tap where the paint isn't.
                        IntOffset(
                            x = convergenceOffsetXDp.dp.roundToPx(),
                            y = targetButtonYOffset.dp.roundToPx(),
                        )
                    }.scale(telegraphScale * depthScale)
                    .clip(CircleShape)
                    .drawBehind { drawSphere(baseColor) },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
            border =
                if (target.isTelegraphingBreakout(gameTimeMs)) {
                    BorderStroke(TELEGRAPH_BORDER_WIDTH_DP.dp, DarkGray)
                } else {
                    null
                },
            onClick = { onTargetClicked.invoke(target.id) },
        ) {
            Text(text = target.value.toString(), fontSize = 20.sp, color = Color.White)
        }
    }
}

private fun DrawScope.drawSphere(baseColor: Color) {
    val radiusPx = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val lightCenter =
        Offset(
            center.x + GRADIENT_LIGHT_CENTER_X_FRACTION * radiusPx,
            center.y + GRADIENT_LIGHT_CENTER_Y_FRACTION * radiusPx,
        )
    val sphereBrush =
        Brush.radialGradient(
            0f to lerp(baseColor, Color.White, GRADIENT_LIGHT_TINT_FRACTION),
            GRADIENT_MID_STOP to baseColor,
            1f to lerp(baseColor, Color.Black, GRADIENT_DARK_SHADE_FRACTION),
            center = lightCenter,
            radius = radiusPx * GRADIENT_RADIUS_FRACTION,
        )
    drawCircle(brush = sphereBrush, radius = radiusPx, center = center)

    val highlightCenter =
        Offset(
            center.x + HIGHLIGHT_CENTER_X_FRACTION * radiusPx,
            center.y + HIGHLIGHT_CENTER_Y_FRACTION * radiusPx,
        )
    val highlightBrush =
        Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = HIGHLIGHT_ALPHA), Color.White.copy(alpha = 0f)),
            center = highlightCenter,
            radius = radiusPx * HIGHLIGHT_RADIUS_FRACTION,
        )
    drawCircle(brush = highlightBrush, radius = radiusPx, center = center)
}
