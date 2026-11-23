package com.abhi.musicapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
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
        composeRule.onNodeWithText("Privacy policy: not published yet").assertDoesNotExist() // collapsed by default
        val header = composeRule.onNodeWithText("About")
        header.performScrollTo(); header.performClick(); composeRule.waitForIdle()
        composeRule.onNodeWithText("Privacy policy: not published yet").performScrollTo().assertIsDisplayed()
        val node = composeRule.onNodeWithText("About the maker (portfolio)")
        node.performScrollTo()
        node.assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/narrow360_settings_scrolled_to_about.png")
        node.performClick()
        val intent = org.robolectric.Shadows.shadowOf(androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()).nextStartedActivity
        org.junit.Assert.assertEquals("https://abhicreative.world", intent.data.toString())
    }

    private fun libraryFixture(): List<com.abhi.musicapp.library.Song> = (1..30).map { i ->
        com.abhi.musicapp.library.Song(
            id = i.toLong(), title = "Track %02d".format(i), artist = "Artist ${(i - 1) / 10 + 1}", durationMs = 200_000L,
            uri = android.net.Uri.parse("content://media/external/audio/media/$i"),
            folder = if (i <= 15) "Music/Download" else "Music/Recordings")
    }

    /** Shipped Home (real library rows, no design fixture): the whole page scrolls, the header leaves, the mini player stays pinned. */
    @Test fun homeShippedScrolledMiniPlayerPinned() {
        composeRule.setContent { FitDesignWidth { MusicAppTheme { bg { HomeScreen({}, {}, {}, {}, {}, {}, fixture = track, songsFixture = libraryFixture()) }() } } }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Not checked").assertIsDisplayed() // header pill visible before scrolling
        repeat(3) { composeRule.onRoot().performTouchInput { swipeUp() }; composeRule.waitForIdle() }
        composeRule.onNodeWithText("Not checked").assertDoesNotExist() // header scrolled away with the page
        composeRule.onNodeWithText("Track 30").assertIsDisplayed() // list actually scrolled to the bottom
        composeRule.onNodeWithText("Tera Naam Japdi Phiran", substring = true).assertIsDisplayed() // mini player stayed pinned
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/narrow360_home_shipped_scrolled.png")
    }

    /** Shipped folder drill-in: Home passes the picked folder, the screen opens directly inside it. */
    @Test fun folderDrillInShipped() {
        composeRule.setContent { FitDesignWidth { MusicAppTheme { bg { com.abhi.musicapp.ui.screens.FoldersScreen(onBack = {}, initialFolder = "Download", songsFixture = libraryFixture()) }() } } }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Track 01").assertIsDisplayed()
        composeRule.onNodeWithText("Track 10").assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/narrow360_folder_drill_in.png")
    }

    /** Tapping a folder in the chooser opens its songs directly - no second chooser step. */
    @Test fun folderTapDrillsInDirectly() {
        composeRule.setContent { FitDesignWidth { MusicAppTheme { bg { com.abhi.musicapp.ui.screens.FoldersScreen(onBack = {}, songsFixture = libraryFixture()) }() } } }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Download").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Track 01").assertIsDisplayed()
    }

    /** Shipped bin: real removal rows show the removal date and never a countdown; nothing auto-deletes. */
    @Test fun recycleBinShippedPopulated() {
        val songs = libraryFixture()
        val entries = listOf(
            com.abhi.musicapp.library.BinStore.Entry(3L, 1_763_900_000_000L) to songs[2],
            com.abhi.musicapp.library.BinStore.Entry(7L, 1_763_800_000_000L) to songs[6],
        )
        composeRule.setContent { FitDesignWidth { MusicAppTheme { bg { com.abhi.musicapp.ui.screens.RecycleBinScreen(onBack = {}, fixtureEntries = entries) }() } } }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Track 03").assertIsDisplayed()
        composeRule.onNodeWithText("days left", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("file untouched", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("nothing is ever deleted automatically", substring = true).assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/narrow360_recycle_bin_shipped.png")
    }

    /** Shipped remove-from-library confirmation (player menu): app-hide wording, file stays on the phone. */
    @Test fun playerRemoveConfirmShipped() {
        composeRule.setContent { FitDesignWidth { MusicAppTheme { bg { PlayerScreen(onBack = {}, fixture = track, initialOverlay = 10, progressInFixture = true) }() } } }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Remove from library?").assertIsDisplayed()
        composeRule.onNodeWithText("The file stays on your phone untouched", substring = true).assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/narrow360_player_remove_confirm.png")
    }
}
