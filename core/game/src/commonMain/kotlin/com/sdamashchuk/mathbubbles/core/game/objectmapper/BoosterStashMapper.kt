package com.sdamashchuk.mathbubbles.core.game.objectmapper

import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.Field

internal fun Field.stashCurrentBooster(booster: Booster): Field = copy(boosterStash = boosterStash + booster)

// index is validated by the caller (Game.applyBoosterFromStash never reaches here with one out of range).
internal fun Field.freeStashSlot(index: Int): Field =
    copy(boosterStash = boosterStash.toMutableList().apply { removeAt(index) })
