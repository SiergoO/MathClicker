package com.sdamashchuk.mathbubbles.core.game.objectmapper

import com.sdamashchuk.mathbubbles.core.game.model.IcePickSource
import com.sdamashchuk.mathbubbles.core.game.model.TimedBoosterEffect
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.Field

// The Field <-> Game-private-state translation for persisted effects, kept as pure functions so
// Game.kt only has to call them, not carry the mapping logic itself.

internal fun Field.restoredTimedEffect(): TimedBoosterEffect? =
    timedEffectBooster?.let { TimedBoosterEffect(it, timedEffectRemainingMs, timedEffectRate) }

internal fun Field.restoredIcePickSource(): IcePickSource? {
    val armedStashIndex = icePickArmedStashIndex
    return when {
        icePickArmedFireButton -> {
            IcePickSource.FireButton
        }

        armedStashIndex != null && boosterStash.getOrNull(armedStashIndex) == Booster.ICE_PICK -> {
            IcePickSource.StashSlot(armedStashIndex)
        }

        else -> {
            null
        }
    }
}

internal fun Field.withEffectColumns(
    timedEffect: TimedBoosterEffect?,
    freezeTintEnvelope: Double,
    icePickArmedFrom: IcePickSource?,
    shieldActive: Boolean,
    neutralRate: Double,
): Field =
    copy(
        timedEffectBooster = timedEffect?.booster,
        timedEffectRemainingMs = timedEffect?.remainingRealMs ?: 0,
        timedEffectRate = timedEffect?.rate ?: neutralRate,
        freezeTintEnvelope = freezeTintEnvelope,
        icePickArmedFireButton = icePickArmedFrom == IcePickSource.FireButton,
        icePickArmedStashIndex = (icePickArmedFrom as? IcePickSource.StashSlot)?.index,
        shieldActive = shieldActive,
    )
