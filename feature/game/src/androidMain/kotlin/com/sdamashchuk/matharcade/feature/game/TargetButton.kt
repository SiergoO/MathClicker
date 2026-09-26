package com.sdamashchuk.matharcade.feature.game

import android.util.Size
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdamashchuk.matharcade.core.model.Target
import com.sdamashchuk.matharcade.core.ui.theme.DarkGray
import com.sdamashchuk.matharcade.core.ui.theme.Red200
import com.sdamashchuk.matharcade.core.ui.theme.Red500
import com.sdamashchuk.matharcade.feature.game.model.TargetScreenPosition

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

// A target sits at the midpoint of its column.
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

// MC-83: the specular blob is a light source, not a sticker. Tightened and faded from MC-81's
// 0.6/0.55 - at that strength it read as a white patch pasted on rather than as a sheen, which is
// twice as obvious now that the Material elevation shadow no longer muddies the rest of the ball.
private const val HIGHLIGHT_CENTER_X_FRACTION = -0.34f
private const val HIGHLIGHT_CENTER_Y_FRACTION = -0.40f
private const val HIGHLIGHT_RADIUS_FRACTION = 0.45f
private const val HIGHLIGHT_ALPHA = 0.38f
private const val HIGHLIGHT_MID_STOP = 0.5f
private const val HIGHLIGHT_MID_ALPHA_FRACTION = 0.35f

// MC-82: squash-and-rebound on a hit, not a slow ease - three short steps land the whole gesture
// well under 200ms so rapid tapping never feels mushy waiting on the previous squash to finish.
// MC-83 pulled the extremes in from 0.85/1.10: at that amplitude a fast combo read as the ball
// wobbling rather than as it being struck.
private const val SQUASH_COMPRESS_SCALE = 0.93f
private const val SQUASH_REBOUND_SCALE = 1.04f
private const val SQUASH_COMPRESS_MS = 40
private const val SQUASH_REBOUND_MS = 60
private const val SQUASH_SETTLE_MS = 60

@Composable
fun TargetButton(
    target: Target,
    gameColumnSize: Size,
    isReady: Boolean,
    gameTimeMs: Long,
    onTargetClicked: (id: Int) -> Unit,
    onTargetPositioned: (id: Int, position: TargetScreenPosition) -> Unit,
) {
    val fallFraction = target.position(gameTimeMs)
    val targetButtonYOffset = fallFraction * gameColumnSize.height
    if (target.isActive && targetButtonYOffset.dp > 0.dp) {
        val squashScale = remember(target.id) { Animatable(1f) }
        var lastKnownValue by remember(target.id) { mutableIntStateOf(target.value) }
        // Keyed on both id and value: a tap that doesn't change value (a no-op click) never
        // relaunches this, and a fresh target reusing this slot never compares against a stale
        // predecessor's value.
        LaunchedEffect(target.id, target.value) {
            if (shouldSquashTarget(target.value, lastKnownValue)) squashTarget(squashScale)
            lastKnownValue = target.value
        }
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
        val buttonDiameterDp = (gameColumnSize.width * TARGET_DIAMETER_FRACTION).toFloat()
        val columnCenterXDp = target.columnId * gameColumnSize.width + gameColumnSize.width * LANE_CENTER_FRACTION
        val telegraphBorderColor = if (target.isTelegraphingBreakout(gameTimeMs)) DarkGray else null
        // Reported every active frame, not just on click: the target leaves the composition the
        // instant it zeroes (isActive flips false above), so a burst fired from that same event has
        // nothing left to ask for its position - this is that position's only record.
        SideEffect {
            onTargetPositioned(
                target.id,
                TargetScreenPosition(
                    xDp = columnCenterXDp,
                    yDp = targetButtonYOffset + buttonDiameterDp / 2f,
                ),
            )
        }
        // MC-83: a plain Box, not a Material Button. Button drew an elevation shadow around a
        // transparent background - a dark arc along the sphere's top edge and a smear under it -
        // and a touch ripple over the top of the shading. Both fought the sphere the draw below
        // paints, and neither belongs on an object that is meant to read as lit from one side.
        Box(
            modifier =
                Modifier
                    .width(buttonDiameterDp.dp)
                    .height(buttonDiameterDp.dp)
                    .offset { IntOffset(x = 0, y = targetButtonYOffset.dp.roundToPx()) }
                    .scale(telegraphScale * depthScale * squashScale.value)
                    .drawBehind { drawSphere(baseColor, telegraphBorderColor) }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTargetClicked.invoke(target.id) },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = target.value.toString(), fontSize = 20.sp, color = Color.White)
        }
    }
}

// A real hit only: a tap that misses (value unchanged) or a fresh target reusing this slot (value
// reset upward by remember(target.id)) must never trigger the squash.
internal fun shouldSquashTarget(
    newValue: Int,
    previousValue: Int,
): Boolean = newValue < previousValue

private suspend fun squashTarget(scale: Animatable<Float, AnimationVector1D>) {
    scale.animateTo(SQUASH_COMPRESS_SCALE, tween(SQUASH_COMPRESS_MS))
    scale.animateTo(SQUASH_REBOUND_SCALE, tween(SQUASH_REBOUND_MS))
    scale.animateTo(1f, tween(SQUASH_SETTLE_MS))
}

private fun DrawScope.drawSphere(
    baseColor: Color,
    borderColor: Color?,
) {
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
            0f to Color.White.copy(alpha = HIGHLIGHT_ALPHA),
            HIGHLIGHT_MID_STOP to Color.White.copy(alpha = HIGHLIGHT_ALPHA * HIGHLIGHT_MID_ALPHA_FRACTION),
            1f to Color.White.copy(alpha = 0f),
            center = highlightCenter,
            radius = radiusPx * HIGHLIGHT_RADIUS_FRACTION,
        )
    drawCircle(brush = highlightBrush, radius = radiusPx, center = center)

    // Drawn here rather than as a Button border, because the Button is gone - the telegraph ring
    // has to sit on the sphere's own edge, inside the same draw that owns it.
    borderColor?.let {
        val strokeWidthPx = TELEGRAPH_BORDER_WIDTH_DP.dp.toPx()
        drawCircle(
            color = it,
            radius = radiusPx - strokeWidthPx / 2f,
            center = center,
            style = Stroke(width = strokeWidthPx),
        )
    }
}
