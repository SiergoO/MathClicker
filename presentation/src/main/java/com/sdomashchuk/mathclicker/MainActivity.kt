package com.sdomashchuk.mathclicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.sdomashchuk.mathclicker.navigation.SetupNavGraph
import com.sdomashchuk.mathclicker.ui.theme.MathClickerTheme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MathClickerTheme {
                val navController = rememberAnimatedNavController()
                SetupNavGraph(navController = navController)
            }
        }
    }
}