package com.sdamashchuk.mathbubbles.feature.game

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.sdamashchuk.mathbubbles.core.component.viewModel
import com.sdamashchuk.mathbubbles.core.database.repository.GameRepository
import com.sdamashchuk.mathbubbles.core.game.Game
import com.sdamashchuk.mathbubbles.core.model.logging.Logger
import com.sdamashchuk.mathbubbles.core.ui.sound.SoundEventPlayer
import com.sdamashchuk.mathbubbles.core.ui.sound.model.SoundSample
import com.sdamashchuk.mathbubbles.feature.game.model.FeedbackEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Owns navigation for the game screen: forwards GameViewModel's post-game "back to menu" event to
 * [onBackToMenu] instead of leaving the composable to collect it off a NavController.
 *
 * [soundEventPlayer] is this component's, not shared: RootComponent asks Koin for a fresh one on
 * every entry to the game screen (see its `factory` registration in `soundModule`), and it is
 * released below rather than left for the next session to inherit an already-released SoundPool.
 */
class GameComponent(
    componentContext: ComponentContext,
    game: Game,
    gameRepository: GameRepository,
    logger: Logger,
    private val soundEventPlayer: SoundEventPlayer,
    private val onBackToMenu: () -> Unit,
) : ComponentContext by componentContext {
    private val viewModel: GameViewModel = viewModel { GameViewModel(game, gameRepository, logger) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val state: StateFlow<GameViewModel.State> = viewModel.state
    val feedback: ReceiveChannel<FeedbackEffect> = viewModel.feedback

    init {
        lifecycle.doOnDestroy {
            scope.cancel()
            soundEventPlayer.release()
        }
        scope.launch {
            viewModel.uiEvents.receiveAsFlow().collect { event ->
                when (event) {
                    GameViewModel.UiEvent.NavigateToMainMenuScreen -> onBackToMenu()
                }
            }
        }
    }

    fun sendAction(action: GameViewModel.Action) = viewModel.sendAction(action)

    fun playSound(sample: SoundSample) = soundEventPlayer.play(sample)
}
