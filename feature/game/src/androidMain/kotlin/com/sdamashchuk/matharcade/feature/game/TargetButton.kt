package com.sdamashchuk.matharcade.feature.game

import android.util.Size
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import com.sdamashchuk.matharcade.core.model.Target
import com.sdamashchuk.matharcade.core.ui.theme.Red200
import kotlin.math.roundToInt

@Composable
fun TargetButton(
    target: Target,
    gameColumnSize: Size,
    onTargetRevealed: (id: Int) -> Unit,
    onTargetClicked: (id: Int) -> Unit,
    onTargetDidBreakout: (id: Int) -> Unit,
    onTargetPositionSave: (id: Int, position: Int, gameColumnHeightPx: Int) -> Unit,
) {
    var isNeedToRefreshAnimation by remember { mutableStateOf(true) }
    var hasRevealedThisActivation by remember { mutableStateOf(false) }
    var hasBrokenOutThisActivation by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = target.appearanceDelayMs, key2 = target.isActive) {
        isNeedToRefreshAnimation = true
        hasRevealedThisActivation = false
        hasBrokenOutThisActivation = false
    }
    val infiniteTransition =
        if (!isNeedToRefreshAnimation && target.isActive) {
            rememberInfiniteTransition()
        } else {
            isNeedToRefreshAnimation = false
            null
        }
    val targetButtonYOffset =
        if (infiniteTransition != null) {
            val yOffset by infiniteTransition.animateFloat(
                initialValue = target.position.toFloat(),
                targetValue = gameColumnSize.height.toFloat(),
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            tween(
                                remainingFallDurationMs(target, gameColumnSize.height),
                                easing = LinearEasing,
                                delayMillis = target.appearanceDelayMs,
                            ),
                        repeatMode = RepeatMode.Restart,
                    ),
            )
            yOffset
        } else {
            0f
        }
    // Effect-scoped and flag-guarded so a burst of recompositions (e.g. across a pause/resume)
    // can't re-fire either callback for the same activation; each target.appearanceDelayMs/isActive
    // change above starts a new activation and re-arms both flags.
    LaunchedEffect(targetButtonYOffset) {
        if (shouldReveal(targetButtonYOffset, target.isVisible, hasRevealedThisActivation)) {
            hasRevealedThisActivation = true
            onTargetRevealed.invoke(target.id)
        }
        if (shouldBreakout(targetButtonYOffset, gameColumnSize.height, hasBrokenOutThisActivation)) {
            hasBrokenOutThisActivation = true
            onTargetDidBreakout.invoke(target.id)
        }
    }
    if (target.isActive && targetButtonYOffset.dp > 0.dp) {
        Button(
            modifier =
                Modifier
                    .width((gameColumnSize.width * 0.8).dp)
                    .height((gameColumnSize.width * 0.8).dp)
                    .offset(0.dp, targetButtonYOffset.dp)
                    .clip(CircleShape),
            colors =
                ButtonDefaults.buttonColors(
                    backgroundColor = if (target.isProfitable) Red200 else Color.LightGray,
                ),
            onClick = { onTargetClicked.invoke(target.id) },
        ) {
            Text(text = target.value.toString(), fontSize = 20.sp, color = Color.White)
        }
    }
    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                onTargetPositionSave.invoke(target.id, targetButtonYOffset.toInt(), gameColumnSize.height)
            }

            else -> { /* do nothing */ }
        }
    }
}

// target.lifetimeMs is the fall's original total duration (see updateTargetPositioning); the
// remaining distance from the last saved position is animated over the matching remaining share
// of it, so a pause/resume restart can't shrink the fall's real duration (MC-27).
// internal, not private: it is otherwise a pure function of its inputs, and TargetButtonLogicTest
// exercises it directly rather than through Compose.
internal fun remainingFallDurationMs(
    target: Target,
    gameColumnHeightPx: Int,
): Int =
    if (gameColumnHeightPx > 0) {
        (target.lifetimeMs * (1f - target.position.toFloat() / gameColumnHeightPx)).toInt().coerceAtLeast(0)
    } else {
        target.lifetimeMs
    }

internal fun shouldReveal(
    targetButtonYOffset: Float,
    isVisible: Boolean,
    hasRevealedThisActivation: Boolean,
): Boolean = targetButtonYOffset > 0f && !isVisible && !hasRevealedThisActivation

internal fun shouldBreakout(
    targetButtonYOffset: Float,
    gameColumnHeightPx: Int,
    hasBrokenOutThisActivation: Boolean,
): Boolean =
    gameColumnHeightPx != 0 &&
        targetButtonYOffset.roundToInt() + 1 >= gameColumnHeightPx &&
        !hasBrokenOutThisActivation
