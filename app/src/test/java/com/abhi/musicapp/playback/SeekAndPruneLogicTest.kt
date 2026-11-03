package com.abhi.musicapp.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeekAndPruneLogicTest {
    @Test fun forwardPlayingAddsExtra100() = assertEquals(10_100L, seekHoldTarget(1, true, 10_000, 190_000))
    @Test fun forwardPausedAdds200() = assertEquals(10_200L, seekHoldTarget(1, false, 10_000, 190_000))
    @Test fun backwardPlayingSubtracts300() = assertEquals(9_700L, seekHoldTarget(-1, true, 10_000, 190_000))
    @Test fun speedScalesHold() {
        assertEquals(10_200L, seekHoldTarget(1, true, 10_000, 190_000, 2.0f))
        assertEquals(9_400L, seekHoldTarget(-1, true, 10_000, 190_000, 2.0f))
        assertEquals(10_050L, seekHoldTarget(1, true, 10_000, 190_000, 0.5f))
        assertEquals(9_850L, seekHoldTarget(-1, true, 10_000, 190_000, 0.5f))
        assertEquals(10_400L, seekHoldTarget(1, false, 10_000, 190_000, 2.0f))
    }
    @Test fun backwardClampsAtZero() = assertEquals(0L, seekHoldTarget(-1, true, 100, 190_000))
    @Test fun forwardStopsOneSecondBeforeEnd() = assertEquals(189_000L, seekHoldTarget(1, false, 188_900, 190_000))
    @Test fun forwardAtLimitStops() = assertNull(seekHoldTarget(1, true, 189_000, 190_000))
    @Test fun unknownDurationStopsForward() = assertNull(seekHoldTarget(1, true, 5_000, -9_223_372_036_854_775_807L))
    @Test fun shortTrackStopsForward() = assertNull(seekHoldTarget(1, true, 0, 800))

    private val folders = mapOf("1" to "/a", "2" to "/b", "3" to "/a", "4" to "/b", "5" to "")
    @Test fun pruneOnlyUpcomingHidden() {
        val ids = listOf("1", "2", "3", "4", "5")
        // current index 1 ("2", folder /b, hidden): it stays; upcoming "3" (/a) removed; "4" (/b) removed; "5" (no folder) kept; "1" is before current.
        assertEquals(listOf(3, 2), upcomingIndicesToRemove(ids, 1, folders, setOf("/a", "/b")))
    }
    @Test fun pruneNothingWhenNoneHidden() = assertEquals(emptyList<Int>(), upcomingIndicesToRemove(listOf("1", "2"), 0, folders, emptySet()))
    @Test fun pruneUnknownIdsKept() = assertEquals(emptyList<Int>(), upcomingIndicesToRemove(listOf("9", "8"), 0, folders, setOf("/a")))
}
