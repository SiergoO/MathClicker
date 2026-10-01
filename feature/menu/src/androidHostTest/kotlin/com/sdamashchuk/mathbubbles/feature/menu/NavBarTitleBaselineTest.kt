package com.sdamashchuk.mathbubbles.feature.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.sdamashchuk.mathbubbles.core.ui.component.NavBar
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

private val SETTINGS_SCREEN_SLICE_HEIGHT = 200.dp
private val NAV_BAR_FIXTURE_HEIGHT = 56.dp

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class NavBarTitleBaselineTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `settings title sits on the same baseline as a bare NavBar fixture`() {
        val component =
            SettingsComponent(
                componentContext = DefaultComponentContext(lifecycle = LifecycleRegistry()),
                soundSettings = FakeSoundSettings(soundEnabled = true),
                onBackClicked = {},
            )

        composeTestRule.setContent {
            Column {
                Box(modifier = Modifier.height(SETTINGS_SCREEN_SLICE_HEIGHT)) {
                    SettingsScreen(component = component)
                }
                Box(modifier = Modifier.height(NAV_BAR_FIXTURE_HEIGHT)) {
                    MathBubblesTheme {
                        NavBar(title = "Settings")
                    }
                }
            }
        }

        val density = composeTestRule.density
        val titleBaselines =
            composeTestRule.onAllNodesWithText("Settings").fetchSemanticsNodes().map { node ->
                firstBaselineInRoot(node, density)
            }

        assertEquals(
            SETTINGS_SCREEN_SLICE_HEIGHT.value,
            titleBaselines[1] - titleBaselines[0],
            0.5f,
            "Settings screen title baseline vs the bare NavBar fixture's title baseline: $titleBaselines",
        )
    }
}

private fun firstBaselineInRoot(
    node: SemanticsNode,
    density: Density,
): Float {
    val textLayoutResults = mutableListOf<TextLayoutResult>()
    node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(textLayoutResults)
    val firstBaseline = textLayoutResults.first().firstBaseline
    return with(density) { (node.boundsInRoot.top + firstBaseline).toDp().value }
}
