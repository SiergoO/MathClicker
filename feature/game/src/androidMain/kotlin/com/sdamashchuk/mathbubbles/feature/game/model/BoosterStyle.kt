package com.sdamashchuk.mathbubbles.feature.game.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

data class BoosterStyle(
    val circleColor: Color,
    val iconColor: Color,
    @DrawableRes val iconRes: Int,
)
