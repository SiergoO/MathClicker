package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.ui.component.BottomActions
import com.sdamashchuk.mathbubbles.core.ui.component.GlassButton
import com.sdamashchuk.mathbubbles.core.ui.component.NavBar
import com.sdamashchuk.mathbubbles.core.ui.component.ScreenWrapper
import com.sdamashchuk.mathbubbles.core.ui.theme.InkHud
import com.sdamashchuk.mathbubbles.core.ui.theme.Success
import com.sdamashchuk.mathbubbles.feature.game.model.ResultsSummary
import kotlinx.collections.immutable.ImmutableList

private const val TABLE_AREA_WEIGHT = 2f
private const val BUTTON_AREA_WEIGHT = 1f

/**
 * What GameOver shows instead of the old bare Game Over/Restart/Main Menu dialog (MC-53): this
 * run's score, its near-miss delta to the player's best, and the last ten finished runs.
 */
@Composable
fun ResultsScreen(
    field: Field,
    recentResults: ImmutableList<Field>,
    bestResult: Field?,
    onRestartClicked: () -> Unit,
    onBackToMainMenuClicked: () -> Unit,
) {
    val resultsContentDescription = stringResource(id = R.string.results_content_description)
    ScreenWrapper(
        topBar = { NavBar(title = stringResource(id = R.string.game_over)) },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .semantics { contentDescription = resultsContentDescription },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = field.score.toString(),
                style = MaterialTheme.typography.h1,
            )
            SummaryText(resultsSummaryOf(field, bestResult))
            // The table sits in the space left below the header; verticalScroll is the fallback if
            // a large font scale or a ten-row history ever makes it taller than that space.
            Column(
                modifier =
                    Modifier
                        .weight(TABLE_AREA_WEIGHT)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ResultsTable(
                    recentResults = recentResults,
                    bestResultId = bestResult?.id,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                )
            }
            BottomActions(modifier = Modifier.fillMaxWidth().weight(BUTTON_AREA_WEIGHT)) {
                GlassButton(
                    text = stringResource(id = R.string.restart),
                    onClick = onRestartClicked,
                )
                GlassButton(
                    text = stringResource(id = R.string.main_menu),
                    onClick = onBackToMainMenuClicked,
                )
            }
        }
    }
}

@Composable
private fun SummaryText(summary: ResultsSummary) {
    val (text, color) =
        when (summary) {
            is ResultsSummary.NewRecord -> {
                stringResource(id = R.string.results_new_record) to Success
            }

            is ResultsSummary.ShortOfBest -> {
                stringResource(id = R.string.results_short_of_best, summary.deltaToBest) to InkHud
            }

            is ResultsSummary.NoHistory -> {
                "" to Color.Transparent
            }
        }
    Text(
        text = text,
        color = color,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.body1,
    )
}
