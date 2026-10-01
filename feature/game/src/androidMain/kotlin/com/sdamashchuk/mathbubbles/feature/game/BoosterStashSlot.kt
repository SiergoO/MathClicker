package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.model.Booster

private val STASH_SLOT_VISUAL_SIZE = 36.dp
private val STASH_SLOT_TOUCH_SIZE = 48.dp

@Composable
fun BoosterStashSlot(
    booster: Booster?,
    slotIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isArmed: Boolean = false,
) {
    val armedLabel = stringResource(id = R.string.booster_ice_pick_armed)
    val label =
        if (isArmed) {
            armedLabel
        } else {
            booster?.let { stringResource(id = R.string.booster_stash_slot, slotIndex + 1) }
                ?: stringResource(id = R.string.booster_stash_slot_empty, slotIndex + 1)
        }
    val pulseScale = rememberPulseScale(enabled = isArmed)
    Box(
        modifier =
            modifier
                .size(STASH_SLOT_TOUCH_SIZE)
                .semantics { contentDescription = label }
                .let { if (booster != null) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center,
    ) {
        if (booster != null) {
            BoosterToken(
                booster = booster,
                diameter = STASH_SLOT_VISUAL_SIZE,
                modifier =
                    Modifier.graphicsLayer {
                        scaleX = pulseScale.value
                        scaleY = pulseScale.value
                    },
            )
        }
    }
}
