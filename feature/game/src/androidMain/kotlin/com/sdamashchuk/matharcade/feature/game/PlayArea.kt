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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
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

private const val PLAY_AREA_HEIGHT_FRACTION = 0.75f

// Same alpha the old flat VerticalDivider used - carried over, not re-picked.
private const val LANE_GUIDE_ALPHA = 0.12f
private const val LANE_GUIDE_WIDTH_DP = 1

// MC-81: four falling-target columns plus their converging guide lines. The guide lines and the
// targets share laneOffsetX (see LaneConvergenceOffsetX.kt) so neither can drift from the other.
@Composable
fun PlayArea(
    gameState: State<GameViewModel.State>,
    onTargetClicked: (id: Int) -> Unit,
    readinessHintsEnabled: Boolean,
) {
    var gameColumnSize by remember { mutableStateOf(Size(0, 0)) }
    val localDensity = LocalDensity.current
    val laneGuideColor = MaterialTheme.colors.onSurface.copy(alpha = LANE_GUIDE_ALPHA)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(PLAY_AREA_HEIGHT_FRACTION),
    ) {
        // Replaces the old straight VerticalDivider per boundary - a converging guide, drawn with
        // the same laneOffsetX a falling target's hit box uses, so the lane lines and the targets
        // always agree on where a lane is.
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
                        )
                    }
                }
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

// boundaryIndex is 1-based (the boundary after column 0 is lane index 1, matching the fractional
// lane indices laneOffsetX already uses for target centres) - only the endpoints matter since the
// convergence is linear in fallFraction, so a straight line between them is exact, not an
// approximation.
private fun DrawScope.drawLaneGuide(
    color: Color,
    boundaryIndex: Int,
    columnWidth: Int,
) {
    val centerX = size.width / 2f
    // laneOffsetX works in dp, the same unit gameColumnSize carries; this canvas draws in px. The
    // conversion is what keeps the lines on the lanes the targets actually fall down.
    val topX = centerX + laneOffsetX(boundaryIndex.toFloat(), columnWidth, fallFraction = 0f).dp.toPx()
    val bottomX = centerX + laneOffsetX(boundaryIndex.toFloat(), columnWidth, fallFraction = 1f).dp.toPx()
    drawLine(
        color = color,
        start = Offset(topX, 0f),
        end = Offset(bottomX, size.height),
        strokeWidth = LANE_GUIDE_WIDTH_DP.dp.toPx(),
    )
}
