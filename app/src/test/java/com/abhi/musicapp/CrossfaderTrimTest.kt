package com.abhi.musicapp

import com.abhi.musicapp.playback.Crossfader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Pure maths for the level-trim path: the RMS trim must never step the incoming deck's volume at the
 * final fade step, the swap, or a mid-fade fallback. Player-free: drives the same companion functions the
 * 40 ms loop composes in step(), swap() and dropPhase().
 */
class CrossfaderTrimTest {
    private val boosted = 1.9953f // +6 dB clamp from MixPlanner.gainTrim
    private val cut = 0.5012f // -6 dB clamp

    @Test fun trimTargetHoldsThenEasesToUnity() {
        assertEquals(boosted, Crossfader.trimTargetAt(0f, boosted), 0f)
        assertEquals(boosted, Crossfader.trimTargetAt(0.5f, boosted), 0f)
        assertEquals(boosted, Crossfader.trimTargetAt(Crossfader.TRIM_EASE_T, boosted), 0f)
        assertEquals(1f, Crossfader.trimTargetAt(1f, boosted), 0.0001f)
        assertEquals(1f, Crossfader.trimTargetAt(1f, cut), 0.0001f)
        assertEquals((boosted + 1f) / 2f, Crossfader.trimTargetAt((Crossfader.TRIM_EASE_T + 1f) / 2f, boosted), 0.0001f)
        assertEquals(1f, Crossfader.trimTargetAt(0.5f, 1f), 0f)
    }

    @Test fun longFadeEndsAtUnitySoSwapIsContinuous() {
        for (gain in listOf(boosted, cut)) {
            var trim = 1f
            var prev = trim
            val steps = 200 // 8000 ms fade at a 40 ms loop
            for (i in 1..steps) {
                val t = i / steps.toFloat()
                trim = Crossfader.slewTrim(trim, Crossfader.trimTargetAt(t, gain))
                assertTrue("trim stepped by ${trim - prev} at t=$t", abs(trim - prev) <= 0.0301f)
                prev = trim
            }
            assertEquals(1f, trim, 0.0201f) // swap() then writes base volume: no audible step
        }
    }

    @Test fun shortestFadeLeavesBoundedResidueForPostSwapEase() {
        var trim = 1f
        val steps = 12 // 500 ms fade at a 40 ms loop
        for (i in 1..steps) trim = Crossfader.slewTrim(trim, Crossfader.trimTargetAt(i / steps.toFloat(), boosted))
        assertTrue("residue $trim outside expected bounds", trim in 1f..1.35f)
        // The post-swap ease then ramps the residue to unity over 400 ms in small equal steps.
        var prev = trim
        for (ms in 0L..399L step 40L) {
            val f = Crossfader.trimEaseAt(trim, ms)!!
            assertTrue(abs(f - prev) <= 0.1f)
            prev = f
        }
        assertNull(Crossfader.trimEaseAt(trim, 400L))
    }

    @Test fun midFadeFallbackEasesTrimWithoutAStep() {
        var trim = boosted // dropPhase() mid-fade makes the target 1f instantly
        var steps = 0
        while (trim != 1f && steps < 100) {
            val next = Crossfader.slewTrim(trim, 1f)
            assertTrue(next < trim) // monotonic toward unity
            assertTrue(trim - next <= 0.0301f) // never a jump
            trim = next
            steps++
        }
        assertEquals(1f, trim, 0f)
        assertTrue("took $steps steps", steps <= 40) // <= 1.6 s on the 40 ms loop
    }

    @Test fun unityTrimStaysUnity() {
        var trim = 1f
        for (i in 1..100) trim = Crossfader.slewTrim(trim, Crossfader.trimTargetAt(i / 100f, 1f))
        assertEquals(1f, trim, 0f)
    }
}
