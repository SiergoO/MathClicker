package com.sdamashchuk.mathbubbles.feature.menu

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdamashchuk.mathbubbles.core.ui.component.BubbleSurface
import com.sdamashchuk.mathbubbles.core.ui.component.targetBubbleStyle
import com.sdamashchuk.mathbubbles.core.ui.theme.Ink

internal const val LOGO_DIAMETER_SCREEN_FRACTION = 0.62f
internal const val LOGO_TEXT_WIDTH_FRACTION = 0.72f
private const val MID_FIELD_HORIZONTAL_FRACTION = 0f

private val WORDMARK_AUTO_SIZE = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 48.sp)

@Composable
fun MenuLogo(
    timeMsProvider: () -> Long,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(id = R.string.logo_content_description)
    val density = LocalDensity.current
    val screenWidth =
        with(density) {
            LocalWindowInfo.current.containerSize.width
                .toDp()
        }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val diameter = minOf(screenWidth * LOGO_DIAMETER_SCREEN_FRACTION, maxWidth, maxHeight)
        val style = remember(diameter) { targetBubbleStyle(diameter, true, MID_FIELD_HORIZONTAL_FRACTION) }

        BubbleSurface(
            style = style,
            modifier =
                Modifier
                    .size(diameter)
                    .graphicsLayer {
                        val timeMs = timeMsProvider()
                        val scale = MenuLogoMotion.breathScale(timeMs)
                        scaleX = scale
                        scaleY = scale
                        translationY = MenuLogoMotion.bobFraction(timeMs) * MenuLogoMotion.BOB_AMPLITUDE_DP.dp.toPx()
                    }.clearAndSetSemantics { contentDescription = description },
        ) {
            BasicText(
                text =
                    stringResource(id = R.string.logo_wordmark_line_1) + "\n" +
                        stringResource(id = R.string.logo_wordmark_line_2),
                modifier = Modifier.fillMaxWidth(LOGO_TEXT_WIDTH_FRACTION),
                style = MaterialTheme.typography.h1.copy(color = Ink, textAlign = TextAlign.Center),
                maxLines = 2,
                autoSize = WORDMARK_AUTO_SIZE,
            )
        }
    }
}
