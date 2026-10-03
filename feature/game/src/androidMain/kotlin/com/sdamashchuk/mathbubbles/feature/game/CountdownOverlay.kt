package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.ui.theme.Ink
import com.sdamashchuk.mathbubbles.core.ui.theme.Scrim
import io.github.alexzhirkevich.compottie.LottieCompositionResult
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.flow.first

@Composable
fun CountdownOverlay(
    compositionResult: LottieCompositionResult,
    onFinish: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxSize()
                .background(Scrim)
                .padding(40.dp),
    ) {
        val composition by compositionResult
        val progress by animateLottieCompositionAsState(composition)
        Image(
            painter = rememberLottiePainter(composition = composition, progress = { progress }),
            contentDescription = null,
            // The .lottie asset has its own colour baked in; this overrides it.
            colorFilter = ColorFilter.tint(Ink),
        )
        val currentOnFinish by rememberUpdatedState(onFinish)
        LaunchedEffect(Unit) {
            snapshotFlow { progress >= 1f }.first { it }
            currentOnFinish()
        }
    }
}
