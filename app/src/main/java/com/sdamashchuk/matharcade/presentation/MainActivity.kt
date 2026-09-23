package com.sdamashchuk.matharcade.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.retainedComponent
import com.sdamashchuk.matharcade.core.ui.theme.MathArcadeTheme
import com.sdamashchuk.matharcade.presentation.navigation.RootComponent
import com.sdamashchuk.matharcade.presentation.navigation.RootContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Survives configuration changes, so a game in progress is not restarted by one. Not
        // rotation — the manifest pins portrait, so that never recreates this Activity; the
        // reachable ones are font scale, dark mode and locale.
        val root = retainedComponent { RootComponent(it) }
        setContent {
            MathArcadeTheme {
                RootContent(root)
            }
        }
    }
}
