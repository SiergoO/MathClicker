package com.sdamashchuk.mathbubbles.presentation.navigation

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.sdamashchuk.mathbubbles.feature.game.GameScreen
import com.sdamashchuk.mathbubbles.feature.menu.MenuScreen
import com.sdamashchuk.mathbubbles.feature.menu.SettingsScreen
import com.sdamashchuk.mathbubbles.presentation.splash.SplashScreen

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun RootContent(root: RootComponent) {
    val screenFades =
        stackAnimation<RootConfig, RootChild> { child ->
            when (child.instance) {
                is RootChild.Splash -> fade()

                is RootChild.Menu -> asymmetricFade(MENU_SCREEN_FADE_IN_DURATION, MENU_SCREEN_FADE_OUT_DURATION)

                is RootChild.Game -> asymmetricFade(GAME_SCREEN_FADE_IN_DURATION, GAME_SCREEN_FADE_OUT_DURATION)

                // Reached from the menu the same way "how to play" is, so it shares the menu's
                // own fade rather than earning a third duration pair.
                is RootChild.Settings -> asymmetricFade(MENU_SCREEN_FADE_IN_DURATION, MENU_SCREEN_FADE_OUT_DURATION)
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
            is RootChild.Settings -> SettingsScreen(component = child.component)
        }
    }
}
