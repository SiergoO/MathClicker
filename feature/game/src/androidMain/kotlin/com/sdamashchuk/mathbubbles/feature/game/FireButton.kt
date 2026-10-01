package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdamashchuk.mathbubbles.core.model.FieldAction
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleRimReady
import com.sdamashchuk.mathbubbles.core.ui.theme.Ink
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal val FIRE_BUTTON_SIZE = 100.dp
private val SWIPE_STASH_THRESHOLD = 48.dp
private const val FLING_VELOCITY_THRESHOLD_DP_PER_S = -1200f

@Composable
fun FireButton(
    action: FieldAction,
    countdownColor: Color?,
    countdownFraction: () -> Float,
    onFireClicked: () -> Unit,
    onStashBooster: () -> Unit,
    isIcePickArmedHere: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dragOffsetPx = remember { Animatable(0f) }
    val thresholdPx = with(density) { -SWIPE_STASH_THRESHOLD.toPx() }
    val flingThresholdPxPerS = with(density) { FLING_VELOCITY_THRESHOLD_DP_PER_S.dp.toPx() }
    val isBooster = action is FieldAction.BoosterAction
    val pulseScale = rememberPulseScale(enabled = isIcePickArmedHere)
    val armedLabel = stringResource(id = R.string.booster_ice_pick_armed)

    // A promotion can detach the drag mid-animation, so the offset is reset here, not in the drag.
    LaunchedEffect(action) {
        dragOffsetPx.snapTo(0f)
    }

    val draggableState =
        rememberDraggableState { delta ->
            scope.launch {
                dragOffsetPx.snapTo((dragOffsetPx.value + delta).coerceIn(thresholdPx, 0f))
            }
        }

    Box(
        modifier =
            modifier
                .size(FIRE_BUTTON_SIZE)
                .offset { IntOffset(dragOffsetPx.value.roundToInt(), 0) }
                .graphicsLayer {
                    scaleX = pulseScale.value
                    scaleY = pulseScale.value
                }.draggable(
                    orientation = Orientation.Horizontal,
                    state = draggableState,
                    enabled = isBooster && !isIcePickArmedHere,
                    onDragStopped = { velocity ->
                        val stashed =
                            dragOffsetPx.value <= thresholdPx || velocity <= flingThresholdPxPerS
                        if (stashed) onStashBooster()
                        dragOffsetPx.animateTo(0f)
                    },
                ).clip(CircleShape)
                .then(if (isIcePickArmedHere) Modifier.semantics { contentDescription = armedLabel } else Modifier)
                .clickable(onClick = onFireClicked),
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
            CountdownRing(color = countdownColor, fraction = countdownFraction)
        }
    }
}
