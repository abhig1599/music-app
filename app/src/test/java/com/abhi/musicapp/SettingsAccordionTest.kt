package com.abhi.musicapp

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.library.Song
import com.abhi.musicapp.library.VisibilityStore
import com.abhi.musicapp.ui.screens.SettingsScreen
import com.abhi.musicapp.ui.theme.MusicAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], instrumentedPackages = ["androidx.loader.content"], qualifiers = "w390dp-h844dp")
class SettingsAccordionTest {
    @get:Rule val rule = createComposeRule()

    private fun state(title: String) =
        rule.onNodeWithTag("accordion:$title").fetchSemanticsNode().config[SemanticsProperties.StateDescription]

    @Test fun sectionsStartCollapsedAndToggleIndependently() {
        rule.setContent { MusicAppTheme { SettingsScreen(onBack = {}) } }
        rule.waitForIdle()
        for (t in listOf("Permission", "Check for updates", "Visibility", "About")) assertEquals("Collapsed", state(t))
        rule.onNodeWithTag("accordion:Permission").performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals("Expanded", state("Permission"))
        assertEquals("Collapsed", state("About"))
        rule.onNodeWithText("Notifications").assertExists()
        rule.onNodeWithTag("accordion:Permission").performClick()
        rule.waitForIdle()
        assertEquals("Collapsed", state("Permission"))
        rule.onNodeWithText("Notifications").assertDoesNotExist()
    }

    private fun song(id: Long, folder: String) = Song(id, "t$id", "a", 1000L, android.net.Uri.EMPTY, folder)

    @Test fun hiddenFoldersAreFilteredByFullPathAndRestorable() {
        val songs = listOf(song(1, "Recordings"), song(2, "Music/Recordings"), song(3, "Music"), song(4, ""))
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = VisibilityStore(ctx)
        store.setHidden("Recordings", true)
        assertEquals(listOf(2L, 3L, 4L), VisibilityStore.filterVisible(songs, VisibilityStore(ctx).hidden()).map { it.id }) // persisted, new instance
        store.setHidden("Recordings", false)
        assertEquals(listOf(1L, 2L, 3L, 4L), VisibilityStore.filterVisible(songs, VisibilityStore(ctx).hidden()).map { it.id })
        store.setHidden("", true) // no folder info: cannot be hidden
        assertEquals(emptySet<String>(), store.hidden())
    }
}
