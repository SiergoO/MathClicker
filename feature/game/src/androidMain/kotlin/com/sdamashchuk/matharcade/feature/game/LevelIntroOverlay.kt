package com.sdamashchuk.matharcade.feature.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import com.sdamashchuk.matharcade.core.ui.theme.Accent
import com.sdamashchuk.matharcade.core.ui.theme.Scrim
import kotlinx.coroutines.delay

private const val FADE_IN_MS = 200
private const val HOLD_MS = 400
private const val FADE_OUT_MS = 200

/**
 * The 800ms `LEVEL N` announcement gating a level-up: no flag stops the fall while this is on
 * screen, the field composable simply isn't in this branch (see GameScreen).
 *
 * Accent, not Ink: the scrim went dark in MC-84, but the announcement still wants to read as the
 * game's own voice rather than as body text, and cyan on near-black is what does that.
 */
@Composable
fun LevelIntroOverlay(
    level: Int,
    onFinish: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = if (visible) FADE_IN_MS else FADE_OUT_MS),
        label = "levelIntroAlpha",
    )

    LaunchedEffect(Unit) {
        visible = true
        delay((FADE_IN_MS + HOLD_MS).toLong())
        visible = false
        delay(FADE_OUT_MS.toLong())
        onFinish()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Scrim)
                .alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(id = R.string.game_level_intro, level).toUpperCase(Locale.current),
            style = MaterialTheme.typography.h1,
            color = Accent,
        )
    }
}
