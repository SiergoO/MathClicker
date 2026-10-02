package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sdamashchuk.mathbubbles.core.ui.component.NavBarAction
import com.sdamashchuk.mathbubbles.core.ui.theme.Ink

@Composable
fun PauseButton(onClick: () -> Unit) {
    NavBarAction(
        painter = painterResource(id = R.drawable.ic_pause),
        contentDescription = stringResource(id = R.string.pause_button),
        onClick = onClick,
        tint = Ink,
    )
}
