package com.abhi.musicapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.up
import androidx.compose.ui.test.click
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.playback.PlayerUiState
import com.abhi.musicapp.ui.screens.PlayerScreen
import com.abhi.musicapp.ui.tapOrHold
import com.abhi.musicapp.ui.theme.MusicAppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
class PlayerProgressAndHoldTest {
    @get:Rule val rule = createComposeRule()

    /** Full Player screen at 25% (47.5 s of 190 s) with progress integration on (fixture otherwise draws the static Figma cluster). */
    @Test fun playerAtQuarterProgress() {
        val st = PlayerUiState(title = "Test track", isPlaying = true, positionMs = 47_500L, durationMs = 190_000L, hasTrack = true)
        rule.setContent { MusicAppTheme { PlayerScreen(onBack = {}, fixture = st, sourceLabel = "Local music", progressInFixture = true) } }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/outputs/roborazzi/player_progress_25_screen.png")
    }

    private fun harness(log: MutableList<String>) = rule.setContent {
        Box(Modifier.size(100.dp).testTag("btn").tapOrHold({ log += "tap" }, { log += "holdStart" }, { log += "holdEnd" }))
    }

    @Test fun shortPressIsTap() {
        val log = mutableListOf<String>(); harness(log)
        rule.onNodeWithTag("btn").performTouchInput { click() }
        rule.waitForIdle()
        assertEquals(listOf("tap"), log)
    }

    @Test fun longPressIsHoldNotTap() {
        val log = mutableListOf<String>(); harness(log)
        rule.onNodeWithTag("btn").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(600)
        rule.onNodeWithTag("btn").performTouchInput { up() }
        rule.waitForIdle()
        assertEquals(listOf("holdStart", "holdEnd"), log)
    }
}
