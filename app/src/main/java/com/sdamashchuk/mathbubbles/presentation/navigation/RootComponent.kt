package com.sdamashchuk.mathbubbles.presentation.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.sdamashchuk.mathbubbles.core.database.repository.GameRepository
import com.sdamashchuk.mathbubbles.core.game.Game
import com.sdamashchuk.mathbubbles.core.model.logging.Logger
import com.sdamashchuk.mathbubbles.core.ui.sound.SoundEventPlayer
import com.sdamashchuk.mathbubbles.core.ui.sound.SoundSettings
import com.sdamashchuk.mathbubbles.feature.game.GameComponent
import com.sdamashchuk.mathbubbles.feature.menu.MenuComponent
import com.sdamashchuk.mathbubbles.feature.menu.SettingsComponent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * The composition root, created once via `retainedComponent` in MainActivity so a rotation
 * mid-game does not restart the session. Only this component talks to Koin — every child below
 * takes its dependencies as constructor parameters, supplied here.
 */
@OptIn(DelicateDecomposeApi::class)
class RootComponent(
    componentContext: ComponentContext,
) : ComponentContext by componentContext,
    KoinComponent {
    private val navigation = StackNavigation<RootConfig>()

    val stack: Value<ChildStack<RootConfig, RootChild>> =
        childStack(
            source = navigation,
            serializer = RootConfig.serializer(),
            initialConfiguration = RootConfig.Menu,
            handleBackButton = true,
            childFactory = ::child,
        )

    fun handleBack() {
        navigation.pop()
    }

    private fun child(
        config: RootConfig,
        componentContext: ComponentContext,
    ): RootChild =
        when (config) {
            RootConfig.Menu -> {
                RootChild.Menu(
                    MenuComponent(
                        componentContext = componentContext,
                        gameRepository = get<GameRepository>(),
                        logger = get<Logger>(),
                        onPlayClicked = { navigation.push(RootConfig.Game) },
                        onSettingsClicked = { navigation.push(RootConfig.Settings) },
                    ),
                )
            }

            RootConfig.Game -> {
                RootChild.Game(
                    GameComponent(
                        componentContext = componentContext,
                        game = get<Game>(),
                        gameRepository = get<GameRepository>(),
                        logger = get<Logger>(),
                        // A fresh SoundEventPlayer per entry, not a shared one: soundModule
                        // registers it as a Koin factory precisely so GameComponent gets its own
                        // SoundPool to release on the way out, never one a previous session already
                        // released.
                        soundEventPlayer = get<SoundEventPlayer>(),
                        // Decompose requires unique configs in a stack, unlike the androidx.navigation
                        // backstack this replaces, so "back to menu" pops the existing Menu entry
                        // rather than pushing a second one. That is also a behaviour change worth
                        // naming: before, back from the menu re-showed the finished game and its
                        // Game Over dialog, and the stack grew by two entries per round. Now the
                        // menu is the bottom of the stack and back from it leaves the app.
                        onBackToMenu = { navigation.pop() },
                    ),
                )
            }

            RootConfig.Settings -> {
                RootChild.Settings(
                    SettingsComponent(
                        componentContext = componentContext,
                        soundSettings = get<SoundSettings>(),
                        onBackClicked = { navigation.pop() },
                    ),
                )
            }
        }
}
