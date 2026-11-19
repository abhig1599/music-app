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
}
