package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.sdamashchuk.mathbubbles.core.ui.component.NavBar
import com.sdamashchuk.mathbubbles.core.ui.component.ScreenWrapper

@Composable
fun GameChrome(
    level: Int,
    score: Int,
    appliedMultiplier: Int,
    onPauseClicked: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ScreenWrapper(
        edgeToEdgeContent = true,
        topBar = {
            NavBar(
                titleContent = {
                    GameHudTitle(
                        level = level,
                        score = score,
                        appliedMultiplier = appliedMultiplier,
                    )
                },
                actions = {
                    PauseButton(onClick = onPauseClicked)
                },
            )
        },
        content = content,
    )
}
