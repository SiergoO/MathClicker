package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.ui.theme.Ink

private const val ICON_SIZE_FRACTION = 0.55f

@Composable
fun BoosterToken(
    booster: Booster,
    diameter: Dp,
    modifier: Modifier = Modifier,
    iconSizeFraction: Float = ICON_SIZE_FRACTION,
) {
    val style = boosterStyleFor(booster)
    BubbleSurface(
        style = dockBubbleStyle(rimColor = style.rimColor, diameter = diameter),
        modifier = modifier.size(diameter),
    ) {
        Icon(
            painter = painterResource(id = style.iconRes),
            contentDescription = stringResource(id = boosterLabelFor(booster)),
            tint = Ink,
            modifier = Modifier.fillMaxSize(iconSizeFraction),
        )
    }
}
