package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleRimReady

const val GAME_HUD_COMBO_SLOT_TAG = "GameHudComboSlot"
const val GAME_HUD_COMBO_VALUE_TAG = "GameHudComboValue"

private val COMBO_BUBBLE_SIZE = 28.dp
private const val APPEAR_ANIMATION_MS = 180
private const val STEP_ANIMATION_MS = 140
private const val STEP_PULSE_SCALE = 1.25f

@Composable
fun GameHudCombo(
    appliedMultiplier: Int,
    modifier: Modifier = Modifier,
) {
    val visible = appliedMultiplier > 1
    var rendered by remember { mutableStateOf(visible) }
    var displayedMultiplier by remember { mutableStateOf(appliedMultiplier) }
    if (visible) displayedMultiplier = appliedMultiplier
    val appear = remember { Animatable(if (visible) 1f else 0f) }
    val pulse = remember { Animatable(1f) }

    LaunchedEffect(visible) {
        if (visible) rendered = true
        appear.animateTo(if (visible) 1f else 0f, tween(APPEAR_ANIMATION_MS))
        if (!visible) rendered = false
    }
    LaunchedEffect(appliedMultiplier) {
        if (visible) {
            pulse.snapTo(STEP_PULSE_SCALE)
            pulse.animateTo(1f, tween(STEP_ANIMATION_MS))
        }
    }

    Box(modifier = modifier.size(COMBO_BUBBLE_SIZE).testTag(GAME_HUD_COMBO_SLOT_TAG)) {
        if (rendered) {
            BubbleSurface(
                style = dockBubbleStyle(rimColor = BubbleRimReady, diameter = COMBO_BUBBLE_SIZE),
                modifier =
                    Modifier.size(COMBO_BUBBLE_SIZE).graphicsLayer {
                        val scale = appear.value * pulse.value
                        scaleX = scale
                        scaleY = scale
                        alpha = appear.value
                    },
            ) {
                Text(
                    modifier = Modifier.testTag(GAME_HUD_COMBO_VALUE_TAG),
                    text = stringResource(id = R.string.game_session_combo_value, displayedMultiplier),
                    style = MaterialTheme.typography.caption,
                )
            }
        }
    }
}
