package com.abhi.musicapp

import com.abhi.musicapp.analysis.PhaseTransitionPlanner as P
import org.junit.Assert.*
import org.junit.Test

class PhaseTransitionPlannerTest {
    private fun grid(start: Long, end: Long, period: Long, confidence: Float = .9f) =
        P.Grid(generateSequence(start) { it + period }.takeWhile { it <= end }.toList(), 60000f / period, confidence)

    @Test fun shiftedBeatPhasePreservesBothCompleteSongs() {
        val p = P.plan(grid(20000, 30000, 500), grid(237, 10000, 500), 30000, 4000)
        assertTrue(p.reason, p.aligned)
        assertEquals(30000, p.startOutgoingMs + p.fadeMs)
        assertTrue(p.fadeMs in 500L..4000L)
        assertEquals(1f, p.incomingSpeedRatio, .0001f)
        assertTrue(p.predictedMaxPhaseErrorMs!! <= 1f)
        // No incoming seek or trim parameter exists in this plan.
    }
    @Test fun jitteryGridCannotPassOnOneAccidentalMatch() {
        val bad = P.Grid(listOf(0L,500L,1100L,1510L,2200L,2500L,3100L,3500L,4100L),120f,.95f)
        assertFalse(P.plan(grid(20000,30000,500),bad,30000,4000).aligned)
    }
    @Test fun confidenceAndIncompatibleTempoFallBack() {
        val a = grid(20000,30000,500)
        assertFalse(P.plan(a,grid(0,10000,500,.74f),30000,4000).aligned)
        assertFalse(P.plan(a,grid(0,10000,667),30000,4000).aligned)
    }
    @Test fun invalidGridAndShortSongsAreSafe() {
        assertFalse(P.plan(grid(0,10000,500),P.Grid(listOf(0,0,500,1000),120f,1f),30000,4000).aligned)
        assertFalse(P.plan(grid(0,10000,500),grid(0,10000,500),1000,4000).aligned)
    }
    @Test fun changingDurationDoesNotOverrunNaturalEnd() {
        for (duration in 28000L..31000L step 137L) {
            val p = P.plan(grid(20000,32000,500),grid(237,10000,500),duration,4000)
            assertEquals(duration,p.startOutgoingMs+p.fadeMs)
        }
    }
    @Test fun invalidNumbersAndDisabledFadeCannotActivate() {
        val g = grid(0,40000,500)
        for (bad in listOf(g.copy(bpm=Float.NaN), g.copy(confidence=Float.NaN),
            g.copy(bpm=Float.POSITIVE_INFINITY), g.copy(confidence=1.1f))) {
            assertFalse(P.plan(g,bad,30000,4000).aligned)
        }
        assertEquals(0L,P.plan(g,g,30000,0).fadeMs)
        assertEquals(0L,P.plan(g,g,-1,4000).fadeMs)
        assertEquals(0L,P.plan(g,g,-1,4000).startOutgoingMs)
    }
    @Test fun nearbyTempoUsesCorrectDirectionWithoutSkippingHead() {
        val a = grid(20000,30000,500)
        val b = grid(237,10000,520)
        val p = P.plan(a,b,30000,4000)
        assertTrue(p.reason,p.aligned)
        assertEquals(1.04f,p.incomingSpeedRatio,.0001f)
        assertTrue(p.predictedMaxPhaseErrorMs!! < 2f)
    }
}
