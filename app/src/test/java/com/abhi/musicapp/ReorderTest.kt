package com.abhi.musicapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.library.PlaylistStore
import com.abhi.musicapp.library.Song
import com.abhi.musicapp.ui.components.dropIndex
import com.abhi.musicapp.ui.components.moveItem
import com.abhi.musicapp.ui.screens.PlaylistSongList
import com.abhi.musicapp.ui.screens.QueueEntry
import com.abhi.musicapp.ui.screens.QueueSheetContent
import com.abhi.musicapp.ui.theme.MusicAppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class ReorderTest {
    @get:Rule val rule = createComposeRule()

    private fun song(i: Int) = Song(i.toLong(), "Song $i", "Artist $i", 180000L, android.net.Uri.parse("content://x/$i"))

    @Test fun moveAndDropMaths() {
        assertEquals(listOf(1, 2, 0, 3), moveItem(listOf(0, 1, 2, 3), 0, 2))
        assertEquals(listOf(3, 0, 1, 2), moveItem(listOf(0, 1, 2, 3), 3, 0))
        assertEquals(listOf(0, 1), moveItem(listOf(0, 1), 5, 0))
        assertEquals(2, dropIndex(0, 160f, 76f, 5)); assertEquals(0, dropIndex(1, -500f, 76f, 5)); assertEquals(4, dropIndex(3, 900f, 76f, 5))
        assertEquals(1, dropIndex(1, 20f, 76f, 5))
    }

    @Test fun storeSetOrderKeepsMissingSongs() {
        val f = File.createTempFile("plst", ".json"); f.delete()
        val st = PlaylistStore(f); st.create("A"); listOf(1L, 2L, 3L, 4L).forEach { st.addSong("A", it) }
        st.setOrder("A", listOf(3L, 1L, 2L)) // 4 is "not on the phone"
        assertEquals(listOf(3L, 1L, 2L, 4L), st.load().first().songIds)
        st.setOrder("A", listOf(9L, 4L)) // unknown id ignored
        assertEquals(listOf(4L, 3L, 1L, 2L), st.load().first().songIds)
    }

    @Test fun dragHandleReordersPlaylistRows() {
        var songs by mutableStateOf((1..5).map { song(it) })
        var played = -1
        rule.setContent {
            MusicAppTheme { Box(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
                PlaylistSongList(songs, onPlay = { played = it }, onMove = { f, t -> songs = moveItem(songs, f, t) })
            } }
        }
        rule.onRoot().captureRoboImage("build/outputs/roborazzi/playlist_list_before.png")
        val rowPx = 76f * 3f // xxhdpi
        rule.onNodeWithTag("handle_0", useUnmergedTree = true).performTouchInput {
            down(center); moveBy(Offset(0f, rowPx * 0.9f)); moveBy(Offset(0f, rowPx * 0.9f)) // about 1.7 rows after touch slop: lands on index 2
        }
        rule.onRoot().captureRoboImage("build/outputs/roborazzi/playlist_list_dragging.png")
        rule.onNodeWithTag("handle_0", useUnmergedTree = true).performTouchInput { up() }
        rule.waitForIdle()
        assertEquals(listOf(2, 3, 1, 4, 5), songs.map { it.id.toInt() })
        rule.onRoot().captureRoboImage("build/outputs/roborazzi/playlist_list_after.png")
        assertEquals(-1, played) // a drag never starts playback
    }

    @Test fun queueSheetRender() {
        var items by mutableStateOf((1..6).map { QueueEntry("$it", "Song $it", "Artist $it") })
        rule.setContent { MusicAppTheme { QueueSheetContent(items, current = 1, onMove = { f, t -> items = moveItem(items, f, t) }, onPlay = {}, onClose = {}) } }
        rule.onRoot().captureRoboImage("build/outputs/roborazzi/queue_sheet.png")
        rule.onNodeWithTag("qhandle_4", useUnmergedTree = true).performTouchInput { down(center); moveBy(Offset(0f, -70f * 3f * 1.4f)); up() }
        rule.waitForIdle()
        assertEquals(listOf("1", "2", "3", "5", "4", "6"), items.map { it.id })
    }
}
