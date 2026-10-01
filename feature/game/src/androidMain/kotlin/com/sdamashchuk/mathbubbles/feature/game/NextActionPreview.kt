package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdamashchuk.mathbubbles.core.model.FieldAction
import com.sdamashchuk.mathbubbles.core.ui.theme.AccentSoft
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
            Box(
                modifier =
                    modifier
                        .size(PREVIEW_SIZE)
                        .clip(CircleShape)
                        .alpha(PREVIEW_ALPHA)
                        .background(AccentSoft),
                contentAlignment = Alignment.Center,
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
