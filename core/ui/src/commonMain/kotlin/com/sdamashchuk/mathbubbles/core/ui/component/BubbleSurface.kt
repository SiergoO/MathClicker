package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sdamashchuk.mathbubbles.core.ui.component.model.BubbleStyle

@Composable
fun BubbleSurface(
    style: BubbleStyle,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier.bubbleSurface(style),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
