package com.sdamashchuk.matharcade.feature.game

import android.util.Size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sdamashchuk.matharcade.core.model.GAME_COLUMN_COUNT
import com.sdamashchuk.matharcade.core.ui.theme.WaterDeep
import com.sdamashchuk.matharcade.core.ui.theme.WaterShaft
import com.sdamashchuk.matharcade.core.ui.theme.WaterSurface
import com.sdamashchuk.matharcade.feature.game.model.TargetScreenPosition
import com.sdamashchuk.matharcade.feature.game.model.TargetZeroedBurst
import com.sdamashchuk.matharcade.feature.game.model.TargetZeroedSignal

private const val PLAY_AREA_HEIGHT_FRACTION = 0.75f

// MC-84: the brightest band of the water sits here, not at the top - it reads as a shaft of light
// from above and is what stops the column from looking like a flat wash.
private const val WATER_SHAFT_STOP = 0.62f

// MC-84: four falling-target columns over the water. The reference art has no lane separators at
// all: the bubbles carry the columns on their own, and a hairline grid over water reads as a
// spreadsheet.
@Composable
fun PlayArea(
    gameState: State<GameViewModel.State>,
    onTargetClicked: (id: Int) -> Unit,
    targetZeroedSignal: TargetZeroedSignal?,
    readinessHintsEnabled: Boolean,
) {
    var gameColumnSize by remember { mutableStateOf(Size(0, 0)) }
    val localDensity = LocalDensity.current
    val depthFactor = waterDepthFactor(gameState.value.field.level)

    // Where a target last was, kept around after it leaves the composition: TargetButton reports
    // its own position on every active frame (see SideEffect there), and a zeroed target's last
    // report is exactly where its burst below needs to appear.
    val targetPositions = remember { mutableStateMapOf<Int, TargetScreenPosition>() }
    val bursts = remember { mutableStateListOf<TargetZeroedBurst>() }
    LaunchedEffect(targetZeroedSignal) {
        val signal = targetZeroedSignal ?: return@LaunchedEffect
        val position = targetPositions.remove(signal.targetId) ?: return@LaunchedEffect
        bursts.add(TargetZeroedBurst(signal.sequence, position))
    }
    // Bounds targetPositions to the current wave: a target that only ever breaks out (never
    // zeroes) leaves its entry unconsumed above, so this is what keeps that map from growing for
    // the rest of the session.
    LaunchedEffect(gameState.value.targetList) {
        targetPositions.keys.retainAll(gameState.value.targetList.mapTo(mutableSetOf()) { it.id })
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(PLAY_AREA_HEIGHT_FRACTION)
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            0f to sink(WaterSurface, depthFactor),
                            WATER_SHAFT_STOP to sink(WaterShaft, depthFactor),
                            1f to sink(WaterDeep, depthFactor),
                        ),
                    )
                },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        val measuredWidthDp =
                            with(localDensity) {
                                coordinates.size.width
                                    .toDp()
                                    .value
                                    .toInt()
                            }
                        val gameColumnWidth = calculateGameColumnWidth(measuredWidthDp)
                        val gameColumnHeight =
                            with(localDensity) {
                                coordinates.size.height
                                    .toDp()
                                    .value
                                    .toInt() - (gameColumnWidth * 0.8).toInt()
                            }
                        gameColumnSize = Size(gameColumnWidth, gameColumnHeight)
                    },
        ) {
            repeat(GAME_COLUMN_COUNT) { columnId ->
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(gameColumnSize.width.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    gameState.value.targetList.filter { target -> target.columnId == columnId }.forEach {
                        TargetButton(
                            it,
                            gameColumnSize,
                            isReady =
                                shouldShowReadinessHint(
                                    it,
                                    gameState.value.field.currentOperationSign,
                                    gameState.value.field.currentOperationDigit,
                                    hintsEnabled = readinessHintsEnabled,
                                ),
                            gameTimeMs = gameState.value.field.gameTimeMs,
                            onTargetClicked = onTargetClicked,
                            onTargetPositioned = { id, position -> targetPositions[id] = position },
                        )
                    }
                }
            }
        }
        // Drawn last, on top of the columns: a burst marks where a target was, not another lane
        // occupant.
        bursts.forEach { burst ->
            key(burst.id) {
                BurstRing(position = burst.position, onFinished = { bursts.remove(burst) })
            }
        }
    }
}

// An even split. The subtraction this used to carry reserved width for 1dp lane separators; MC-81
// moved those into an overlay and MC-84 removed them, so reserving for them left the row 3dp short
// and every column fractionally left of where it belongs.
internal fun calculateGameColumnWidth(measuredWidthDp: Int): Int = measuredWidthDp / GAME_COLUMN_COUNT

// MC-84: darkens one water stop toward the abyss. Multiplying every channel by the same factor
// keeps the hue and only removes light, which is what water actually does with depth.
private fun sink(
    color: Color,
    factor: Float,
): Color = Color(color.red * factor, color.green * factor, color.blue * factor, color.alpha)
