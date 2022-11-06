package com.sdomashchuk.mathclicker.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.sdomashchuk.mathclicker.R
import com.sdomashchuk.mathclicker.navigation.Screen
import kotlinx.coroutines.cancel

@Composable
fun SplashScreen(navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.logo))
        val progress by animateLottieCompositionAsState(
            composition,
            iterations = 1,
            speed = 1f
        )
        LottieAnimation(composition)
        LaunchedEffect(progress) {
            if (progress >= 1f) {
                navController.navigate(Screen.Menu.route)
            }
        }
    }
}