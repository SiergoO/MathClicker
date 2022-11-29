package com.sdomashchuk.mathclicker.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sdomashchuk.mathclicker.R
import com.sdomashchuk.mathclicker.menu.MenuButton
import com.sdomashchuk.mathclicker.ui.theme.Typography

@Composable
fun GameMenuDialog(
    headerText: String,
    onRestartClick: () -> Unit,
    onBackToMainMenuClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .wrapContentSize(),
            text = headerText,
            textAlign = TextAlign.Center,
            style = Typography.h1
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            MenuButton(
                modifier = Modifier.padding(bottom = 20.dp),
                text = stringResource(id = R.string.restart),
                onClick = onRestartClick
            )
            MenuButton(
                text = stringResource(id = R.string.main_menu),
                onClick = onBackToMainMenuClick
            )
        }
    }
}