package com.abhi.musicapp

import com.abhi.musicapp.playback.Crossfader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure maths for the AutoMix transition plan and the post-swap speed ease. Made-up tempos only:
 * this says the numbers are right, not that a mix sounds right - that needs a phone.
 */
class AutoMixTransitionTest {

    @Test fun missingTempoOnEitherSideIsPlainCrossfade() {
        assertEquals(4000L to 1f, Crossfader.plannedTransition(null, 120f, 4, 180_000L))
        assertEquals(4000L to 1f, Crossfader.plannedTransition(120f, null, 4, 180_000L))
        assertEquals(4000L to 1f, Crossfader.plannedTransition(null, null, 4, 180_000L))
    }

    @Test fun plainCrossfadeIsCappedAtThirdOfPlayingSong() {
        assertEquals(3000L to 1f, Crossfader.plannedTransition(null, 120f, 12, 9_000L))
    }

    @Test fun closeTemposGiveWholeBarFadeAndNudge() {
        // 120 into 124 bpm, 8 s limit: bar at 120 bpm is 2000 ms, 4 bars fit -> 8000 ms fade, nudge 120/124.
        val (fade, nudge) = Crossfader.plannedTransition(120f, 124f, 8, 180_000L)
        assertEquals(8000L, fade)
        assertEquals(120f / 124f, nudge, 0.001f)
    }

    @Test fun farApartTemposFallBackToPlain() {
        assertEquals(6000L to 1f, Crossfader.plannedTransition(90f, 128f, 6, 180_000L))
    }

    @Test fun beatMatchedFadeIsCappedByPlayingSongToo() {
        // 128 bpm, 12 s limit plans 11250 ms, but a 9 s playing song caps any fade at 3000 ms.
        val (fade, nudge) = Crossfader.plannedTransition(128f, 128f, 12, 9_000L)
        assertEquals(3000L, fade)
        assertEquals(1f, nudge, 0.001f)
    }

    @Test fun tooShortPlayingSongFallsBackToPlain() {
        // 1.2 s song caps any fade at 400 ms, under the 500 ms floor: plain plan (which is then skipped as too short downstream).
        assertEquals(400L to 1f, Crossfader.plannedTransition(120f, 120f, 6, 1_200L))
    }

    @Test fun halfAndDoubleTempoStillMatch() {
        val (fade, nudge) = Crossfader.plannedTransition(70f, 140f, 8, 180_000L)
        assertEquals(1f, nudge, 0.001f)
        assert(fade in 1..8000)
    }

    @Test fun speedEaseRunsLinearlyAndEnds() {
        assertEquals(1.05f, Crossfader.restoreSpeedAt(1.05f, 1f, 0)!!, 0.0001f)
        assertEquals(1.025f, Crossfader.restoreSpeedAt(1.05f, 1f, 750)!!, 0.0001f)
        assertNull(Crossfader.restoreSpeedAt(1.05f, 1f, 1500))
        assertNull(Crossfader.restoreSpeedAt(1.05f, 1f, 5000))
    }

    @Test fun speedEaseIsMonotonicTowardTheTarget() {
        var prev = Crossfader.restoreSpeedAt(0.95f, 1f, 0)!!
        for (ms in 40L..1440L step 40) {
            val s = Crossfader.restoreSpeedAt(0.95f, 1f, ms)!!
            assert(s >= prev)
            prev = s
        }
        assertEquals(1f, Crossfader.restoreSpeedAt(0.95f, 1f, 1440)!!, 0.002f)
    }

    @Test fun nudgeNeverExceedsSixPercent() {
        // Any matched plan must stay inside the planner's own cap.
        for (a in listOf(60f, 90f, 120f, 150f, 178f)) {
            for (b in listOf(60f, 90f, 120f, 150f, 178f)) {
                val (_, nudge) = Crossfader.plannedTransition(a, b, 8, 180_000L)
                assert(kotlin.math.abs(nudge - 1f) <= 0.06f + 0.001f)
            }
        }
    }
}
