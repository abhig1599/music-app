package com.abhi.musicapp

import com.abhi.musicapp.playback.Crossfader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure maths only: no audio is played. Real overlap, speed changes and hand-over need a phone. */
class CrossfaderMathTest {
    @Test fun equalPowerKeepsLoudness() {
        for (i in 0..20) { val (a, b) = Crossfader.gains(i / 20f); assertEquals(1f, a * a + b * b, 0.001f) }
        assertEquals(1f, Crossfader.gains(0f).first, 0.001f)
        assertEquals(1f, Crossfader.gains(1f).second, 0.001f)
    }
    @Test fun beginWindowAtNormalSpeed() {
        assertTrue(Crossfader.shouldBegin(5000, 4000, 1f))
        assertFalse(Crossfader.shouldBegin(6000, 4000, 1f))
        assertFalse(Crossfader.shouldBegin(200, 4000, 1f))
    }
    @Test fun beginWindowFollowsSpeed() {
        // 2x: 10 s of media left is 5 s of real time, so it must start. 0.5x: 4 s of media is 8 s of real time, too early.
        assertTrue(Crossfader.shouldBegin(10_000, 4000, 2f))
        assertFalse(Crossfader.shouldBegin(4_000, 4000, 0.5f))
        assertTrue(Crossfader.shouldBegin(2_500, 4000, 0.5f))
    }
    @Test fun givesUpWaitingForIncomingAfterFourSeconds() {
        assertFalse(Crossfader.shouldGiveUp(3999L)); assertTrue(Crossfader.shouldGiveUp(4001L))
    }
    @Test fun nextSongDurationCapsTheFade() {
        assertEquals(6000L, Crossfader.effectiveFade(6000L, 180_000L)) // long song: unchanged
        assertEquals(2000L, Crossfader.effectiveFade(6000L, 6000L))    // short song: a third of it
        assertEquals(0L, Crossfader.effectiveFade(6000L, 1200L))       // too short to fade into
    }
    @Test fun unknownOrInvalidNextDurationKeepsThePlannedFade() {
        assertEquals(5000L, Crossfader.effectiveFade(5000L, androidx.media3.common.C.TIME_UNSET))
        assertEquals(5000L, Crossfader.effectiveFade(5000L, 0L))
        assertEquals(5000L, Crossfader.effectiveFade(5000L, -5L))
    }
    @Test fun fadeWindowWaitsForTheBoundaryAlignedStart() {
        // Before the planned start: -1 (keep the incoming deck paused). On time: the full planned fade. Late: shorter, never longer.
        assertEquals(-1L, Crossfader.fadeWindow(9_999L, 10_000L, 4000L))
        assertEquals(4000L, Crossfader.fadeWindow(10_000L, 10_000L, 4000L))
        assertEquals(3700L, Crossfader.fadeWindow(10_300L, 10_000L, 4000L))
        assertEquals(400L, Crossfader.fadeWindow(13_600L, 10_000L, 4000L)) // too late: the caller treats <500 ms as "skip the fade"
    }
}
