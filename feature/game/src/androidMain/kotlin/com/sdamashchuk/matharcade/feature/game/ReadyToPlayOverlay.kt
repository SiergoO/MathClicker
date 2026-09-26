package com.sdamashchuk.matharcade.feature.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import com.sdamashchuk.matharcade.core.ui.theme.Red200
import com.sdamashchuk.matharcade.core.ui.theme.Translucent
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

private const val PROMPT_ANCHOR_HEIGHT_FRACTION = 0.5f

/**
 * GamePhase.ReadyToPlay's pre-game screen - a game that hasn't started can't be "paused", so this
 * carries no pause label. The tap-highlight animation plus the hint below it already say what to do.
 */
@Composable
fun ReadyToPlayOverlay(onClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Translucent)
                .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxHeight(PROMPT_ANCHOR_HEIGHT_FRACTION),
            contentAlignment = Alignment.Center,
        ) {
            val resources = LocalResources.current
            val composition by rememberLottieComposition {
                LottieCompositionSpec.DotLottie(resources.openRawResource(R.raw.tap_higlight).readBytes())
            }
            Image(
                painter = rememberLottiePainter(composition = composition, iterations = Compottie.IterateForever),
                contentDescription = null,
            )
        }
        Text(
            text = stringResource(id = R.string.ready_to_pay_overlay_hint).toUpperCase(Locale.current),
            style = MaterialTheme.typography.h2,
            color = Red200,
        )
    }
}
