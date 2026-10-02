package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.game.model.IcePickSource
import com.sdamashchuk.mathbubbles.core.model.BOOSTER_STASH_CAPACITY
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.EFFECT_RAMP_MS
import com.sdamashchuk.mathbubbles.core.model.INITIAL_LIFE_COUNT
import com.sdamashchuk.mathbubbles.core.ui.theme.Accent
import com.sdamashchuk.mathbubbles.core.ui.theme.AccentDeep
import com.sdamashchuk.mathbubbles.feature.game.model.TargetZeroedSignal
import kotlinx.collections.immutable.toImmutableList

private val SHIELD_MARK_SIZE = 16.dp

@Composable
fun Field(
    gameState: State<GameViewModel.State>,
    onTargetClicked: (id: Int) -> Unit,
    onFireClicked: () -> Unit,
    onTick: (elapsedMs: Int) -> Unit,
    targetZeroedSignal: TargetZeroedSignal?,
    running: Boolean = true,
    // The division hint's on/off switch, not a property of the game itself. A constant
    // today; once difficulty/mods exist, that's what supplies this value - TargetButton never sees
    // why a hint is off, and neither does this composable's own body beyond reading the flag.
    divisionHintsEnabled: Boolean = true,
    onStashBooster: () -> Unit = {},
    onApplyBoosterFromStash: (slotIndex: Int) -> Unit = {},
    onDisarmIcePick: () -> Unit = {},
    shieldCracking: Boolean = false,
    onShieldCrackFinished: () -> Unit = {},
) {
    // Every one of these used to be a `gameState.value.field.X` read straight in this
    // composable's body. GameViewModel.State is a fresh object on every tick because gameTimeMs
    // moved, so each of those reads dragged the whole HUD, the life stripes and the dock through
    // composition sixty times a second to render numbers that change once a second at most.
    val lifeCount by remember { derivedStateOf { gameState.value.field.lifeCount } }
    val currentAction by remember { derivedStateOf { gameState.value.field.currentAction } }
    val nextAction by remember { derivedStateOf { gameState.value.field.nextAction } }
    val boosterStash by remember {
        derivedStateOf {
            gameState.value.field.boosterStash
                .toImmutableList()
        }
    }
    val firstFreeStashSlotIndex = boosterStash.size.takeIf { it < BOOSTER_STASH_CAPACITY }
    val timedBooster by remember { derivedStateOf { gameState.value.effects.timedBooster } }
    val shieldActive by remember { derivedStateOf { gameState.value.effects.shieldActive } }
    val icePickArmedFrom by remember { derivedStateOf { gameState.value.effects.icePickArmedFrom } }
    val countdownFraction = remember { { gameState.value.effects.remainingFraction } }
    val countdownAlpha = remember { { gameState.value.effects.intensity } }
    // Fades in on activation and fades out on a plain deactivation; an absorb skips this and
    // shows ShieldCrack instead, which owns the exit on its own.
    val shieldEnvelope by
        animateFloatAsState(
            targetValue = if (shieldActive) 1f else 0f,
            animationSpec = tween(EFFECT_RAMP_MS),
            label = "shieldEnvelope",
        )

    // Frame-clock elapsed real time, not gameTimeMs: the ice pick sway reads this so it keeps
    // moving while the game clock itself is frozen or reversed.
    var realElapsedMs by remember { mutableLongStateOf(0L) }
    val realTimeMsProvider = remember { { realElapsedMs } }
    // Keyed on running so a restart takes a fresh previousFrameNanos; otherwise the first frame
    // back would report the whole stopped interval as elapsed.
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var previousFrameNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { frameNanos ->
                val elapsedMs = ((frameNanos - previousFrameNanos) / 1_000_000L).toInt()
                previousFrameNanos = frameNanos
                realElapsedMs += elapsedMs
                onTick(elapsedMs)
            }
        }
    }

    Divider()
    PlayArea(gameState, onTargetClicked, targetZeroedSignal, divisionHintsEnabled, realTimeMsProvider)

    Box(
        modifier = Modifier.fillMaxSize(),
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
        if (shieldCracking) {
            ShieldCrack(
                diameter = SHIELD_MARK_SIZE,
                onFinished = onShieldCrackFinished,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp),
            )
        } else if (shieldEnvelope > 0f) {
            BoosterToken(
                booster = Booster.SHIELD,
                diameter = SHIELD_MARK_SIZE,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .graphicsLayer {
                            scaleX = shieldEnvelope
                            scaleY = shieldEnvelope
                            alpha = shieldEnvelope
                        },
            )
        }
        val stashSlotCentersX = remember { mutableStateMapOf<Int, Float>() }
        var fireButtonCenterX by remember { mutableFloatStateOf(0f) }
        Row(
            modifier =
                Modifier
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
                    onSlotPositioned = { slotIndex, centerX -> stashSlotCentersX[slotIndex] = centerX },
                )
            }
            Box(
                modifier =
                    Modifier
                        .width(FIRE_BUTTON_SIZE)
                        .onGloballyPositioned {
                            fireButtonCenterX = it.positionInRoot().x + it.size.width / 2f
                        },
                contentAlignment = Alignment.Center,
            ) {
                val icePickArmedInFireButton = icePickArmedFrom == IcePickSource.FireButton
                FireButton(
                    action = currentAction,
                    countdownColor = timedBooster?.let { boosterStyleFor(it).rimColor },
                    countdownFraction = countdownFraction,
                    countdownAlpha = countdownAlpha,
                    isIcePickArmedHere = icePickArmedInFireButton,
                    onFireClicked = if (icePickArmedInFireButton) onDisarmIcePick else onFireClicked,
                    onStashBooster = onStashBooster,
                    flightTargetOffsetPx = {
                        firstFreeStashSlotIndex
                            ?.let { stashSlotCentersX[it] }
                            ?.let { slotCenterX -> slotCenterX - fireButtonCenterX }
                    },
                    flightLandingDiameter = STASH_SLOT_VISUAL_SIZE,
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NextActionPreview(action = nextAction)
            }
        }
    }
}

// maxOf, not the bare cap: a lifeCount above INITIAL_LIFE_COUNT (a future random-event grant -
// see LifeBonusTest) must still get a slot, or the extra life stays invisible.
internal fun calculateLifeSlotCount(lifeCount: Int): Int = maxOf(INITIAL_LIFE_COUNT, lifeCount)
