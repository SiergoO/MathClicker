package com.sdamashchuk.matharcade.feature.game

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdamashchuk.matharcade.core.model.INITIAL_LIFE_COUNT
import com.sdamashchuk.matharcade.core.ui.theme.Accent
import com.sdamashchuk.matharcade.core.ui.theme.AccentDeep
import com.sdamashchuk.matharcade.core.ui.theme.AccentSoft
import com.sdamashchuk.matharcade.core.ui.theme.Ink
import com.sdamashchuk.matharcade.feature.game.model.TargetZeroedSignal

private const val HUD_HEIGHT_FRACTION = 0.05f

// The next-operation button is a preview, not a control - dimmed so it reads as one.
private const val NEXT_OPERATION_PREVIEW_ALPHA = 0.8f

@Composable
fun Field(
    gameState: State<GameViewModel.State>,
    onTargetClicked: (id: Int) -> Unit,
    onFireClicked: () -> Unit,
    onTick: (elapsedMs: Int) -> Unit,
    onPauseClicked: () -> Unit,
    targetZeroedSignal: TargetZeroedSignal?,
    // MC-61: the readiness hint's on/off switch, not a property of the game itself. A constant
    // today; once difficulty/mods exist, that's what supplies this value - TargetButton never sees
    // why a hint is off, and neither does this composable's own body beyond reading the flag.
    readinessHintsEnabled: Boolean = true,
) {
    // MC-85: every one of these used to be a `gameState.value.field.X` read straight in this
    // composable's body. GameViewModel.State is a fresh object on every tick because gameTimeMs
    // moved, so each of those reads dragged the whole HUD, the life stripes and the dock through
    // composition sixty times a second to render numbers that change once a second at most.
    val level by remember { derivedStateOf { gameState.value.field.level } }
    val score by remember { derivedStateOf { gameState.value.field.score } }
    val lifeCount by remember { derivedStateOf { gameState.value.field.lifeCount } }
    val appliedMultiplier by remember { derivedStateOf { gameState.value.field.appliedMultiplier } }
    val currentOperation by
        remember {
            derivedStateOf {
                gameState.value.field.let { "${it.currentOperationSign.sign}${it.currentOperationDigit}" }
            }
        }
    val nextOperation by
        remember {
            derivedStateOf {
                gameState.value.field.let { "${it.nextOperationSign.sign}${it.nextOperationDigit}" }
            }
        }

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
                stringResource(id = R.string.game_session_score, score).toUpperCase(Locale.current),
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
        Row(
            modifier =
                Modifier
                    .navigationBarsPadding()
                    .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text =
                    stringResource(id = R.string.game_session_combo, appliedMultiplier)
                        .toUpperCase(Locale.current),
                style = MaterialTheme.typography.h2,
            )
            Button(
                onClick = onFireClicked,
                modifier =
                    Modifier
                        .weight(1f)
                        .wrapContentSize()
                        .clip(CircleShape)
                        .width(100.dp)
                        .height(100.dp),
            ) {
                Text(
                    text = currentOperation,
                    fontSize = 36.sp,
                    color = Ink,
                )
            }
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentSoft),
                modifier =
                    Modifier
                        .weight(1f)
                        .wrapContentSize()
                        .clip(CircleShape)
                        .width(48.dp)
                        .height(48.dp)
                        .alpha(NEXT_OPERATION_PREVIEW_ALPHA),
            ) {
                Text(
                    text = nextOperation,
                    fontSize = 12.sp,
                    color = Ink,
                )
            }
        }
    }
}

// maxOf, not the bare cap: a lifeCount above INITIAL_LIFE_COUNT (a future random-event grant -
// see LifeBonusTest) must still get a slot, or the extra life stays invisible.
internal fun calculateLifeSlotCount(lifeCount: Int): Int = maxOf(INITIAL_LIFE_COUNT, lifeCount)
