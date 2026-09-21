package com.sdomashchuk.mathclicker.presentation.navigation

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.sdomashchuk.mathclicker.presentation.game.GameScreen
import com.sdomashchuk.mathclicker.presentation.menu.MenuScreen
import com.sdomashchuk.mathclicker.presentation.splash.SplashScreen

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun RootContent(root: RootComponent) {
    val screenFades =
        stackAnimation<RootConfig, RootChild> { child ->
            when (child.instance) {
                is RootChild.Splash -> fade()
                is RootChild.Menu -> asymmetricFade(MENU_SCREEN_FADE_IN_DURATION, MENU_SCREEN_FADE_OUT_DURATION)
                is RootChild.Game -> asymmetricFade(GAME_SCREEN_FADE_IN_DURATION, GAME_SCREEN_FADE_OUT_DURATION)
            }
        }

    ChildStackHost(
        stack = root.stack,
        backHandler = root.backHandler,
        onBack = root::handleBack,
        animation = screenFades,
    ) { child ->
        when (child) {
            is RootChild.Splash -> SplashScreen(component = child.component)
            is RootChild.Menu -> MenuScreen(component = child.component)
            is RootChild.Game -> GameScreen(component = child.component)
        }
    }
}
