package com.abhi.musicapp

import com.abhi.musicapp.analysis.BarPhaseAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Synthetic click tracks only: a broadband tick on every beat plus a low kick burst on chosen beats.
 * Verifies the analyzer's evidence rules (detection, ties, abstention, grid-phase tolerance), never how
 * real songs sound.
 */
class BarPhaseAnalyzerTest {
    private val sr = 22050

    private fun synth(bpm: Float, offset: Float, seconds: Float, kick: (Int) -> Boolean): Pair<FloatArray, List<Float>> {
        val out = FloatArray((sr * seconds).toInt())
        val beats = ArrayList<Float>()
        var k = 0
        var t = offset
        val period = 60f / bpm
        while (t < seconds - .5f) {
            val p = (t * sr).roundToInt()
            for (j in 0 until 600) if (p + j < out.size)
                out[p + j] = (out[p + j] + .8 * exp(-j / 90.0) * sin(2 * PI * 1500 * j / sr)).toFloat()
            if (kick(k)) for (j in 0 until 1500) if (p + j < out.size)
                out[p + j] = (out[p + j] + 1.2 * exp(-j / 220.0) * sin(2 * PI * 55 * j / sr)).toFloat()
            beats.add(t); k++; t += period
        }
        return out to beats
    }

    @Test fun downbeatDetectedFromKickAccents() {
        val (samples, beats) = synth(120f, .237f, 24f) { it % 4 == 1 }
        val r = BarPhaseAnalyzer.analyze(samples, sr, beats)!!
        assertEquals(1, r.downbeatOffset)
        assertTrue("confidence ${r.confidence}", r.confidence >= .5f)
        assertFalse(r.twoBeatAmbiguity)
        assertTrue(r.bars >= BarPhaseAnalyzer.MIN_BARS)
    }

    @Test fun strongPairTieReportedAsTwoBeatAmbiguity() {
        val (samples, beats) = synth(120f, .237f, 24f) { it % 2 == 0 }
        val r = BarPhaseAnalyzer.analyze(samples, sr, beats)!!
        assertTrue(r.twoBeatAmbiguity)
        assertTrue(r.confidence >= BarPhaseAnalyzer.MIN_CONFIDENCE)
    }

    @Test fun uniformOrMissingAccentsAbstain() {
        assertNull(synth(120f, .237f, 24f) { true }.let { (s, b) -> BarPhaseAnalyzer.analyze(s, sr, b) })
        assertNull(synth(120f, .237f, 24f) { false }.let { (s, b) -> BarPhaseAnalyzer.analyze(s, sr, b) })
    }

    @Test fun gridPhaseErrorWithin30MsKeepsTheSamePhase() {
        // Evidence is measured at the given beat times; the windows tolerate a +/-30 ms grid phase error.
        for (shift in listOf(-.03f, -.02f, 0f, .02f, .03f)) {
            val (samples, beats) = synth(120f, .237f + shift, 24f) { it % 4 == 2 }
            assertEquals("shift $shift", 2, BarPhaseAnalyzer.analyze(samples, sr, beats)?.downbeatOffset)
        }
    }

    @Test fun differentTempoAndPhaseDetected() {
        val (samples, beats) = synth(96f, .13f, 26f) { it % 4 == 3 }
        assertEquals(3, BarPhaseAnalyzer.analyze(samples, sr, beats)?.downbeatOffset)
    }

    @Test fun tooFewBeatsAbstains() {
        val (samples, beats) = synth(120f, .237f, 6f) { it % 4 == 1 }
        assertTrue(beats.size < BarPhaseAnalyzer.BEATS_PER_BAR * BarPhaseAnalyzer.MIN_BARS)
        assertNull(BarPhaseAnalyzer.analyze(samples, sr, beats))
    }

    @Test fun invalidInputAbstains() {
        val beats = (0 until 40).map { it * .5f }
        assertNull(BarPhaseAnalyzer.analyze(FloatArray(sr * 24), sr, beats)) // silence: no accents
        assertNull(BarPhaseAnalyzer.analyze(FloatArray(sr * 24), sr, beats.sortedDescending()))
        assertNull(BarPhaseAnalyzer.analyze(FloatArray(sr * 24), sr, beats.map { it - 5f }))
    }
}
