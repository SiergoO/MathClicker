package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.ui.theme.Accent
import com.sdamashchuk.mathbubbles.core.ui.theme.AccentSoft
import com.sdamashchuk.mathbubbles.core.ui.theme.Ink
import com.sdamashchuk.mathbubbles.core.ui.theme.Success
import com.sdamashchuk.mathbubbles.core.ui.theme.Warning
import com.sdamashchuk.mathbubbles.core.ui.theme.WaterDeep
import com.sdamashchuk.mathbubbles.feature.game.model.BoosterStyle

// Ink only on Accent: on the lighter tokens it has too little contrast, so the icon is WaterDeep.
internal fun boosterStyleFor(booster: Booster): BoosterStyle =
    when (booster) {
        Booster.FREEZE -> BoosterStyle(AccentSoft, WaterDeep, R.drawable.ic_booster_ac_unit)
        Booster.REWIND -> BoosterStyle(Accent, Ink, R.drawable.ic_booster_history)
        Booster.ICE_PICK -> BoosterStyle(Warning, WaterDeep, R.drawable.ic_booster_pick)
        Booster.SHIELD -> BoosterStyle(Success, WaterDeep, R.drawable.ic_booster_shield)
    }

internal fun boosterLabelFor(booster: Booster): Int =
    when (booster) {
        Booster.FREEZE -> R.string.booster_freeze
        Booster.REWIND -> R.string.booster_rewind
        Booster.ICE_PICK -> R.string.booster_ice_pick
        Booster.SHIELD -> R.string.booster_shield
    }
