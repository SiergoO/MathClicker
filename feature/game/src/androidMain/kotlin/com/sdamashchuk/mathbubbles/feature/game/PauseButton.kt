package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.ui.theme.Accent

// Two bars rather than the word: "Pause" needs more width than the HUD has beside Level and Score,
// and it clipped against the screen edge at 56dp. A glyph also survives translation, which the word
// would not. The label stays as the content description so the control is still findable by name.
@Composable
fun PauseButton(onClick: () -> Unit) {
    val label = stringResource(id = R.string.pause_button)
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clickable(onClick = onClick)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(2) {
                Box(
                    modifier =
                        Modifier
                            .width(5.dp)
                            .height(18.dp)
                            .background(Accent, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}
