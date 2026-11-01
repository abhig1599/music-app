package com.abhi.musicapp

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.library.PlaylistStore
import com.abhi.musicapp.ui.screens.HomeScreen
import com.abhi.musicapp.ui.screens.PlayerScreen
import com.abhi.musicapp.ui.theme.MusicAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Robolectric entry-path tests (callbacks and overlays). Not NavHost or device tests. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp")
class NavigationEntryTest {
    @get:Rule val rule = createComposeRule()

    @Test fun homePlaylistChipRowInvokesOpenPlaylistWithName() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        java.io.File(ctx.filesDir, "playlists.json").delete()
        PlaylistStore(ctx).create("Gym")
        var opened: String? = null
        rule.setContent { MusicAppTheme { HomeScreen({}, {}, {}, {}, {}, { opened = it }) } }
        rule.onNode(hasText("Playlist") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Gym").performClick()
        rule.waitForIdle()
        assertEquals("Gym", opened)
    }

    @Test fun playerMenuAddToPlaylistOpensSheetOnShippedPath() {
        rule.setContent { MusicAppTheme { PlayerScreen(onBack = {}) } }
        rule.onNodeWithContentDescription("Menu").performClick()
        rule.onNodeWithText("Add to playlist").performClick()
        rule.waitForIdle()
        // Shipped path with no loaded track: the sheet opens and says so (mediaId = -1).
        rule.onNodeWithText("No song is loaded.").assertExists()
    }

    @Test fun playerMenuAddToPlaylistInFixtureDoesNotOpenShippedSheet() {
        rule.setContent { MusicAppTheme { PlayerScreen(onBack = {}, fixture = com.abhi.musicapp.playback.PlayerUiState(title = "x", hasTrack = true, mediaId = 1L), initialOverlay = 1) } }
        rule.onNodeWithText("Add to playlist").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("No song is loaded.").assertDoesNotExist()
        rule.onNodeWithText("New playlist").assertDoesNotExist()
    }
}
