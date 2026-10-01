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
import com.sdamashchuk.mathbubbles.core.ui.component.GlassButton
import com.sdamashchuk.mathbubbles.core.ui.component.NavBar
import com.sdamashchuk.mathbubbles.core.ui.component.ScreenWrapper
import com.sdamashchuk.mathbubbles.core.ui.theme.Success
import com.sdamashchuk.mathbubbles.core.ui.theme.Warning
import com.sdamashchuk.mathbubbles.feature.game.model.ResultsSummary
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
            // Table and buttons are one group centred in the space left below the header, so a
            // two-row history (new install) and a ten-row one both read as deliberate instead of the
            // table stranding the buttons above an empty half-screen. verticalScroll is the
            // fallback if a large font scale ever makes the group taller than that space.
            Column(
                modifier =
                    Modifier
                        .weight(1f)
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
                GlassButton(
                    modifier = Modifier.padding(top = 20.dp, bottom = 12.dp),
                    text = stringResource(id = R.string.restart),
                    onClick = onRestartClicked,
                )
                GlassButton(
                    modifier = Modifier.padding(bottom = 12.dp),
                    text = stringResource(id = R.string.main_menu),
                    onClick = onBackToMainMenuClicked,
                )
            }
        }
    }
}

// The near-miss line itself: a new record reads as a win (Success), falling short reads as the
// gap left to close (Warning) - the same two colours FeedbackBanner already uses for the same two
// meanings elsewhere on this screen's own flow.
@Composable
private fun SummaryText(summary: ResultsSummary) {
    val (text, color) =
        when (summary) {
            is ResultsSummary.NewRecord -> {
                stringResource(id = R.string.results_new_record) to Success
            }

            is ResultsSummary.ShortOfBest -> {
                stringResource(id = R.string.results_short_of_best, summary.deltaToBest) to Warning
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
