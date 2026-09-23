package com.sdamashchuk.matharcade.presentation.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.sdamashchuk.matharcade.core.database.repository.GameRepository
import com.sdamashchuk.matharcade.core.game.Game
import com.sdamashchuk.matharcade.feature.game.GameComponent
import com.sdamashchuk.matharcade.feature.menu.MenuComponent
import com.sdamashchuk.matharcade.presentation.splash.SplashComponent
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
            initialConfiguration = RootConfig.Splash,
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
            RootConfig.Splash -> {
                RootChild.Splash(
                    SplashComponent(
                        componentContext = componentContext,
                        // replaceAll, not push: the splash is an entrance, not somewhere to go
                        // back to. Pushing left it under the menu, so back from the menu
                        // replayed the logo and pushed the menu again instead of leaving.
                        onFinished = { navigation.replaceAll(RootConfig.Menu) },
                    ),
                )
            }

            RootConfig.Menu -> {
                RootChild.Menu(
                    MenuComponent(
                        componentContext = componentContext,
                        onPlayClicked = { navigation.push(RootConfig.Game) },
                    ),
                )
            }

            RootConfig.Game -> {
                RootChild.Game(
                    GameComponent(
                        componentContext = componentContext,
                        game = get<Game>(),
                        gameRepository = get<GameRepository>(),
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
        }
}
