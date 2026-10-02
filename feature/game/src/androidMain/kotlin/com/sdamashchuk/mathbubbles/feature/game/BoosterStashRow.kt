package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.model.Booster
import kotlinx.collections.immutable.ImmutableList

private const val STASH_SLOT_COUNT = 3

// Touch targets overlap by 8dp on purpose: three 48dp slots must fit the dock's left third.
private val STASH_SLOT_OVERLAP = (-8).dp

@Composable
fun BoosterStashRow(
    stash: ImmutableList<Booster>,
    onSlotClicked: (slotIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    armedSlotIndex: Int? = null,
    onSlotPositioned: (slotIndex: Int, centerXInRoot: Float) -> Unit = { _, _ -> },
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(STASH_SLOT_OVERLAP),
    ) {
        repeat(STASH_SLOT_COUNT) { slotIndex ->
            BoosterStashSlot(
                booster = stash.getOrNull(slotIndex),
                slotIndex = slotIndex,
                onClick = { onSlotClicked(slotIndex) },
                isArmed = slotIndex == armedSlotIndex,
                onPositioned = onSlotPositioned,
            )
        }
    }
}
