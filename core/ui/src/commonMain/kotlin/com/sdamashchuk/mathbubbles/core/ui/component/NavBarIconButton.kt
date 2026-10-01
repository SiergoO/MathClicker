package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.ui.theme.Accent

internal val NavBarIconButtonTouchTarget = 48.dp
internal val NavBarIconSize = 24.dp

@Composable
internal fun NavBarIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    iconAlignment: Alignment,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(NavBarIconButtonTouchTarget),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = iconAlignment) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.size(NavBarIconSize),
                tint = Accent,
            )
        }
    }
}
