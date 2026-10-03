package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.sdamashchuk.mathbubbles.core.game.Game
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import com.sdamashchuk.mathbubbles.core.model.Target
import com.sdamashchuk.mathbubbles.core.model.logging.NoOpLogger
import com.sdamashchuk.mathbubbles.core.ui.sound.SilentModeChecker
import com.sdamashchuk.mathbubbles.core.ui.sound.SoundEventPlayer
import com.sdamashchuk.mathbubbles.core.ui.sound.SoundSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random

private const val SEED = 20_261_003
private const val FIXED_FINISHED_AT_MS = 1_700_000_000_000L
private const val FRAME_MS = 16L

/**
 * The real component, view model and engine over a recording repository. A [savedField] is restored
 * paused with [savedTargets] on the board; without one the session starts fresh on the ready screen.
 */
class GameScreenFixture(
    savedField: Field? = null,
    savedTargets: List<Target> = emptyList(),
    earlierFields: List<Field> = emptyList(),
) {
    val repository =
        RecordingGameRepository(
            initialFields = earlierFields + listOfNotNull(savedField),
            targets = savedTargets,
        )
    val sounds = RecordingSoundPlayer()
    var menuExits = 0
        private set

    private val game =
        Game(
            sessionHelper = SteadySessionHelper(),
            scope = CoroutineScope(Dispatchers.Unconfined),
            random = Random(SEED),
            clock = FixedClock(FIXED_FINISHED_AT_MS),
        ).apply { start() }

    val component =
        GameComponent(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            game = game,
            gameRepository = repository,
            logger = NoOpLogger,
            soundEventPlayer =
                SoundEventPlayer(
                    player = sounds,
                    settings =
                        object : SoundSettings {
                            override fun isSoundEnabled() = true

                            override fun setSoundEnabled(enabled: Boolean) = Unit
                        },
                    silentMode =
                        object : SilentModeChecker {
                            override fun isSilent() = false
                        },
                ),
            onBackToMenu = { menuExits++ },
        )

    val state: GameViewModel.State get() = component.state.value

    fun show(rule: ComposeContentTestRule) {
        rule.mainClock.autoAdvance = false
        rule.setContent { GameScreen(component) }
        rule.mainClock.advanceTimeBy(FRAME_MS)
    }

    fun startPlaying() {
        component.sendAction(GameViewModel.Action.ReadyToPlayButtonClicked)
        component.sendAction(GameViewModel.Action.StartGame)
    }

    companion object {
        const val FINISHED_AT_MS = FIXED_FINISHED_AT_MS

        fun openField(
            score: Int = 0,
            level: Int = 1,
            lifeCount: Int = 3,
            currentBooster: Booster? = null,
            boosterStash: List<Booster> = emptyList(),
        ) = Field(
            id = 1,
            level = level,
            score = score,
            lifeCount = lifeCount,
            currentOperationSign = OperationSign.DIVISION,
            currentOperationDigit = 2,
            nextOperationSign = OperationSign.SUBTRACTION,
            nextOperationDigit = 3,
            currentBooster = currentBooster,
            boosterStash = boosterStash,
            gameTimeMs = 5_000,
        )
    }
}
