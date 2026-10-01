package com.sdamashchuk.mathbubbles.feature.game

import android.util.Size
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdamashchuk.mathbubbles.core.model.Target
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleFillIdle
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleFillReady
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleRimIdle
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleRimReady
import com.sdamashchuk.mathbubbles.core.ui.theme.Ink
import com.sdamashchuk.mathbubbles.feature.game.model.BubbleHighlight
import com.sdamashchuk.mathbubbles.feature.game.model.BubbleStyle
import com.sdamashchuk.mathbubbles.feature.game.model.TargetScreenPosition
import kotlin.math.abs

// Telegraphs the final 15% of a fall so a breakout is never a surprise: a size pulse plus a ring.
private const val TARGET_DIAMETER_FRACTION = 0.8
private const val TELEGRAPH_PULSE_SCALE = 1.06f
private const val TELEGRAPH_PULSE_MS = 300
private const val TELEGRAPH_RING_WIDTH_FRACTION = 0.03f
private const val TELEGRAPH_RING_ALPHA = 0.85f

private const val READINESS_TRANSITION_MS = 200

// 0.72 is a hard floor, not a taste call: the target is 0.8 of a column, a column is about 81.6dp on a
// 411dp phone, and 48dp of minimum touch target is 0.59 of that. Do not lower it without redoing this.
private const val DEPTH_SCALE_AT_TOP = 0.72f
private const val DEPTH_SCALE_AT_BOTTOM = 1f

private const val LANE_CENTER_FRACTION = 0.5f

// Fitted numerically against the reference art, not eyeballed - mean absolute channel error 3.4%.
// The highlight is two layers, not one: with a single blob the fit will not go below 10.7/255.
private const val FILL_ALPHA_READY = 0.62f
private const val FILL_ALPHA_IDLE = 0.28f
private const val RIM_WIDTH_FRACTION_READY = 0.027f
private const val RIM_WIDTH_FRACTION_IDLE = 0.031f
private const val HIGHLIGHT_CORE_ALPHA_READY = 0.90f
private const val HIGHLIGHT_CORE_ALPHA_IDLE = 0.30f
private const val HIGHLIGHT_GLOW_ALPHA_READY = 0.36f
private const val HIGHLIGHT_GLOW_ALPHA_IDLE = 0.14f

private const val DIGIT_SIZE_FRACTION = 0.28f

private const val CRACK_STROKE_WIDTH_FRACTION = 0.02f
private const val CRACK_ALPHA = 0.8f
private const val CRACK_BRANCH_1_X = 0.4f
private const val CRACK_BRANCH_1_Y = 0.6f
private const val CRACK_BRANCH_2_X = 0.5f
private const val CRACK_BRANCH_2_Y = 0.3f
private const val CRACK_BRANCH_3_X = 0.3f
private const val CRACK_BRANCH_3_Y = 0.5f

// A target that cannot be reduced by the armed operation is fully idle; one that can, but is not
// yet a single press from zero, sits partway. One number drives every layer, so the three states
// are one animatable value rather than three branches. An unreachable target additionally loses its
// highlight core, which is what separates it from a merely unready one at a glance.
private const val LIVELINESS_PROFITABLE_FLOOR = 0.62f
private const val UNREACHABLE_CORE_ALPHA = 0.06f
private const val UNREACHABLE_FILL_ALPHA = 0.16f

// Three short steps land the whole gesture under 200ms, so rapid tapping never waits on the previous one.
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
    // A provider, not a Long: passing the clock by value would read a per-frame value during composition
    // and recompose every target 60 times a second. The lambdas below cost a relayout and a redraw instead.
    gameTimeMsProvider: () -> Long,
    onTargetClicked: (id: Int) -> Unit,
    onTargetPositioned: (id: Int, position: TargetScreenPosition) -> Unit,
    icePickArmed: Boolean = false,
) {
    val squashScale = remember(target.id) { Animatable(1f) }
    var lastKnownValue by remember(target.id) { mutableIntStateOf(target.value) }
    // Keyed on both id and value: a tap that doesn't change value (a no-op click) never
    // relaunches this, and a fresh target reusing this slot never compares against a stale
    // predecessor's value.
    LaunchedEffect(target.id, target.value) {
        if (shouldSquashTarget(target.value, lastKnownValue)) squashTarget(squashScale)
        lastKnownValue = target.value
    }
    val readinessFraction by
        animateFloatAsState(
            targetValue = if (isReady) 1f else 0f,
            animationSpec = tween(READINESS_TRANSITION_MS),
            label = "readinessFraction",
        )
    val liveliness = liveliness(target.isProfitable, readinessFraction)
    val buttonDiameterDp = (gameColumnSize.width * TARGET_DIAMETER_FRACTION).toFloat()
    val bubbleStyle = targetBubbleStyle(buttonDiameterDp.dp, liveliness)
    val columnCenterXDp = target.columnId * gameColumnSize.width + gameColumnSize.width * LANE_CENTER_FRACTION
    val columnHeight = gameColumnSize.height

    Box(
        modifier =
            Modifier
                .width(buttonDiameterDp.dp)
                .height(buttonDiameterDp.dp)
                .offset {
                    val fallFraction = target.position(gameTimeMsProvider())
                    val yDp = fallFraction * columnHeight
                    // Recorded from the layout lambda, not a SideEffect: the effect only ran after a recomposition, and it
                    // wrote to a snapshot map, which allocates a record per write.
                    onTargetPositioned(
                        target.id,
                        TargetScreenPosition(xDp = columnCenterXDp, yDp = yDp + buttonDiameterDp / 2f),
                    )
                    IntOffset(x = 0, y = yDp.dp.roundToPx())
                }.graphicsLayer {
                    val gameTimeMs = gameTimeMsProvider()
                    val fallFraction = target.position(gameTimeMs)
                    val depthScale =
                        DEPTH_SCALE_AT_TOP + (DEPTH_SCALE_AT_BOTTOM - DEPTH_SCALE_AT_TOP) * fallFraction
                    val combined =
                        depthScale *
                            telegraphPulse(gameTimeMs, target.isTelegraphingBreakout(gameTimeMs)) *
                            squashScale.value
                    scaleX = combined
                    scaleY = combined
                }.bubbleSurface(bubbleStyle)
                .icePickAndTelegraph(target, gameTimeMsProvider, icePickArmed)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onTargetClicked.invoke(target.id) },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = target.value.toString(),
            fontSize = (buttonDiameterDp * DIGIT_SIZE_FRACTION).sp,
            color = Ink,
        )
    }
}

