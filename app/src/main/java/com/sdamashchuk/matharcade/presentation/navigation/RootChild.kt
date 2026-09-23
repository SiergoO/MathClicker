package com.sdamashchuk.matharcade.presentation.navigation

import com.sdamashchuk.matharcade.feature.game.GameComponent
import com.sdamashchuk.matharcade.feature.menu.MenuComponent
import com.sdamashchuk.matharcade.presentation.splash.SplashComponent

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
