package com.sdomashchuk.mathclicker.core.ui.component

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sdomashchuk.mathclicker.core.ui.theme.Red500
import com.sdomashchuk.mathclicker.core.ui.theme.Shapes

@Composable
fun MenuButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = Red500),
        modifier =
            modifier
                .width(200.dp)
                .height(60.dp)
                .clip(Shapes.large),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.h2,
        )
    }
}
