package com.sdamashchuk.matharcade.feature.game

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.ui.theme.Green200

/**
 * One row of the results table - a single closed run. [isBest] marks the player's record among
 * [ResultsTable]'s window, which is not necessarily this row's position in it (see
 * GameRepository.getBestClosedField's own contract: the best is taken over all history, so it can
 * be absent from a ten-row recent window entirely).
 */
@Composable
fun ResultRow(
    field: Field,
    isBest: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.width(ResultsColumns.STAR_WIDTH),
            text = if (isBest) "★" else "",
            color = Green200,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body2,
        )
        Text(
            modifier = Modifier.weight(ResultsColumns.DATE_WEIGHT),
            text = formatResultDate(field.finishedAt, stringResource(id = R.string.results_no_date)),
            textAlign = TextAlign.Center,
            style = rowStyle(isBest),
        )
        Text(
            modifier = Modifier.weight(ResultsColumns.LEVEL_WEIGHT),
            text = field.level.toString(),
            textAlign = TextAlign.Center,
            style = rowStyle(isBest),
        )
        Text(
            modifier = Modifier.weight(ResultsColumns.SCORE_WEIGHT),
            text = field.score.toString(),
            textAlign = TextAlign.Center,
            style = rowStyle(isBest),
        )
    }
}

@Composable
private fun rowStyle(isBest: Boolean) =
    if (isBest) {
        MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.body2
    }
