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
import androidx.compose.runtime.mutableStateOf
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
import com.sdamashchuk.mathbubbles.core.model.EFFECT_RAMP_MS
import com.sdamashchuk.mathbubbles.core.model.GAME_COLUMN_COUNT
import com.sdamashchuk.mathbubbles.core.model.Target
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleFillIdle
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleFillReady
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleRimIdle
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleRimReady
import com.sdamashchuk.mathbubbles.core.ui.theme.Ink
import com.sdamashchuk.mathbubbles.feature.game.model.BubbleHighlight
import com.sdamashchuk.mathbubbles.feature.game.model.BubbleStyle
import com.sdamashchuk.mathbubbles.feature.game.model.TargetScreenPosition
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

// Telegraphs the final 15% of a fall so a breakout is never a surprise: a size pulse plus a ring.
internal const val TARGET_DIAMETER_FRACTION = 0.8
private const val TELEGRAPH_PULSE_SCALE = 1.06f
private const val TELEGRAPH_PULSE_MS = 300
private const val TELEGRAPH_RING_WIDTH_FRACTION = 0.03f
private const val TELEGRAPH_RING_ALPHA = 0.85f

// 0.72 is a hard floor, not a taste call: the target is 0.8 of a column, a column is about 81.6dp on a
// 411dp phone, and 48dp of minimum touch target is 0.59 of that. Do not lower it without redoing this.
private const val DEPTH_SCALE_AT_TOP = 0.72f
private const val DEPTH_SCALE_AT_BOTTOM = 1f

private const val LANE_CENTER_FRACTION = 0.5f

internal fun columnCenterXDp(
    columnId: Int,
    columnWidthDp: Int,
): Float = columnId * columnWidthDp + columnWidthDp * LANE_CENTER_FRACTION

// Fitted numerically against the reference art, not eyeballed - mean absolute channel error 3.4%.
// The highlight is two layers, not one: with a single blob the fit will not go below 10.7/255.
private const val FILL_ALPHA_IDLE = 0.28f
private const val FILL_ALPHA_READY = 0.62f
private const val RIM_WIDTH_FRACTION_IDLE = 0.031f
private const val RIM_WIDTH_FRACTION_READY = 0.027f
private const val HIGHLIGHT_CORE_ALPHA_IDLE = 0.30f
private const val HIGHLIGHT_CORE_ALPHA_READY = 0.90f
private const val HIGHLIGHT_GLOW_ALPHA_IDLE = 0.14f
private const val HIGHLIGHT_GLOW_ALPHA_READY = 0.36f

// The standard look every profitable bubble now holds steady at: the old readiness blend's
// not-yet-ready floor, baked in rather than animated toward the READY constants above.
private const val STANDARD_LIVELINESS = 0.62f
private val STANDARD_FILL_COLOR = lerp(BubbleFillIdle, BubbleFillReady, STANDARD_LIVELINESS)
private val STANDARD_RIM_COLOR = lerp(BubbleRimIdle, BubbleRimReady, STANDARD_LIVELINESS)
private const val STANDARD_FILL_ALPHA = FILL_ALPHA_IDLE + (FILL_ALPHA_READY - FILL_ALPHA_IDLE) * STANDARD_LIVELINESS
private const val STANDARD_RIM_WIDTH_FRACTION =
    RIM_WIDTH_FRACTION_IDLE + (RIM_WIDTH_FRACTION_READY - RIM_WIDTH_FRACTION_IDLE) * STANDARD_LIVELINESS
private const val STANDARD_CORE_ALPHA =
    HIGHLIGHT_CORE_ALPHA_IDLE + (HIGHLIGHT_CORE_ALPHA_READY - HIGHLIGHT_CORE_ALPHA_IDLE) * STANDARD_LIVELINESS
private const val STANDARD_GLOW_ALPHA =
    HIGHLIGHT_GLOW_ALPHA_IDLE + (HIGHLIGHT_GLOW_ALPHA_READY - HIGHLIGHT_GLOW_ALPHA_IDLE) * STANDARD_LIVELINESS

private const val DIGIT_SIZE_FRACTION = 0.28f

// The ice pick's targeting cue: every visible bubble sways out of sync with its neighbours rather
// than carrying a static mark, so the player reads "something is armed" without a crack to parse.
private const val SWAY_PERIOD_MS = 2_000
private const val SWAY_AMPLITUDE_DP = 5f

private const val UNREACHABLE_CORE_ALPHA = 0.06f
private const val UNREACHABLE_FILL_ALPHA = 0.16f

