package com.sdomashchuk.mathclicker.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.retainedComponent
import com.sdomashchuk.mathclicker.presentation.navigation.RootComponent
import com.sdomashchuk.mathclicker.presentation.navigation.RootContent
import com.sdomashchuk.mathclicker.presentation.ui.theme.MathClickerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Survives configuration changes, so a game in progress is not restarted by one. Not
        // rotation — the manifest pins portrait, so that never recreates this Activity; the
        // reachable ones are font scale, dark mode and locale.
        val root = retainedComponent { RootComponent(it) }
        setContent {
            MathClickerTheme {
                RootContent(root)
            }
        }
    }
}
