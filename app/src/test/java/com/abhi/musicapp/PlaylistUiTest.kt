package com.abhi.musicapp

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.library.PlaylistStore
import com.abhi.musicapp.ui.screens.AddToPlaylistSheet
import com.abhi.musicapp.ui.theme.MusicAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Robolectric UI test: the shipped Add-to-playlist sheet writes to PlaylistStore. Not a device test. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp")
class PlaylistUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun createPlaylistFromSheetPersistsSong() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        java.io.File(ctx.filesDir, "playlists.json").delete()
        var done = false
        rule.setContent { MusicAppTheme { AddToPlaylistSheet(songId = 7L) { done = true } } }
        rule.onNodeWithText("New playlist").performClick()
        rule.onNodeWithText("Playlist Name...").performTextInput("Gym")
        rule.onNodeWithText("Create").performClick()
        rule.waitForIdle()
        val p = PlaylistStore(ctx).load().single()
        assertEquals("Gym", p.name)
        assertEquals(listOf(7L), p.songIds)
        assertEquals(true, done)
    }
}
