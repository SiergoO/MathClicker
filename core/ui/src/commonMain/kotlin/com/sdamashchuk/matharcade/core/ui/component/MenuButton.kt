package com.sdamashchuk.matharcade.core.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sdamashchuk.matharcade.core.ui.theme.Accent
import com.sdamashchuk.matharcade.core.ui.theme.Shapes

// MC-91: one shape for every menu-style button - full width inside a 24dp gutter, rather than a
// fixed 200dp that left the two buttons floating in the middle of a phone screen.
private const val GUTTER_DP = 24
private const val HEIGHT_DP = 60

@Composable
fun MenuButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = Accent),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = GUTTER_DP.dp)
                .height(HEIGHT_DP.dp)
                .clip(Shapes.large),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.h2,
        )
    }
}
