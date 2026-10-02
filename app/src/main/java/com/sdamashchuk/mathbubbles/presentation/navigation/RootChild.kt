package com.sdamashchuk.mathbubbles.presentation.navigation

import com.sdamashchuk.mathbubbles.feature.game.GameComponent
import com.sdamashchuk.mathbubbles.feature.menu.MenuComponent
import com.sdamashchuk.mathbubbles.feature.menu.SettingsComponent

sealed interface RootChild {
    data class Menu(
        val component: MenuComponent,
    ) : RootChild

    data class Game(
        val component: GameComponent,
    ) : RootChild

    data class Settings(
        val component: SettingsComponent,
    ) : RootChild
}
