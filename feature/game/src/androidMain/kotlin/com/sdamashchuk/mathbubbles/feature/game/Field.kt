package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.game.model.IcePickSource
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.INITIAL_LIFE_COUNT
import com.sdamashchuk.mathbubbles.core.ui.theme.Accent
import com.sdamashchuk.mathbubbles.core.ui.theme.AccentDeep
import com.sdamashchuk.mathbubbles.feature.game.model.TargetZeroedSignal
import kotlinx.collections.immutable.toImmutableList

private const val HUD_HEIGHT_FRACTION = 0.05f
private val SHIELD_MARK_SIZE = 16.dp

@Composable
fun Field(
    gameState: State<GameViewModel.State>,
    onTargetClicked: (id: Int) -> Unit,
    onFireClicked: () -> Unit,
    onTick: (elapsedMs: Int) -> Unit,
    onPauseClicked: () -> Unit,
    targetZeroedSignal: TargetZeroedSignal?,
    // The readiness hint's on/off switch, not a property of the game itself. A constant
    // today; once difficulty/mods exist, that's what supplies this value - TargetButton never sees
    // why a hint is off, and neither does this composable's own body beyond reading the flag.
    readinessHintsEnabled: Boolean = true,
    onStashBooster: () -> Unit = {},
    onApplyBoosterFromStash: (slotIndex: Int) -> Unit = {},
    onDisarmIcePick: () -> Unit = {},
) {
    // Every one of these used to be a `gameState.value.field.X` read straight in this
    // composable's body. GameViewModel.State is a fresh object on every tick because gameTimeMs
    // moved, so each of those reads dragged the whole HUD, the life stripes and the dock through
    // composition sixty times a second to render numbers that change once a second at most.
    val level by remember { derivedStateOf { gameState.value.field.level } }
    val score by remember { derivedStateOf { gameState.value.field.score } }
    val lifeCount by remember { derivedStateOf { gameState.value.field.lifeCount } }
    val appliedMultiplier by remember { derivedStateOf { gameState.value.field.appliedMultiplier } }
    val currentAction by remember { derivedStateOf { gameState.value.field.currentAction } }
    val nextAction by remember { derivedStateOf { gameState.value.field.nextAction } }
    val boosterStash by remember {
        derivedStateOf {
            gameState.value.field.boosterStash
                .toImmutableList()
        }
    }
    val timedBooster by remember { derivedStateOf { gameState.value.effects.timedBooster } }
    val shieldActive by remember { derivedStateOf { gameState.value.effects.shieldActive } }
    val icePickArmedFrom by remember { derivedStateOf { gameState.value.effects.icePickArmedFrom } }
    val countdownFraction = remember { { gameState.value.effects.remainingFraction } }

    LaunchedEffect(Unit) {
        var previousFrameNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { frameNanos ->
                val elapsedMs = ((frameNanos - previousFrameNanos) / 1_000_000L).toInt()
                previousFrameNanos = frameNanos
                onTick(elapsedMs)
            }
        }
    }

    Row(
        modifier =
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .fillMaxHeight(HUD_HEIGHT_FRACTION),
    ) {
        Text(
            modifier =
                Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
            textAlign = TextAlign.Center,
            text =
                stringResource(id = R.string.game_session_level, level).toUpperCase(Locale.current),
            style = MaterialTheme.typography.body1,
        )
        Text(
            modifier =
                Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
            textAlign = TextAlign.Center,
            text =
                scoreLabel(
                    appliedMultiplier = appliedMultiplier,
                    plainScore = stringResource(id = R.string.game_session_score, score),
                    comboScore = stringResource(id = R.string.game_session_score_combo, appliedMultiplier, score),
                ).toUpperCase(Locale.current),
            style = MaterialTheme.typography.body1,
        )
        // Fixed width, not weight(1f): Level and Score stay centred on each other regardless of
        // this button's presence.
        Box(
            modifier =
                Modifier
                    .width(56.dp)
                    .align(Alignment.CenterVertically),
            contentAlignment = Alignment.Center,
        ) {
            PauseButton(onClick = onPauseClicked)
        }
    }
    Divider()
    PlayArea(gameState, onTargetClicked, targetZeroedSignal, readinessHintsEnabled)

    Box(
        modifier =
            Modifier
                .navigationBarsPadding()
                .fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.Top,
        ) {
            repeat(calculateLifeSlotCount(lifeCount)) {
                Divider(color = if (it < lifeCount) Accent else AccentDeep, thickness = 3.dp)
                Spacer(modifier = Modifier.padding(bottom = 2.dp))
            }
        }
        if (shieldActive) {
            BoosterToken(
                booster = Booster.SHIELD,
                diameter = SHIELD_MARK_SIZE,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp),
            )
        }
        Row(
            modifier =
                Modifier
                    .navigationBarsPadding()
                    .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                BoosterStashRow(
                    stash = boosterStash,
                    armedSlotIndex = (icePickArmedFrom as? IcePickSource.StashSlot)?.index,
                    onSlotClicked = { slotIndex ->
                        if (icePickArmedFrom == IcePickSource.StashSlot(slotIndex)) {
                            onDisarmIcePick()
                        } else {
                            onApplyBoosterFromStash(slotIndex)
                        }
                    },
                )
            }
            Box(modifier = Modifier.width(FIRE_BUTTON_SIZE), contentAlignment = Alignment.Center) {
                val icePickArmedInFireButton = icePickArmedFrom == IcePickSource.FireButton
                FireButton(
                    action = currentAction,
                    countdownColor = timedBooster?.let { boosterStyleFor(it).rimColor },
                    countdownFraction = countdownFraction,
                    isIcePickArmedHere = icePickArmedInFireButton,
                    onFireClicked = if (icePickArmedInFireButton) onDisarmIcePick else onFireClicked,
                    onStashBooster = onStashBooster,
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NextActionPreview(action = nextAction)
            }
        }
    }
}

internal fun scoreLabel(
    appliedMultiplier: Int,
    plainScore: String,
    comboScore: String,
): String = if (appliedMultiplier > 1) comboScore else plainScore

// maxOf, not the bare cap: a lifeCount above INITIAL_LIFE_COUNT (a future random-event grant -
// see LifeBonusTest) must still get a slot, or the extra life stays invisible.
internal fun calculateLifeSlotCount(lifeCount: Int): Int = maxOf(INITIAL_LIFE_COUNT, lifeCount)
