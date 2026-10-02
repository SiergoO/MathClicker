package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.ui.theme.Accent
import com.sdamashchuk.mathbubbles.core.ui.theme.Ink
import com.sdamashchuk.mathbubbles.core.ui.theme.InkHud

private val ROW_VERTICAL_PADDING = 16.dp

/**
 * One row of the results table - a single closed run. [isBest] marks the player's record among
 * [ResultsTable]'s window, which is not necessarily this row's position in it (see
 * GameRepository.getBestClosedField's own contract: the best is taken over all history, so it can
 * be absent from a seven-row recent window entirely).
 */
@Composable
fun ResultRow(
    field: Field,
    isBest: Boolean,
    modifier: Modifier = Modifier,
) {
    val supportColor = if (isBest) Accent else InkHud
    val scoreColor = if (isBest) Accent else Ink
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = ROW_VERTICAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cell(
            text = formatResultDate(field.finishedAt, stringResource(id = R.string.results_no_date)),
            weight = ResultsColumns.DATE_WEIGHT,
            alignment = ResultsColumns.DATE_ALIGNMENT,
            color = supportColor,
            style = MaterialTheme.typography.body1,
        )
        Cell(
            text = field.level.toString(),
            weight = ResultsColumns.LEVEL_WEIGHT,
            alignment = ResultsColumns.NUMBER_ALIGNMENT,
            color = supportColor,
            style = MaterialTheme.typography.body1,
        )
        Cell(
            text = field.score.toString(),
            weight = ResultsColumns.SCORE_WEIGHT,
            alignment = ResultsColumns.NUMBER_ALIGNMENT,
            color = scoreColor,
            style =
                MaterialTheme.typography.body1.copy(
                    fontWeight = if (isBest) FontWeight.Bold else FontWeight.Medium,
                ),
        )
    }
}

@Composable
private fun RowScope.Cell(
    text: String,
    weight: Float,
    alignment: Alignment,
    color: Color,
    style: TextStyle,
) {
    Box(modifier = Modifier.weight(weight), contentAlignment = alignment) {
        Text(text = text, color = color, style = style)
    }
}
