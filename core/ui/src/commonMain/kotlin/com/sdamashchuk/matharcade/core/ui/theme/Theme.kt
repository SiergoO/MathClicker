package com.sdamashchuk.matharcade.core.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// MC-84: one palette, not a light/dark pair. The game is set underwater, and a light variant of
// that is not a theme - it is a different world. Following the system setting here would have the
// board flip to white paper on half the devices.
private val WaterPalette =
    darkColors(
        primary = Accent,
        primaryVariant = AccentDeep,
        secondary = Success,
        background = WaterDeep,
        surface = WaterDeep,
        onPrimary = WaterSurface,
        onSecondary = WaterSurface,
        onBackground = Ink,
        onSurface = Ink,
    )

@Composable
fun MathArcadeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = WaterPalette,
        // Typography() rather than a val: loading the QuickSand family via CMP resources needs a
        // composable context, so it can no longer be a top-level constant (see Typography.kt).
        typography = Typography(),
        shapes = Shapes,
    ) {
        // The background has to be painted by someone. It used to come from the Activity window
        // being white; now that the window is water, every screen still needs a surface under it
        // or a translucent overlay composites against nothing.
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
            content()
        }
    }
}
