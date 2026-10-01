package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.ui.component.GlassButton
import com.sdamashchuk.mathbubbles.core.ui.component.ScreenWrapper

@Composable
fun GamePausedDialog(
    onResumeClicked: () -> Unit,
    onRestartClicked: () -> Unit,
    onBackToMainMenuClicked: () -> Unit,
) {
    ScreenWrapper(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier =
                    Modifier
                        .weight(1f)
                        .wrapContentSize(),
                text = stringResource(id = R.string.pause),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h1,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                GlassButton(
                    modifier = Modifier.padding(bottom = 20.dp),
                    text = stringResource(id = R.string.resume),
                    onClick = onResumeClicked,
                )
                GlassButton(
                    modifier = Modifier.padding(bottom = 20.dp),
                    text = stringResource(id = R.string.restart),
                    onClick = onRestartClicked,
                )
                GlassButton(
                    text = stringResource(id = R.string.main_menu),
                    onClick = onBackToMainMenuClicked,
                )
            }
        }
    }
}
