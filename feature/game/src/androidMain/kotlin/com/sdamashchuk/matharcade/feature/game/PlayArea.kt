package com.sdamashchuk.matharcade.feature.game

import android.util.Size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sdamashchuk.matharcade.core.model.GAME_COLUMN_COUNT
import com.sdamashchuk.matharcade.feature.game.model.TargetScreenPosition
import com.sdamashchuk.matharcade.feature.game.model.TargetZeroedBurst
import com.sdamashchuk.matharcade.feature.game.model.TargetZeroedSignal

private const val PLAY_AREA_HEIGHT_FRACTION = 0.75f

private const val LANE_GUIDE_ALPHA = 0.12f
private const val LANE_GUIDE_WIDTH_DP = 1

// Four falling-target columns and the hairlines that separate them.
@Composable
fun PlayArea(
    gameState: State<GameViewModel.State>,
    onTargetClicked: (id: Int) -> Unit,
    targetZeroedSignal: TargetZeroedSignal?,
    readinessHintsEnabled: Boolean,
) {
    var gameColumnSize by remember { mutableStateOf(Size(0, 0)) }
    val localDensity = LocalDensity.current
    val laneGuideColor = MaterialTheme.colors.onSurface.copy(alpha = LANE_GUIDE_ALPHA)

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
                .fillMaxHeight(PLAY_AREA_HEIGHT_FRACTION),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            repeat(GAME_COLUMN_COUNT) { columnId ->
                if (shouldDrawDividerAfterColumn(columnId)) {
                    drawLaneGuide(laneGuideColor, boundaryIndex = columnId + 1, columnWidth = gameColumnSize.width)
                }
            }
        }
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

// The dividers between columns cost one gap fewer than there are columns.
internal fun calculateGameColumnWidth(measuredWidthDp: Int): Int =
    (measuredWidthDp - (GAME_COLUMN_COUNT - 1)) / GAME_COLUMN_COUNT

// A divider sits between columns, not after the last one: GAME_COLUMN_COUNT columns need
// GAME_COLUMN_COUNT - 1 of them.
internal fun shouldDrawDividerAfterColumn(columnId: Int): Boolean = columnId < GAME_COLUMN_COUNT - 1

// boundaryIndex is 1-based: the boundary after column 0 is boundary 1. The Row packs its columns
// from the left edge, so a boundary sits at exactly that many column widths in.
private fun DrawScope.drawLaneGuide(
    color: Color,
    boundaryIndex: Int,
    columnWidth: Int,
) {
    val x = (boundaryIndex * columnWidth).dp.toPx()
    drawLine(
        color = color,
        start = Offset(x, 0f),
        end = Offset(x, size.height),
        strokeWidth = LANE_GUIDE_WIDTH_DP.dp.toPx(),
    )
}
