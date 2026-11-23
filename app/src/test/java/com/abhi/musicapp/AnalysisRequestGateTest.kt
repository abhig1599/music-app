package com.abhi.musicapp

import com.abhi.musicapp.analysis.AnalysisRequestGate
import org.junit.Assert.*
import org.junit.Test

class AnalysisRequestGateTest {
    @Test fun durationUnknownThenKnownStartsOnce() {
        val g=AnalysisRequestGate()
        assertFalse(g.update(null)); assertTrue(g.update("pair:duration180000:fade4"))
        val generation=g.generation
        repeat(20) { assertFalse(g.update("pair:duration180000:fade4")) }
        assertTrue(g.accepts("pair:duration180000:fade4",generation))
    }
    @Test fun toggleQueueDurationAndFadeChangesInvalidateResults() {
        val g=AnalysisRequestGate(); g.update("pairA:180000:4"); val old=g.generation
        assertTrue(g.update(null)); assertFalse(g.accepts("pairA:180000:4",old))
        assertTrue(g.update("pairA:180000:4")); assertFalse(g.accepts("pairA:180000:4",old))
        assertTrue(g.update("pairB:180000:4")); assertTrue(g.update("pairB:180001:4"))
        assertTrue(g.update("pairB:180001:6"))
        val latest=g.generation; g.clear(); assertFalse(g.accepts("pairB:180001:6",latest))
    }
}
