package com.abhi.musicapp

import com.abhi.musicapp.library.PlaylistStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.robolectric.annotation.Config
import java.io.File

/** Robolectric test of persistence logic (file round trip; org.json needs Robolectric). Not a device test. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PlaylistStoreTest {
    private fun store() = PlaylistStore(File.createTempFile("pl", ".json").also { it.delete() })

    @Test fun createAddReloadRemoveDelete() {
        val s = store()
        assertTrue(s.create("Gym"))
        assertFalse(s.create("gym"))
        assertFalse(s.create("  "))
        assertTrue(s.addSong("Gym", 5L))
        assertTrue(s.addSong("Gym", 5L))
        assertTrue(s.addSong("Gym", 9L))
        assertFalse(s.addSong("Nope", 1L))
        assertEquals(listOf(5L, 9L), s.load().single().songIds)
        s.removeSong("Gym", 5L)
        assertEquals(listOf(9L), s.load().single().songIds)
        s.delete("Gym")
        assertTrue(s.load().isEmpty())
    }
}
