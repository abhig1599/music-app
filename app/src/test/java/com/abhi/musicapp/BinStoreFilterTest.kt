package com.abhi.musicapp

import android.net.Uri
import com.abhi.musicapp.library.BinStore
import com.abhi.musicapp.library.Song
import org.junit.Assert.assertEquals
import org.junit.Test

/** Recycle Bin filtering: removed songs drop out of the library view; nothing is reordered, deleted or edited. */
class BinStoreFilterTest {
    private fun song(id: Long) = Song(id, "t$id", "a", 1000L, Uri.EMPTY, "Music")

    @Test fun emptyBinKeepsEverything() {
        val songs = listOf(song(1), song(2), song(3))
        assertEquals(songs, BinStore.filterRemoved(songs, emptySet()))
    }

    @Test fun removedIdsDropOutOrderPreserved() {
        val songs = listOf(song(1), song(2), song(3), song(4))
        assertEquals(listOf(song(1), song(3)), BinStore.filterRemoved(songs, setOf(2L, 4L)))
    }

    @Test fun unknownIdsRemoveNothing() {
        val songs = listOf(song(1), song(2))
        assertEquals(songs, BinStore.filterRemoved(songs, setOf(999L)))
    }
}
