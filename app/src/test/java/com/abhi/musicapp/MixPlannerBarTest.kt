package com.abhi.musicapp

import com.abhi.musicapp.analysis.MixPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bar-aware candidate preference on made-up grids: among candidates that already pass every beat gate,
 * confident bar evidence on both grids prefers the start whose matched pairs share a bar position.
 * Without evidence the choice is exactly the pre-existing one. Synthetic grids only.
 */
class MixPlannerBarTest {
    private fun beats(bpm: Float, startMs: Long, count: Int): List<Long> {
        val period = 60000.0 / bpm
        return (0 until count).map { startMs + (it * period).toLong() }
    }

    private fun plan(outBarPos: Int? = null, inBarPos: Int? = null, outBarConf: Float = 0f, inBarConf: Float = 0f) =
        MixPlanner.plan(
            beats(120f, 160_000L, 100), 120f, .9f, 210_000L, 198_700L,
            beats(120f, 0L, 80), 120f, .9f, 200_000L, 12_000L, null, null,
            outBarPos, inBarPos, outBarConf, inBarConf)

    @Test fun withoutBarEvidenceTheBeatOnlyChoiceIsUnchanged() {
        // Same fixture as MixPlannerTest: nearest-to-anchor candidate 190500 wins on residual ties.
        assertEquals(190_500L, plan()!!.outStartMs)
    }

    @Test fun barEvidencePrefersTheBarAlignedStart() {
        // ob[0] and ib[0] are both downbeats (position 0). Candidates near the anchor: 190500 (ob index 61,
        // bar position 1), 191000 (62 -> 2), 190000 (60 -> 0). Only 190000 is bar-aligned with the cue.
        val p = plan(outBarPos = 0, inBarPos = 0, outBarConf = .9f, inBarConf = .9f)!!
        assertEquals(190_000L, p.outStartMs)
        assertEquals(198_700L, p.outEndMs) // content boundary untouched
        assertTrue(p.maxResidualMs <= MixPlanner.MAX_PHASE_ERROR_MS)
        assertTrue(p.reason.contains("bar-matched"))
    }

    @Test fun weakBarConfidenceKeepsTheBeatOnlyChoice() {
        val p = plan(outBarPos = 0, inBarPos = 0, outBarConf = .1f, inBarConf = .9f)!!
        assertEquals(190_500L, p.outStartMs)
    }

    @Test fun missingBarPositionKeepsTheBeatOnlyChoice() {
        val p = plan(outBarPos = null, inBarPos = 0, outBarConf = .9f, inBarConf = .9f)!!
        assertEquals(190_500L, p.outStartMs)
    }

    @Test fun decisionCallbackReportsWhyAFallbackHappened() {
        // Diagnostics only: the callback observes the fallback reason and never changes the outcome.
        var reason: String? = null
        val p = MixPlanner.plan(
            beats(120f, 160_000L, 100), 120f, .9f, 210_000L, 198_700L,
            beats(128.4f, 0L, 80), 128.4f, .9f, 200_000L, 12_000L, null, null,
            null, null, 0f, 0f) { reason = it }
        assertNull(p)
        assertNotNull(reason)
        assertTrue(reason!!.isNotBlank())
    }

    @Test fun halfTimeGridWithBarEvidenceStillPlans() {
        val p = MixPlanner.plan(
            beats(120f, 160_000L, 100), 120f, .9f, 210_000L, 198_700L,
            beats(60f, 0L, 80), 60f, .9f, 200_000L, 12_000L, null, null,
            0, 0, .9f, .9f)
        assertNotNull(p)
        assertTrue(p!!.maxResidualMs <= MixPlanner.MAX_PHASE_ERROR_MS)
    }
}
