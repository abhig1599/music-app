package com.abhi.musicapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.ui.FitDesignWidth
import com.abhi.musicapp.ui.screens.HomeScreen
import com.abhi.musicapp.ui.screens.PlayerScreen
import com.abhi.musicapp.ui.theme.MusicAppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders at the width of his phone (360dp, 2.0 density) to check nothing is clipped. Fixture data, not phone screenshots. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], instrumentedPackages = ["androidx.loader.content"], qualifiers = "w360dp-h800dp-xhdpi")
class NarrowPhoneShotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val track = com.abhi.musicapp.playback.PlayerUiState(
        title = "Tera Naam Japdi Phiran - Full Song", isPlaying = true, positionMs = 9_000L, durationMs = 190_000L, hasTrack = true)

    @Test fun playerNarrow() {
        composeRule.setContent { FitDesignWidth { MusicAppTheme { androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) { PlayerScreen(onBack = {}, fixture = track) } } } }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/narrow360_player.png")
    }

    @Test fun homeNarrow() {
        composeRule.setContent { FitDesignWidth { MusicAppTheme { androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) { HomeScreen({}, {}, {}, {}, {}, {}, fixture = track) } } } }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/narrow360_home.png")
    }

    private fun bg(c: @androidx.compose.runtime.Composable () -> Unit): @androidx.compose.runtime.Composable () -> Unit = {
        androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) { c() }
    }

    @Test fun settingsNarrow() {
        org.robolectric.RuntimeEnvironment.setQualifiers("w360dp-h1700dp-xhdpi")
        composeRule.setContent { FitDesignWidth { MusicAppTheme { bg { com.abhi.musicapp.ui.screens.SettingsScreen(onBack = {}) }() } } }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/narrow360_settings.png")
    }

    @Test fun playerFontScale130() {
        org.robolectric.RuntimeEnvironment.setFontScale(1.3f)
        composeRule.setContent { FitDesignWidth { MusicAppTheme { bg { PlayerScreen(onBack = {}, fixture = track) }() } } }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/narrow360_player_font130.png")
    }

    /** Shipped wording: source label as the real app shows it. */
    @Test fun playerShippedWording() {
        composeRule.setContent { FitDesignWidth { MusicAppTheme { bg { PlayerScreen(onBack = {}, fixture = track, sourceLabel = "Local music") }() } } }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/narrow360_player_shipped_label.png")
    }

    /** Shipped Home (no fixture): real "Not checked" pill, empty library. */
    @Test fun homeShipped() {
        composeRule.setContent { FitDesignWidth { MusicAppTheme { bg { HomeScreen({}, {}, {}, {}, {}, {}) }() } } }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/narrow360_home_shipped.png")
    }

    /** Reachability at 360x800: scroll the Settings list to the About rows, check displayed, tap one and check the VIEW intent. */
    @Test fun aboutReachableAndOpensLink() {
        org.robolectric.RuntimeEnvironment.setQualifiers("w360dp-h800dp-xhdpi")
        composeRule.setContent { FitDesignWidth { MusicAppTheme { bg { com.abhi.musicapp.ui.screens.SettingsScreen(onBack = {}) }() } } }
        val node = composeRule.onNodeWithText("Privacy policy (portfolio page for now)")
        node.performScrollTo()
        node.assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/narrow360_settings_scrolled_to_about.png")
        node.performClick()
        val intent = org.robolectric.Shadows.shadowOf(androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()).nextStartedActivity
        org.junit.Assert.assertEquals("https://abhicreative.world", intent.data.toString())
    }
}
