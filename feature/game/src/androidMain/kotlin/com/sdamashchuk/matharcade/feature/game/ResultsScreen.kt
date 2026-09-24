package com.sdamashchuk.matharcade.feature.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.ui.component.MenuButton
import com.sdamashchuk.matharcade.core.ui.theme.Green200
import com.sdamashchuk.matharcade.core.ui.theme.Red500
import com.sdamashchuk.matharcade.feature.game.model.ResultsSummary
import kotlinx.collections.immutable.ImmutableList

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
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(id = R.string.game_over),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h2,
        )
        Text(
            text = field.score.toString(),
            style = MaterialTheme.typography.h1,
        )
        SummaryText(resultsSummaryOf(field, bestResult))
        ResultsTable(
            recentResults = recentResults,
            bestResultId = bestResult?.id,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
        )
        MenuButton(
            modifier = Modifier.padding(bottom = 12.dp),
            text = stringResource(id = R.string.restart),
            onClick = onRestartClicked,
        )
        MenuButton(
            modifier = Modifier.padding(bottom = 12.dp),
            text = stringResource(id = R.string.main_menu),
            onClick = onBackToMainMenuClicked,
        )
    }
}

// The near-miss line itself: a new record reads as a win (Green200), falling short reads as the
// gap left to close (Red500) - the same two colours FeedbackBanner already uses for the same two
// meanings elsewhere on this screen's own flow.
@Composable
private fun SummaryText(summary: ResultsSummary) {
    val (text, color) =
        when (summary) {
            is ResultsSummary.NewRecord -> {
                stringResource(id = R.string.results_new_record) to Green200
            }

            is ResultsSummary.ShortOfBest -> {
                stringResource(id = R.string.results_short_of_best, summary.deltaToBest) to Red500
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
