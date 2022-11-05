package com.sdomashchuk.mathclicker.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.sdomashchuk.mathclicker.main.MainScreen
import com.sdomashchuk.mathclicker.menu.MenuScreen
import com.sdomashchuk.mathclicker.splash.SplashScreen

const val MENU_SCREEN_FADE_IN_DURATION = 2000

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SetupNavGraph(navController: NavHostController) {
    AnimatedNavHost(navController = navController, startDestination = Screen.Splash.route) {

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
            exitTransition = { fadeOut() }
        ) {
            MenuScreen(navController = navController)
        }

        composable(route = Screen.Main.route) {
            MainScreen(navController = navController)
        }
    }
}