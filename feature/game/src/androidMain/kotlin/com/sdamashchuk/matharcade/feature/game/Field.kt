package com.sdamashchuk.matharcade.feature.game

import android.util.Size
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdamashchuk.matharcade.core.model.GAME_COLUMN_COUNT
import com.sdamashchuk.matharcade.core.ui.theme.Red200
import com.sdamashchuk.matharcade.core.ui.theme.Red500
import com.sdamashchuk.matharcade.core.ui.theme.White

@Composable
fun Field(
    gameState: State<GameViewModel.State>,
    onGameColumnSizeMeasured: (gameColumnSize: Size) -> Unit,
    onTargetRevealed: (id: Int) -> Unit,
    onTargetClicked: (id: Int) -> Unit,
    onTargetDidBreakout: (id: Int) -> Unit,
    onTargetPositionSave: (id: Int, position: Int, gameColumnHeightPx: Int) -> Unit,
    onFireClicked: () -> Unit,
) {
    var gameColumnSize by remember { mutableStateOf(Size(0, 0)) }
    val localDensity = LocalDensity.current

    Row(
        modifier =
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .fillMaxHeight(0.05f),
    ) {
        Text(
            modifier =
                Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
            textAlign = TextAlign.Center,
            text =
                stringResource(
                    id = R.string.game_session_level,
                    gameState.value.field.level,
                ).toUpperCase(Locale.current),
            style = MaterialTheme.typography.body1,
        )
        Text(
            modifier =
                Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
            textAlign = TextAlign.Center,
            text =
                stringResource(
                    id = R.string.game_session_score,
                    gameState.value.field.score,
                ).toUpperCase(Locale.current),
            style = MaterialTheme.typography.body1,
        )
    }
    Divider()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
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
                    onGameColumnSizeMeasured.invoke(gameColumnSize)
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
                        onTargetRevealed = onTargetRevealed,
                        onTargetClicked = onTargetClicked,
                        onTargetDidBreakout = onTargetDidBreakout,
                        onTargetPositionSave = onTargetPositionSave,
                    )
                }
            }
            if (shouldDrawDividerAfterColumn(columnId)) {
                VerticalDivider()
            }
        }
    }

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
            repeat(3) {
                Divider(color = if (it < gameState.value.field.lifeCount) Red500 else Color.LightGray, thickness = 3.dp)
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
                    stringResource(
                        id = R.string.game_session_combo,
                        gameState.value.field.bonusMultiplier,
                    ).toUpperCase(Locale.current),
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
                    text = gameState.value.field.let { "${it.currentOperationSign.sign}${it.currentOperationDigit}" },
                    fontSize = 36.sp,
                    color = White,
                )
            }
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(backgroundColor = Red200),
                modifier =
                    Modifier
                        .weight(1f)
                        .wrapContentSize()
                        .clip(CircleShape)
                        .width(48.dp)
                        .height(48.dp)
                        .alpha(0.8f),
            ) {
                Text(
                    text = gameState.value.field.let { "${it.nextOperationSign.sign}${it.nextOperationDigit}" },
                    fontSize = 12.sp,
                    color = White,
                )
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
