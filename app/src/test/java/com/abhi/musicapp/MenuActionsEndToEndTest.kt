package com.abhi.musicapp

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.library.BinStore
import com.abhi.musicapp.playback.PlayerUiState
import com.abhi.musicapp.playback.SleepState
import com.abhi.musicapp.playback.SleepTimer
import com.abhi.musicapp.ui.screens.PlayerScreen
import com.abhi.musicapp.ui.screens.RecycleBinScreen
import com.abhi.musicapp.ui.screens.SleepTimerSheet
import com.abhi.musicapp.ui.theme.MusicAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Behavior-level checks for the player menu actions and the bin's Delete forever confirmation.
 * Robolectric only: Speed is excluded (PlayerManager.setSpeed needs the live MediaController, so it is device-only),
 * and the actual MediaStore file deletion needs the phone's system consent dialog. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], instrumentedPackages = ["androidx.loader.content"], qualifiers = "w390dp-h844dp")
class MenuActionsEndToEndTest {
    @get:Rule val rule = createComposeRule()

    private val track = PlayerUiState(title = "Some Song", isPlaying = true, positionMs = 1000L, durationMs = 200000L, hasTrack = true, mediaId = 42L)

    @Test fun shareBuildsAudioSendIntent() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        com.abhi.musicapp.ui.screens.shareCurrent(app, 42L, "Some Song")
        val chooser = shadowOf(app).nextStartedActivity
        assertEquals(android.content.Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<android.content.Intent>(android.content.Intent.EXTRA_INTENT)!!
        assertEquals(android.content.Intent.ACTION_SEND, send.action)
        assertEquals("audio/*", send.type)
        @Suppress("DEPRECATION")
        assertEquals(
            android.content.ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 42L),
            send.getParcelableExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM))
    }

    @Test fun removeConfirmHidesSongFromLibrary() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        BinStore(ctx).restore(42L) // clean slate
        rule.setContent { MusicAppTheme { PlayerScreen(onBack = {}, fixture = track, initialOverlay = 10, progressInFixture = true) } }
        rule.waitForIdle()
        rule.onNodeWithText("Remove").performClick() // exact match: the confirm button, not the "Remove from library?" title
        rule.waitForIdle()
        assertTrue(BinStore(ctx).removedIds().contains(42L))
        rule.onNodeWithText("Removed from the library", substring = true).assertExists()
        BinStore(ctx).restore(42L)
    }

    @Test fun equaliserMenuTapFiresCallback() {
        var fired = false
        rule.setContent { MusicAppTheme { PlayerScreen(onBack = {}, initialOverlay = 1, onOpenEqualiser = { fired = true }) } }
        rule.waitForIdle()
        rule.onNodeWithTag("menu_Equaliser").performClick()
        rule.waitForIdle()
        assertTrue(fired)
    }

    @Test fun sleepSheetPresetStartsTimer() {
        SleepTimer.cancel()
        rule.setContent { MusicAppTheme { SleepTimerSheet {} } }
        rule.waitForIdle()
        rule.onNodeWithText("10 min. later").performClick()
        rule.waitForIdle()
        val st = SleepTimer.state.value
        assertTrue(st is SleepState.Counting)
        assertEquals(600_000L, (st as SleepState.Counting).totalMs)
        SleepTimer.cancel()
    }

    @Test fun binDeleteForeverAsksInAppFirst() {
        val song = com.abhi.musicapp.library.Song(id = 7L, title = "Track X", artist = "A", durationMs = 1L,
            uri = android.net.Uri.parse("content://media/external/audio/media/7"), folder = "Music")
        rule.setContent { MusicAppTheme { RecycleBinScreen(onBack = {}, fixtureEntries = listOf(BinStore.Entry(7L, 1_763_900_000_000L) to song)) } }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Delete forever").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("permanently deleted from this phone", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Cancel").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("permanently deleted from this phone", substring = true).assertDoesNotExist()
    }
}
