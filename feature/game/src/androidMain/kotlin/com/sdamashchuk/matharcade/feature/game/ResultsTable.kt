package com.sdamashchuk.matharcade.feature.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.ui.theme.AccentDeep
import com.sdamashchuk.matharcade.core.ui.theme.InkDim
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderCell(
                stringResource(id = R.string.results_header_date),
                weight = ResultsColumns.DATE_WEIGHT,
                alignment = ResultsColumns.DATE_ALIGNMENT,
            )
            HeaderCell(
                stringResource(id = R.string.results_header_level),
                weight = ResultsColumns.LEVEL_WEIGHT,
                alignment = ResultsColumns.NUMBER_ALIGNMENT,
            )
            HeaderCell(
                stringResource(id = R.string.results_header_score),
                weight = ResultsColumns.SCORE_WEIGHT,
                alignment = ResultsColumns.NUMBER_ALIGNMENT,
            )
        }
        Divider(color = AccentDeep)
        recentResults.forEach { field ->
            ResultRow(field = field, isBest = field.id == bestResultId)
        }
    }
}

@Composable
private fun RowScope.HeaderCell(
    text: String,
    weight: Float,
    alignment: Alignment,
) {
    Box(modifier = Modifier.weight(weight), contentAlignment = alignment) {
        Text(
            text = text.toUpperCase(Locale.current),
            color = InkDim,
            style = MaterialTheme.typography.caption,
        )
    }
}
