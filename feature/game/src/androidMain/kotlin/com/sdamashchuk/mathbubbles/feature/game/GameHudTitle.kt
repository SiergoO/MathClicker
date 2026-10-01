package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase

@Composable
fun GameHudTitle(
    level: Int,
    score: Int,
    appliedMultiplier: Int,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            text = stringResource(id = R.string.game_session_level, level).toUpperCase(Locale.current),
            style = MaterialTheme.typography.body1,
        )
        Text(
            modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            text =
                scoreLabel(
                    appliedMultiplier = appliedMultiplier,
                    plainScore = score.toString(),
                    comboScore = stringResource(id = R.string.game_session_score_combo, appliedMultiplier, score),
                ).toUpperCase(Locale.current),
            style = MaterialTheme.typography.body1,
        )
    }
}
