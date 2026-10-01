package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.model.Booster

private const val ICON_SIZE_FRACTION = 0.55f

@Composable
fun BoosterToken(
    booster: Booster,
    diameter: Dp,
    modifier: Modifier = Modifier,
) {
    val style = boosterStyleFor(booster)
    Box(
        modifier =
            modifier
                .size(diameter)
                .background(style.circleColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = style.iconRes),
            contentDescription = stringResource(id = boosterLabelFor(booster)),
            tint = style.iconColor,
            modifier = Modifier.fillMaxSize(ICON_SIZE_FRACTION),
        )
    }
}