// Derived from the engine clock, not an InfiniteTransition: one transition per target ran permanently
// whether or not it was telegraphing, each scheduling its own frame callback.
internal fun telegraphPulse(
    gameTimeMs: Long,
    isTelegraphing: Boolean,
): Float {
    if (!isTelegraphing) return 1f
    val period = TELEGRAPH_PULSE_MS * 2f
    // Parenthesised deliberately: % and * bind left to right, so dropping these brackets gives
    // (t % 300) * 2 - a pulse at double speed that never reaches its peak.
    val phase = (gameTimeMs % (TELEGRAPH_PULSE_MS * 2L)).toFloat() / period
    return 1f + (TELEGRAPH_PULSE_SCALE - 1f) * (1f - abs(2f * phase - 1f))
}

internal fun liveliness(
    isProfitable: Boolean,
    readinessFraction: Float,
): Float =
    if (!isProfitable) {
        0f
    } else {
        LIVELINESS_PROFITABLE_FLOOR + (1f - LIVELINESS_PROFITABLE_FLOOR) * readinessFraction
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

// A target that cannot be reduced (isUnreachable) drops to its own, lower floor instead of the
// formula's idle values - that is what separates it from a merely unready one at a glance.
private fun targetBubbleStyle(
    diameter: Dp,
    liveliness: Float,
): BubbleStyle {
    val isUnreachable = liveliness <= 0f
    val fillAlpha =
        if (isUnreachable) {
            UNREACHABLE_FILL_ALPHA
        } else {
            FILL_ALPHA_IDLE + (FILL_ALPHA_READY - FILL_ALPHA_IDLE) * liveliness
        }
    val coreAlpha =
        if (isUnreachable) {
            UNREACHABLE_CORE_ALPHA
        } else {
            HIGHLIGHT_CORE_ALPHA_IDLE + (HIGHLIGHT_CORE_ALPHA_READY - HIGHLIGHT_CORE_ALPHA_IDLE) * liveliness
        }
    val glowAlpha =
        HIGHLIGHT_GLOW_ALPHA_IDLE + (HIGHLIGHT_GLOW_ALPHA_READY - HIGHLIGHT_GLOW_ALPHA_IDLE) * liveliness
    return BubbleStyle(
        fillColor = lerp(BubbleFillIdle, BubbleFillReady, liveliness),
        fillAlpha = fillAlpha,
        rimColor = lerp(BubbleRimIdle, BubbleRimReady, liveliness),
        rimWidth =
            diameter * (RIM_WIDTH_FRACTION_IDLE + (RIM_WIDTH_FRACTION_READY - RIM_WIDTH_FRACTION_IDLE) * liveliness),
        highlight = BubbleHighlight(coreAlpha, glowAlpha),
    )
}

private fun Modifier.icePickAndTelegraph(
    target: Target,
    gameTimeMsProvider: () -> Long,
    icePickArmed: Boolean,
) = drawWithCache {
    val radius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)

    onDrawBehind {
        if (icePickArmed) {
            val crackWidth = size.minDimension * CRACK_STROKE_WIDTH_FRACTION
            val crackColor = Ink.copy(alpha = CRACK_ALPHA)
            drawLine(
                crackColor,
                Offset(center.x - radius * CRACK_BRANCH_1_X, center.y - radius * CRACK_BRANCH_1_Y),
                center,
                crackWidth,
            )
            drawLine(
                crackColor,
                center,
                Offset(center.x + radius * CRACK_BRANCH_2_X, center.y + radius * CRACK_BRANCH_2_Y),
                crackWidth,
            )
            drawLine(
                crackColor,
                center,
                Offset(center.x - radius * CRACK_BRANCH_3_X, center.y + radius * CRACK_BRANCH_3_Y),
                crackWidth,
            )
        }
        if (target.isTelegraphingBreakout(gameTimeMsProvider())) {
            val ringWidth = size.minDimension * TELEGRAPH_RING_WIDTH_FRACTION
            drawCircle(
                BubbleRimReady.copy(alpha = TELEGRAPH_RING_ALPHA),
                radius = radius - ringWidth / 2f,
                center = center,
                style = Stroke(ringWidth),
            )
        }
    }
}
