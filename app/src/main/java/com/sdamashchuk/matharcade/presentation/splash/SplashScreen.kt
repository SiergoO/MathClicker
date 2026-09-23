package com.sdamashchuk.matharcade.presentation.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import com.sdamashchuk.matharcade.R
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

@Composable
fun SplashScreen(component: SplashComponent) {
    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
    ) {
        val resources = LocalResources.current
        val composition by rememberLottieComposition {
            LottieCompositionSpec.DotLottie(resources.openRawResource(R.raw.logo).readBytes())
        }
        val progress by animateLottieCompositionAsState(
            composition,
            iterations = 1,
            speed = 1f,
        )
        Image(
            painter = rememberLottiePainter(composition = composition, progress = { progress }),
            contentDescription = null,
        )
        LaunchedEffect(progress) {
            if (progress >= 1f) {
                component.onFinished()
            }
        }
    }
}
