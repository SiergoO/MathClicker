package com.sdamashchuk.matharcade.feature.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sdamashchuk.matharcade.core.model.Field
import kotlinx.collections.immutable.ImmutableList

/**
 * The last ten finished runs, most recent first (see Field.sq's getRecentClosedFields). No scroll:
 * ten rows plus a header is the whole table, by design (MC-53 spec).
 */
@Composable
fun ResultsTable(
    recentResults: ImmutableList<Field>,
    bestResultId: Int?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text(modifier = Modifier.width(ResultsColumns.STAR_WIDTH), text = "")
            HeaderCell(stringResource(id = R.string.results_header_date), weight = ResultsColumns.DATE_WEIGHT)
            HeaderCell(stringResource(id = R.string.results_header_level), weight = ResultsColumns.LEVEL_WEIGHT)
            HeaderCell(stringResource(id = R.string.results_header_score), weight = ResultsColumns.SCORE_WEIGHT)
        }
        Divider()
        recentResults.forEach { field ->
            ResultRow(field = field, isBest = field.id == bestResultId)
        }
    }
}

@Composable
private fun RowScope.HeaderCell(
    text: String,
    weight: Float,
) {
    Text(
        modifier = Modifier.weight(weight),
        text = text,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.caption,
    )
}
