package com.sdamashchuk.mathbubbles.core.ui.component

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.ui.theme.Accent
import com.sdamashchuk.mathbubbles.core.ui.theme.Shapes

private val DefaultGutter = 24.dp
private const val HEIGHT_DP = 60

@Composable
fun MenuButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    horizontalPadding: Dp = DefaultGutter,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = Accent),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .height(HEIGHT_DP.dp)
                .clip(Shapes.large),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.h2,
        )
    }
}
