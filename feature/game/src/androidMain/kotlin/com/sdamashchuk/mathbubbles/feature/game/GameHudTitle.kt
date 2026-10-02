package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp

const val GAME_HUD_LEVEL_SLOT_TAG = "GameHudLevelSlot"
const val GAME_HUD_LEVEL_LABEL_TAG = "GameHudLevelLabel"
const val GAME_HUD_LEVEL_VALUE_TAG = "GameHudLevelValue"
const val GAME_HUD_SCORE_SLOT_TAG = "GameHudScoreSlot"
const val GAME_HUD_SCORE_VALUE_TAG = "GameHudScoreValue"

private val LevelLabelWidth = 72.dp
private val LevelValueWidth = 38.dp
private val ScoreValueWidth = 104.dp

@Composable
fun GameHudTitle(
    level: Int,
    score: Int,
    appliedMultiplier: Int,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.weight(1f).testTag(GAME_HUD_LEVEL_SLOT_TAG),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.width(LevelLabelWidth).testTag(GAME_HUD_LEVEL_LABEL_TAG),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                text = stringResource(id = R.string.game_session_level_label).toUpperCase(Locale.current),
                style = MaterialTheme.typography.body1,
            )
            Text(
                modifier = Modifier.width(LevelValueWidth).testTag(GAME_HUD_LEVEL_VALUE_TAG),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                text = level.toString(),
                style = MaterialTheme.typography.body1,
            )
        }
        Row(
            modifier = Modifier.weight(1f).testTag(GAME_HUD_SCORE_SLOT_TAG),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.width(ScoreValueWidth).testTag(GAME_HUD_SCORE_VALUE_TAG),
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
}
