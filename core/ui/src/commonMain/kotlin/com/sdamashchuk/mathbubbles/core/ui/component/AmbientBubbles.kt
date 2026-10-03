package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import com.sdamashchuk.mathbubbles.core.ui.component.model.AmbientBubbleStyle

/**
 * Rising bubbles drawn as a pure function of [timeMsProvider], which is read in the draw phase only.
 */
@Composable
fun AmbientBubbles(
    style: AmbientBubbleStyle,
    timeMsProvider: () -> Long,
    modifier: Modifier = Modifier,
) {
    Spacer(modifier.drawBehind { drawAmbientBubbles(style, timeMsProvider()) })
}
