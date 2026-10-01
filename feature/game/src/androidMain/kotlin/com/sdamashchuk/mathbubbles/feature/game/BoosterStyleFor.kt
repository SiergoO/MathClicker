package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.ui.theme.Accent
import com.sdamashchuk.mathbubbles.core.ui.theme.AccentSoft
import com.sdamashchuk.mathbubbles.core.ui.theme.Ink
import com.sdamashchuk.mathbubbles.core.ui.theme.Success
import com.sdamashchuk.mathbubbles.feature.game.model.BoosterStyle

// The booster token is the bubble's rim, not a solid fill, so every icon sits on the same glass
// fill and reads at Ink regardless of which token rims it.
internal fun boosterStyleFor(booster: Booster): BoosterStyle =
    when (booster) {
        Booster.FREEZE -> BoosterStyle(AccentSoft, R.drawable.ic_booster_ac_unit)
        Booster.REWIND -> BoosterStyle(Accent, R.drawable.ic_booster_history)
        Booster.ICE_PICK -> BoosterStyle(Ink, R.drawable.ic_booster_focus_2)
        Booster.SHIELD -> BoosterStyle(Success, R.drawable.ic_booster_shield)
    }

internal fun boosterLabelFor(booster: Booster): Int =
    when (booster) {
        Booster.FREEZE -> R.string.booster_freeze
        Booster.REWIND -> R.string.booster_rewind
        Booster.ICE_PICK -> R.string.booster_ice_pick
        Booster.SHIELD -> R.string.booster_shield
    }
