package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdamashchuk.mathbubbles.core.model.FieldAction
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleRimReady
import com.sdamashchuk.mathbubbles.core.ui.theme.Ink
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal val FIRE_BUTTON_SIZE = 100.dp
private val SWIPE_STASH_THRESHOLD = 48.dp
private val SWIPE_RESISTANCE_RANGE = 120.dp
private const val FLING_VELOCITY_THRESHOLD_DP_PER_S = -1200f

// Under-damped on purpose: the spring's own overshoot is the landing settle.
private const val FLIGHT_SPRING_DAMPING_RATIO = 0.6f

@Composable
fun FireButton(
    action: FieldAction,
    countdownColor: Color?,
    countdownFraction: () -> Float,
    onFireClicked: () -> Unit,
    onStashBooster: () -> Unit,
    isIcePickArmedHere: Boolean = false,
    modifier: Modifier = Modifier,
    countdownAlpha: () -> Float = { 1f },
    // Null means no measured destination yet, or the stash is full: the release resets in place.
    flightTargetOffsetPx: () -> Float? = { null },
    flightLandingDiameter: Dp = FIRE_BUTTON_SIZE,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dragOffsetPx = remember { Animatable(0f) }
    val flightOffsetPx = remember { Animatable(0f) }
    val flightScale = remember { Animatable(1f) }
    var isFlying by remember { mutableStateOf(false) }
    val resistanceRangePx = with(density) { SWIPE_RESISTANCE_RANGE.toPx() }
    val thresholdPx = with(density) { -SWIPE_STASH_THRESHOLD.toPx() }
    val flingThresholdPxPerS = with(density) { FLING_VELOCITY_THRESHOLD_DP_PER_S.dp.toPx() }
    val landingScale = flightLandingDiameter / FIRE_BUTTON_SIZE
    val isBooster = action is FieldAction.BoosterAction
    val armedLabel = stringResource(id = R.string.booster_ice_pick_armed)

    // A promotion can detach the drag mid-gesture before any flight starts, so this resets too;
    // a flight's own landing always resets itself regardless of whether action ends up changing.
    LaunchedEffect(action) {
        dragOffsetPx.snapTo(0f)
        flightOffsetPx.snapTo(0f)
        flightScale.snapTo(1f)
        isFlying = false
    }

    val draggableState =
        rememberDraggableState { delta ->
            scope.launch {
                dragOffsetPx.snapTo((dragOffsetPx.value + delta).coerceAtMost(0f))
            }
        }

    Box(
        modifier =
            modifier
                .size(FIRE_BUTTON_SIZE)
                .offset {
                    val px =
                        if (isFlying) {
                            flightOffsetPx.value
                        } else {
                            applyDragResistance(dragOffsetPx.value, resistanceRangePx)
                        }
                    IntOffset(px.roundToInt(), 0)
                }.graphicsLayer {
                    scaleX = flightScale.value
                    scaleY = flightScale.value
                }.draggable(
                    orientation = Orientation.Horizontal,
                    state = draggableState,
                    enabled = isBooster && !isIcePickArmedHere && !isFlying,
                    onDragStopped = { velocity ->
                        handleFireButtonDragStopped(
                            velocity = velocity,
                            dragOffsetPx = dragOffsetPx,
                            flightOffsetPx = flightOffsetPx,
                            flightScale = flightScale,
                            resistanceRangePx = resistanceRangePx,
                            thresholdPx = thresholdPx,
                            flingThresholdPxPerS = flingThresholdPxPerS,
                            isBooster = isBooster,
                            landingScale = landingScale,
                            flightTargetOffsetPx = flightTargetOffsetPx,
                            setFlying = { isFlying = it },
                            onStashBooster = onStashBooster,
                        )
                    },
                ).clip(CircleShape)
                .then(if (isIcePickArmedHere) Modifier.semantics { contentDescription = armedLabel } else Modifier)
                .clickable(enabled = !isFlying, onClick = onFireClicked),
        contentAlignment = Alignment.Center,
    ) {
        when (action) {
            is FieldAction.Operation -> {
                BubbleSurface(
                    style = dockBubbleStyle(rimColor = BubbleRimReady, diameter = FIRE_BUTTON_SIZE),
                    modifier = Modifier.size(FIRE_BUTTON_SIZE),
                ) {
                    Text(
                        text = "${action.sign.sign}${action.digit}",
                        fontSize = 36.sp,
                        color = Ink,
                        style = MaterialTheme.typography.button,
                    )
                }
            }

            is FieldAction.BoosterAction -> {
                BoosterToken(booster = action.booster, diameter = FIRE_BUTTON_SIZE)
            }
        }
        if (countdownColor != null) {
            CountdownRing(color = countdownColor, fraction = countdownFraction, alpha = countdownAlpha)
        }
    }
}

// isBooster, not just the distance: a promotion that disables the drag mid-gesture cancels it here
// too, and that stop must reset in place, never fly, same as an under-threshold release.
private suspend fun handleFireButtonDragStopped(
    velocity: Float,
    dragOffsetPx: Animatable<Float, AnimationVector1D>,
    flightOffsetPx: Animatable<Float, AnimationVector1D>,
    flightScale: Animatable<Float, AnimationVector1D>,
    resistanceRangePx: Float,
    thresholdPx: Float,
    flingThresholdPxPerS: Float,
    isBooster: Boolean,
    landingScale: Float,
    flightTargetOffsetPx: () -> Float?,
    setFlying: (Boolean) -> Unit,
    onStashBooster: () -> Unit,
) {
    val visualOffsetPx = applyDragResistance(dragOffsetPx.value, resistanceRangePx)
    val stashed = isBooster && shouldStashOnRelease(visualOffsetPx, velocity, thresholdPx, flingThresholdPxPerS)
    val flightTargetPx = if (stashed) flightTargetOffsetPx() else null
    if (flightTargetPx == null) {
        if (stashed) onStashBooster()
        dragOffsetPx.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        return
    }
    setFlying(true)
    flightOffsetPx.snapTo(visualOffsetPx)
    var flightCompleted = false
    try {
        // A snapTo elsewhere cancels only the child animation and coroutineScope still returns
        // normally, so completion is read from the end reason, not from reaching this line.
        coroutineScope {
            launch { flightScale.animateTo(landingScale, spring(dampingRatio = FLIGHT_SPRING_DAMPING_RATIO)) }
            flightCompleted =
                flightOffsetPx
                    .animateTo(flightTargetPx, spring(dampingRatio = FLIGHT_SPRING_DAMPING_RATIO))
                    .endReason == AnimationEndReason.Finished
        }
    } finally {
        setFlying(false)
        dragOffsetPx.snapTo(0f)
        flightOffsetPx.snapTo(0f)
        flightScale.snapTo(1f)
    }
    if (flightCompleted) onStashBooster()
}

internal fun applyDragResistance(
    rawOffsetPx: Float,
    resistanceRangePx: Float,
): Float {
    if (rawOffsetPx >= 0f) return 0f
    val magnitude = -rawOffsetPx
    return -(resistanceRangePx * magnitude / (resistanceRangePx + magnitude))
}

internal fun shouldStashOnRelease(
    visualOffsetPx: Float,
    velocityPxPerS: Float,
    thresholdPx: Float,
    flingVelocityThresholdPxPerS: Float,
): Boolean = visualOffsetPx <= thresholdPx || velocityPxPerS <= flingVelocityThresholdPxPerS
