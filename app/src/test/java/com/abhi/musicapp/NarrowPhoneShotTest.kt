package com.abhi.musicapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
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
}
