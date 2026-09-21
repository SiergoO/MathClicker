package com.sdomashchuk.mathclicker.presentation.navigation

import com.sdomashchuk.mathclicker.feature.game.GameComponent
import com.sdomashchuk.mathclicker.presentation.menu.MenuComponent
import com.sdomashchuk.mathclicker.presentation.splash.SplashComponent

sealed interface RootChild {
    data class Splash(
        val component: SplashComponent,
    ) : RootChild

    data class Menu(
        val component: MenuComponent,
    ) : RootChild

    data class Game(
        val component: GameComponent,
    ) : RootChild
}
