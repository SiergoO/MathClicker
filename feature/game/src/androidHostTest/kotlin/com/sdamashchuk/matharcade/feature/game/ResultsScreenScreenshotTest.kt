package com.sdamashchuk.matharcade.feature.game

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.ui.theme.MathArcadeTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale
import java.util.TimeZone

// Result timestamps render in the default zone and locale; unpinned, the golden differs per host.
private val FIXED_TIME_ZONE: TimeZone = TimeZone.getTimeZone("UTC")
private val FIXED_LOCALE: Locale = Locale.US
private const val NOW_EPOCH_MILLIS = 1_790_208_000_000L
private const val HOUR_MILLIS = 60 * 60 * 1000L

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class ResultsScreenScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var defaultTimeZone: TimeZone
    private lateinit var defaultLocale: Locale

    @Before
    fun pinTimeZoneAndLocale() {
        defaultTimeZone = TimeZone.getDefault()
        defaultLocale = Locale.getDefault()
        TimeZone.setDefault(FIXED_TIME_ZONE)
        Locale.setDefault(FIXED_LOCALE)
    }

    @After
    fun restoreTimeZoneAndLocale() {
        TimeZone.setDefault(defaultTimeZone)
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `renders recent runs and a beaten best`() {
        val bestResult =
            Field(
                id = 10,
                level = 4,
                score = 420,
                isClosed = true,
                finishedAt = NOW_EPOCH_MILLIS - HOUR_MILLIS,
            )
        val field = Field(id = 11, level = 5, score = 480, isClosed = true, finishedAt = NOW_EPOCH_MILLIS)
        val recentResults =
            persistentListOf(
                field,
                bestResult,
                Field(id = 9, level = 3, score = 300, isClosed = true, finishedAt = NOW_EPOCH_MILLIS - 2 * HOUR_MILLIS),
            )

        composeTestRule.setContent {
            MathArcadeTheme {
                ResultsScreen(
                    field = field,
                    recentResults = recentResults,
                    bestResult = bestResult,
                    onRestartClicked = {},
                    onBackToMainMenuClicked = {},
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }
}
