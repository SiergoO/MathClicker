package com.sdomashchuk.mathclicker.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.sdomashchuk.mathclicker.game.GameScreen
import com.sdomashchuk.mathclicker.menu.MenuScreen
import com.sdomashchuk.mathclicker.splash.SplashScreen

const val MENU_SCREEN_FADE_IN_DURATION = 500
const val MENU_SCREEN_FADE_OUT_DURATION = 200
const val GAME_SCREEN_FADE_IN_DURATION = 500
const val GAME_SCREEN_FADE_OUT_DURATION = 0

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SetupNavGraph(navController: NavHostController) {
    AnimatedNavHost(navController = navController, startDestination = Screen.Menu.route) {

        composable(
            route = Screen.Splash.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            SplashScreen(navController = navController)
        }

        composable(
            route = Screen.Menu.route,
            enterTransition = { fadeIn(animationSpec = keyframes { this.durationMillis = MENU_SCREEN_FADE_IN_DURATION }) },
            exitTransition = { fadeOut(animationSpec = keyframes { this.durationMillis = MENU_SCREEN_FADE_OUT_DURATION }) }
        ) {
            MenuScreen(navController = navController)
        }

        composable(
            route = Screen.Game.route,
            enterTransition = { fadeIn(animationSpec = keyframes { this.durationMillis = GAME_SCREEN_FADE_IN_DURATION }) },
            exitTransition = { fadeOut(animationSpec = keyframes { this.durationMillis = GAME_SCREEN_FADE_OUT_DURATION }) }
        ) {
            GameScreen(navController = navController)
        }
    }
}