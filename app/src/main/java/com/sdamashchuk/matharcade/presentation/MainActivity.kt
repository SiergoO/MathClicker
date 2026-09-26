package com.sdamashchuk.matharcade.presentation

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.arkivanov.decompose.retainedComponent
import com.sdamashchuk.matharcade.core.ui.theme.MathArcadeTheme
import com.sdamashchuk.matharcade.presentation.navigation.RootComponent
import com.sdamashchuk.matharcade.presentation.navigation.RootContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // MC-86: draw under the system bars rather than beside them. windowTranslucentStatus used
        // to do half of this, but it also asks the platform for its own grey scrim, which is what
        // put a band of not-quite-water at each end of the screen. The layout already reserves the
        // insets with statusBarsPadding/navigationBarsPadding.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
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
