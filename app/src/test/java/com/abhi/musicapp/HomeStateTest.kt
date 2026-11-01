package com.abhi.musicapp

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.playback.PlayerUiState
import com.abhi.musicapp.ui.screens.HomeScreen
import com.abhi.musicapp.ui.theme.MusicAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * State assertions (Robolectric, no device): real empty library with no audio permission granted,
 * and fixture states. These check rendered semantics only. They do not prove runtime behaviour on a device.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], instrumentedPackages = ["androidx.loader.content"], qualifiers = "w390dp-h844dp")
class HomeStateTest {
    @get:Rule val rule = createComposeRule()

    @Test fun shippedHomeWithNoPermissionShowsEmptyStateNotSampleRows() {
        rule.setContent { MusicAppTheme { HomeScreen({}, {}, {}, {}, {}, {}) } } // fixture = null, Robolectric app has no audio permission
        rule.waitForIdle()
        rule.onNodeWithText("Music permission is off, so your songs can't be read. Tap here to open system settings.").assertExists()
        rule.onNodeWithText("Love songs").assertDoesNotExist()
        rule.onNodeWithContentDescription("Playing").assertDoesNotExist()
        rule.onNodeWithText("Song name in a line...").assertDoesNotExist()
        rule.onNodeWithText("Up to date").assertDoesNotExist()
        rule.onNodeWithText("Not checked").assertExists()
    }

    @Test fun baseFixtureHasNoPlayingIndicatorOrMiniPlayer() {
        rule.setContent { MusicAppTheme { HomeScreen({}, {}, {}, {}, {}, {}, fixture = PlayerUiState()) } }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Playing").assertDoesNotExist()
        rule.onNodeWithText("Song name in a line...").assertDoesNotExist()
    }

    @Test fun loadedTrackFixtureShowsMiniPlayer() {
        rule.setContent { MusicAppTheme { HomeScreen({}, {}, {}, {}, {}, {}, fixture = PlayerUiState(
            title = "Song name in a line...", isPlaying = true, positionMs = 61_000L, durationMs = 192_000L, hasTrack = true)) } }
        rule.waitForIdle()
        rule.onNodeWithText("Song name in a line...").assertExists()
    }

    @Test fun shippedHomeChipCountsAreNotFigmaSamples() {
        rule.setContent { MusicAppTheme { HomeScreen({}, {}, {}, {}, {}, {}) } }
        rule.waitForIdle()
        rule.onNodeWithText("20").assertDoesNotExist()
        rule.onNodeWithText("06").assertDoesNotExist()
    }

    @Test fun shippedHomeUnavailableTilesAreDisabled() {
        rule.setContent { MusicAppTheme { HomeScreen({}, {}, {}, {}, {}, {}) } }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Read Now (unavailable)").assertExists()
        rule.onNodeWithContentDescription("Lyrics (unavailable)").assertExists()
    }

    @Test fun playlistDetailEmptyStateWhenSongsNotResolvable() {
        rule.setContent { MusicAppTheme { com.abhi.musicapp.ui.screens.PlaylistDetailScreen("Gym", {}) } }
        rule.waitForIdle()
        rule.onNodeWithText("No playable songs in this playlist. Add songs from the player menu (Add to playlist).").assertExists()
    }
}
