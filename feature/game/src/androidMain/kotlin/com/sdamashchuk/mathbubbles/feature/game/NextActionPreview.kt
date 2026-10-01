package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdamashchuk.mathbubbles.core.model.FieldAction
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleRimReady
import com.sdamashchuk.mathbubbles.core.ui.theme.Ink

private val PREVIEW_SIZE = 48.dp

private const val PREVIEW_ALPHA = 0.8f

@Composable
fun NextActionPreview(
    action: FieldAction,
    modifier: Modifier = Modifier,
) {
    when (action) {
        is FieldAction.Operation -> {
            BubbleSurface(
                style = dockBubbleStyle(rimColor = BubbleRimReady, diameter = PREVIEW_SIZE),
                modifier = modifier.size(PREVIEW_SIZE).alpha(PREVIEW_ALPHA),
            ) {
                Text(
                    text = "${action.sign.sign}${action.digit}",
                    fontSize = 12.sp,
                    color = Ink,
                )
            }
        }

        is FieldAction.BoosterAction -> {
            BoosterToken(
                booster = action.booster,
                diameter = PREVIEW_SIZE,
                modifier = modifier.alpha(PREVIEW_ALPHA),
            )
        }
    }
}
