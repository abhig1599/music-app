package com.abhi.musicapp

import com.abhi.musicapp.analysis.AutoMixPlanner
import com.abhi.musicapp.analysis.TempoResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure maths on made-up tempos. Says nothing about how a mix sounds. */
class AutoMixPlannerTest {
    private fun t(b: Float?) = TempoResult(b, if (b == null) 0f else 1f)

    @Test fun unknownTempoFallsBackToPlainCrossfade() {
        val p = AutoMixPlanner.plan(t(null), t(120f), 6)
        assertFalse(p.beatMatched); assertEquals(6000L, p.fadeMs); assertEquals(1f, p.incomingSpeed, 0f)
        assertFalse(AutoMixPlanner.plan(t(120f), t(null), 6).beatMatched)
    }
    @Test fun closeTemposMatchAndFadeIsWholeBars() {
        val p = AutoMixPlanner.plan(t(120f), t(124f), 8) // beat 500 ms, bar 2000 ms, 4 bars fit in 8 s
        assertTrue(p.beatMatched); assertEquals(8000L, p.fadeMs); assertEquals(120f / 124f, p.incomingSpeed, 0.001f)
    }
    @Test fun fadeNeverExceedsUserLimit() {
        for (s in 1..12) assertTrue(AutoMixPlanner.plan(t(128f), t(128f), s).fadeMs <= s * 1000L)
    }
    @Test fun farApartTemposFallBack() {
        assertFalse(AutoMixPlanner.plan(t(90f), t(128f), 6).beatMatched)
    }
    @Test fun halfTempoIsRecognised() {
        val p = AutoMixPlanner.plan(t(70f), t(140f), 8)
        assertTrue(p.beatMatched); assertEquals(1f, p.incomingSpeed, 0.001f)
    }
    @Test fun weakAndInvalidEvidenceCannotChangePlaybackSpeed() {
        for (bad in listOf(TempoResult(120f,.74f),TempoResult(120f,Float.NaN),
            TempoResult(Float.NaN,1f),TempoResult(120f,1.1f))) {
            val p=AutoMixPlanner.plan(bad,t(124f),8)
            assertFalse(p.beatMatched); assertEquals(1f,p.incomingSpeed,0f)
        }
    }
}