private const val READY_HINT_SHAKE_AMPLITUDE_DP = 2f

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
    // Frame time, not gameTimeMsProvider: the sway must keep moving while Freeze or Rewind
    // holds the game clock still or running backward.
    realTimeMsProvider: () -> Long = gameTimeMsProvider,
    interactive: Boolean = true,
    fadeProvider: () -> Float = { 0f },
    modifier: Modifier = Modifier,
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
    var reachableSinceMs by remember(target.id) { mutableStateOf<Long?>(null) }
    LaunchedEffect(target.id, isReady) {
        reachableSinceMs = if (isReady) gameTimeMsProvider() else null
    }
    // The ice pick's own onset: every bubble's sway grows in as it arms and shrinks out as it
    // disarms, on the same span every other booster cue ramps over.
    val swayEnvelope by
        animateFloatAsState(
            targetValue = if (icePickArmed) 1f else 0f,
            animationSpec = tween(EFFECT_RAMP_MS),
            label = "icePickSwayEnvelope",
        )
    val swayPhase = remember(target.id) { swayPhase(target.id) }
    val buttonDiameterDp = (gameColumnSize.width * TARGET_DIAMETER_FRACTION).toFloat()
    val horizontalFraction = TargetHighlightTilt.horizontalFraction(target.columnId, GAME_COLUMN_COUNT)
    val bubbleStyle = targetBubbleStyle(buttonDiameterDp.dp, target.isProfitable, horizontalFraction)
    val columnCenterXDp = columnCenterXDp(target.columnId, gameColumnSize.width)
    val columnHeight = gameColumnSize.height
    val interactionModifier =
        if (interactive) {
            Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onTargetClicked.invoke(target.id) },
            )
        } else {
            Modifier
        }

    Box(
        modifier =
            modifier
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
                    val fadeProgress = fadeProvider()
                    val combined =
                        depthScale *
                            telegraphPulse(gameTimeMs, target.isTelegraphingBreakout(gameTimeMs)) *
                            squashScale.value *
                            DisappearFade.scale(fadeProgress)
                    scaleX = combined
                    scaleY = combined
                    alpha = DisappearFade.alpha(fadeProgress)
                    if (swayEnvelope > 0f) {
                        val offset = swayOffsetPx(realTimeMsProvider(), swayPhase, SWAY_AMPLITUDE_DP.dp.toPx())
                        translationX += offset * swayEnvelope
                    }
                    reachableSinceMs?.let { since ->
                        val hint =
                            ShakeHintTiming.offsetAt(
                                reachableSinceMs = since,
                                nowMs = gameTimeMs,
                                seed = target.id,
                                amplitudePx = READY_HINT_SHAKE_AMPLITUDE_DP.dp.toPx(),
                            )
                        translationX += hint.x
                        translationY += hint.y
                    }
                }.bubbleSurface(
                    bubbleStyle,
                    highlightOffsetProvider = {
                        TargetHighlightTilt.fallOffsetDelta(target.position(gameTimeMsProvider()))
                    },
                ).telegraphRing(target, gameTimeMsProvider)
                .then(interactionModifier),
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

// Every target shares one standard look; only a target that would bring no points
// (isProfitable false) drops to the dimmer, coreless unreachable look.
internal fun targetBubbleStyle(
    diameter: Dp,
    isProfitable: Boolean,
    horizontalFraction: Float,
): BubbleStyle {
    val baseOffset = TargetHighlightTilt.baseOffsetFraction(horizontalFraction)
    val highlight =
        BubbleHighlight(
            coreAlpha = if (isProfitable) STANDARD_CORE_ALPHA else UNREACHABLE_CORE_ALPHA,
            glowAlpha = if (isProfitable) STANDARD_GLOW_ALPHA else HIGHLIGHT_GLOW_ALPHA_IDLE,
            offsetXFraction = baseOffset.x,
            offsetYFraction = baseOffset.y,
            rotationDegrees = TargetHighlightTilt.angleDegrees(horizontalFraction),
        )
    return if (isProfitable) {
        BubbleStyle(
            fillColor = STANDARD_FILL_COLOR,
            fillAlpha = STANDARD_FILL_ALPHA,
            rimColor = STANDARD_RIM_COLOR,
            rimWidth = diameter * STANDARD_RIM_WIDTH_FRACTION,
            highlight = highlight,
        )
    } else {
        BubbleStyle(
            fillColor = BubbleFillIdle,
            fillAlpha = UNREACHABLE_FILL_ALPHA,
            rimColor = BubbleRimIdle,
            rimWidth = diameter * RIM_WIDTH_FRACTION_IDLE,
            highlight = highlight,
        )
    }
}

private fun Modifier.telegraphRing(
    target: Target,
    gameTimeMsProvider: () -> Long,
) = drawWithCache {
    val radius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)

    onDrawBehind {
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

// A large odd multiplier spreads consecutive ids across the full phase range before the modulo,
// rather than clustering nearby ids near 0 - the usual trick behind a cheap integer hash.
private const val PHASE_HASH_MULTIPLIER = 2_654_435_761L
private const val PHASE_HASH_MODULUS = 1_000L

// A cheap stand-in for per-bubble randomness: deterministic in id, so two bubbles never happen to
// share a phase, without pulling a Random instance into a composable.
internal fun swayPhase(id: Int): Float =
    (id * PHASE_HASH_MULTIPLIER % PHASE_HASH_MODULUS).toFloat() / PHASE_HASH_MODULUS * (2f * PI.toFloat())

internal fun swayOffsetPx(
    timeMs: Long,
    phase: Float,
    amplitudePx: Float,
): Float {
    val cyclePosition = (timeMs % SWAY_PERIOD_MS).toFloat() / SWAY_PERIOD_MS
    return sin(2f * PI.toFloat() * cyclePosition + phase) * amplitudePx
}
