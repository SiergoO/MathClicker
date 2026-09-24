package com.sdamashchuk.matharcade.feature.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sdamashchuk.matharcade.core.ui.theme.Green200
import com.sdamashchuk.matharcade.core.ui.theme.Red700
import com.sdamashchuk.matharcade.core.ui.theme.White
import com.sdamashchuk.matharcade.feature.game.model.FeedbackEffect

// Both already have a screen of their own - LevelIntroOverlay for one, ResultsScreen (MC-53,
// formerly GameMenuDialog) for the other - so a banner here is a second announcement on top of the
// first. GameOver was verified doing exactly that on device: red "Game Over" from the banner,
// black "Game Over" from the results screen's own header, at the same time. They keep their
// haptic; only the text is suppressed.
private val silentEffects = setOf(FeedbackEffect.LevelUp, FeedbackEffect.GameOver)

private const val FADE_MS = 150
private const val BANNER_TOP_FRACTION = 0.12f
private const val BANNER_CORNER_DP = 8
private const val BANNER_PADDING_DP = 12

/**
 * A transient readout for the most recent [FeedbackEffect] - text and colour are presentation, so
 * they are derived here rather than carried on the model itself.
 */
@Composable
fun FeedbackBanner(effect: FeedbackEffect?) {
    val visible = effect != null && effect !in silentEffects
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = FADE_MS),
        label = "feedbackBannerAlpha",
    )
    val (text, color) = effect?.let { bannerContent(it) } ?: ("" to Color.Transparent)
    // Offset by a fraction rather than a dp padding: the HUD row above is itself sized as
    // fillMaxHeight(0.05f), so a fixed inset lands inside it on some densities - at 64.dp it
    // covered the score outright on a 1080x2424 screen.
    Column(
        modifier = Modifier.fillMaxSize().alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.fillMaxHeight(BANNER_TOP_FRACTION))
        // Opaque, not bare text: the board's own targets fall through exactly this band, and a
        // bare "+40 x5" over a target reads as part of that target's number.
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.h4,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .background(White, RoundedCornerShape(BANNER_CORNER_DP.dp))
                    .padding(horizontal = BANNER_PADDING_DP.dp),
        )
    }
}

@Composable
private fun bannerContent(effect: FeedbackEffect): Pair<String, Color> =
    when (effect) {
        is FeedbackEffect.TargetZeroed -> {
            stringResource(id = R.string.game_feedback_award, effect.awarded) to Green200
        }

        is FeedbackEffect.OperationResolved -> {
            if (effect.gained > 0) {
                stringResource(id = R.string.game_feedback_streak, effect.gained, effect.streak) to Green200
            } else {
                stringResource(id = R.string.game_feedback_miss) to Red700
            }
        }

        is FeedbackEffect.TargetBrokeOut -> {
            stringResource(id = R.string.game_feedback_life_lost) to Red700
        }

        FeedbackEffect.LevelUp -> {
            "" to Color.Transparent
        }

        is FeedbackEffect.LifeGranted -> {
            stringResource(id = R.string.game_feedback_life_gained) to Green200
        }

        FeedbackEffect.GameOver -> {
            "" to Color.Transparent
        }
    }
