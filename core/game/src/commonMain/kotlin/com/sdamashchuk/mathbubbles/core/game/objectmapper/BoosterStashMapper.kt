package com.sdamashchuk.mathbubbles.core.game.objectmapper

import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.Field

internal fun Field.stashCurrentBooster(booster: Booster): Field = copy(boosterStash = boosterStash + booster)

internal fun Field.freeStashSlot(index: Int): Field {
    check(index in boosterStash.indices) { "freeStashSlot($index) on a stash of size ${boosterStash.size}" }
    return copy(boosterStash = boosterStash.toMutableList().apply { removeAt(index) })
}
