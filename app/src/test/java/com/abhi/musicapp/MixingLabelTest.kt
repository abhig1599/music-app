package com.abhi.musicapp

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.playback.PlayerUiState
import com.abhi.musicapp.ui.screens.PlayerScreen
import com.abhi.musicapp.ui.theme.MusicAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Player source slot shows the gradient "Mixing..." label only while a transition is actually running. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], instrumentedPackages = ["androidx.loader.content"], qualifiers = "w390dp-h844dp")
class MixingLabelTest {
    @get:Rule val rule = createComposeRule()

    private fun fixture(mixing: Boolean) = PlayerUiState(
        title = "Song", artist = "Artist", isPlaying = true, positionMs = 1000L, durationMs = 200_000L, hasTrack = true, mixing = mixing,
    )

    @Test fun mixingStateShowsGradientLabelInsteadOfSource() {
        rule.setContent { MusicAppTheme { PlayerScreen(onBack = {}, fixture = fixture(true)) } }
        rule.waitForIdle()
        rule.onNodeWithText("Mixing...").assertExists()
        rule.onNodeWithText("[Playlist/Source]").assertDoesNotExist()
    }

    @Test fun normalStateKeepsSourceLabel() {
        rule.setContent { MusicAppTheme { PlayerScreen(onBack = {}, fixture = fixture(false)) } }
        rule.waitForIdle()
        rule.onNodeWithText("[Playlist/Source]").assertExists()
        rule.onNodeWithText("Mixing...").assertDoesNotExist()
    }
}
