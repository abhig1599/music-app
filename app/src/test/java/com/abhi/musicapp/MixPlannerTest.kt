package com.abhi.musicapp

import com.abhi.musicapp.analysis.MixPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure maths on made-up beat grids. Verifies the planning rules (whole bars, beat start, tempo match,
 * phase tolerance, level trim, content-end boundary), never how a mix sounds on real songs.
 */
class MixPlannerTest {
    private fun beats(bpm: Float, startMs: Long, count: Int): List<Long> {
        val period = 60000.0 / bpm
        return (0 until count).map { startMs + (it * period).toLong() }
    }

    private fun plan(
        outBeats: List<Long>? = beats(120f, 160_000L, 100),
        outBpm: Float? = 120f,
        outConf: Float = .9f,
        outDur: Long = 210_000L,
        contentEnd: Long? = 198_700L,
        inBeats: List<Long>? = beats(120f, 0L, 80),
        inBpm: Float? = 120f,
        inConf: Float = .9f,
        inDur: Long? = 200_000L,
        fadeMs: Long = 12_000L,
        outRms: Float? = null,
        inRms: Float? = null,
    ) = MixPlanner.plan(outBeats, outBpm, outConf, outDur, contentEnd, inBeats, inBpm, inConf, inDur, fadeMs, outRms, inRms)

    @Test fun alignedGridsProduceWholeBarOverlapEndingAtContentEnd() {
        val p = plan()
        assertNotNull(p); p!!
        // 8 bars (16 s) exceed the 12 s limit, so 4 bars = 8000 ms; the nearest outgoing beat to 198700-8000 is 190500.
        assertEquals(190_500L, p.outStartMs)
        assertEquals(198_700L, p.outEndMs) // exactly the detected content end: nothing audible is cut
        assertEquals(8_200L, p.overlapMs)
        assertEquals(1f, p.incomingSpeed, 0f)
        assertEquals(1f, p.inGain, 0f)
        assertEquals(0.0, p.maxResidualMs, 0.001)
    }

    @Test fun closeTempoNudgesIncomingAndBeatsStillAlign() {
        // 126 bpm nudged by 120/126 ~ -4.8% runs at the outgoing tempo, so the grids overlay.
        val p = plan(inBeats = beats(126f, 0L, 90), inBpm = 126f)
        assertNotNull(p); p!!
        assertEquals(120f / 126f, p.incomingSpeed, 0.001f)
        assertTrue(p.maxResidualMs <= MixPlanner.MAX_PHASE_ERROR_MS)
    }

    @Test fun incompatibleTempoFallsBack() {
        assertNull(plan(inBeats = beats(200f, 0L, 130), inBpm = 200f))
    }

    @Test fun weakOrMissingEvidenceFallsBack() {
        assertNull(plan(outConf = .74f))
        assertNull(plan(inConf = .74f))
        assertNull(plan(outBeats = null))
        assertNull(plan(inBeats = null))
        assertNull(plan(outBpm = null))
        assertNull(plan(outBeats = beats(120f, 160_000L, 4))) // too few beats
    }

    @Test fun phaseOffsetBeyondToleranceFallsBack() {
        // The incoming train sits 120 ms off the outgoing one and the cue stays at 0: no safe overlay exists.
        assertNull(plan(inBeats = beats(120f, 120L, 80)))
    }

    @Test fun fileEndIsUsedWhenNoContentEndDetected() {
        val p = plan(contentEnd = null)
        assertNotNull(p); p!!
        assertEquals(210_000L, p.outEndMs)
        assertTrue(p.outEndMs <= 210_000L)
    }

    @Test fun overlapNeverExceedsTheUserFadeCap() {
        for (secs in 1..12) {
            val p = plan(fadeMs = secs * 1000L) ?: continue
            assertTrue("cap ${secs}s but overlap ${p.overlapMs}", p.overlapMs <= secs * 1000L)
            assertTrue(p.overlapMs >= 500L)
        }
    }

    @Test fun gainTrimMatchesLevelAndClamps() {
        assertEquals(1f, MixPlanner.gainTrim(null, .1f), 0f)
        assertEquals(1f, MixPlanner.gainTrim(.1f, null), 0f)
        assertEquals(1f, MixPlanner.gainTrim(.2f, .2f), 0.0001f)
        // Incoming half as loud: +6 dB, clamped there.
        assertEquals(1.9953f, MixPlanner.gainTrim(.2f, .1f), 0.001f)
        // Incoming twice as loud: -6 dB, clamped.
        assertEquals(0.5012f, MixPlanner.gainTrim(.1f, .2f), 0.001f)
        // Beyond the clamp stays at the clamp.
        assertEquals(1.9953f, MixPlanner.gainTrim(.4f, .05f), 0.001f)
    }

    @Test fun planCarriesThroughTheCrossfaderGate() {
        val p = plan()!!
        // Same sanity gate the Crossfader applies at arm time.
        assertTrue(com.abhi.musicapp.playback.Crossfader.selectMix(p, true, 190_000L, 210_000L) === p)
        assertNull(com.abhi.musicapp.playback.Crossfader.selectMix(p, false, 190_000L, 210_000L))
        assertNull(com.abhi.musicapp.playback.Crossfader.selectMix(p, true, 195_000L, 210_000L)) // start already passed
        assertNull(com.abhi.musicapp.playback.Crossfader.selectMix(p, true, 190_000L, 195_000L)) // end beyond the song
        assertNull(com.abhi.musicapp.playback.Crossfader.selectMix(null, true, 0L, 210_000L))
    }
}
