package com.sdomashchuk.mathclicker.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val DarkColorPalette =
    darkColors(
        primary = Red200,
        primaryVariant = Red700,
        secondary = Green200,
        background = DarkGray,
        surface = DarkGray,
    )

private val LightColorPalette =
    lightColors(
        primary = Red500,
        primaryVariant = Red700,
        secondary = Green200,
        background = White,
        surface = White,
    )

@Composable
fun MathClickerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors =
        if (darkTheme) {
            DarkColorPalette
        } else {
            LightColorPalette
        }

    MaterialTheme(
        colors = colors,
        // Typography() rather than a val: loading the QuickSand family via CMP resources needs a
        // composable context, so it can no longer be a top-level constant (see Typography.kt).
        typography = Typography(),
        shapes = Shapes,
        content = content,
    )
}
