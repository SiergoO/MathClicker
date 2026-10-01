package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter

@Composable
fun NavBarAction(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavBarIconButton(
        painter = painter,
        contentDescription = contentDescription,
        onClick = onClick,
        iconAlignment = Alignment.CenterEnd,
        modifier = modifier,
    )
}
