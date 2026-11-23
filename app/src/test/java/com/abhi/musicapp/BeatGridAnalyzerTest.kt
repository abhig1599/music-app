package com.abhi.musicapp

import com.abhi.musicapp.analysis.BeatGridAnalyzer
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class BeatGridAnalyzerTest {
    private val sr = 22050
    private fun clickTrack(bpm: Float, offset: Float): FloatArray {
        val out = FloatArray(sr * 24)
        var t = offset
        while (t < 23.5f) {
            val p = (t * sr).roundToInt()
            for (j in 0 until 600) out[p + j] += (.8 * exp(-j / 90.0) * sin(2 * PI * 1500 * j / sr)).toFloat()
            t += 60f / bpm
        }
        return out
    }
    @Test fun phaseOffsetIsMeasuredNotAssumedZero() {
        val offset = .237f; val bpm = 120f
        val r = BeatGridAnalyzer.analyze(clickTrack(bpm, offset), sr)
        assertTrue("missing beat grid: $r", r.timesSeconds.size >= 30)
        assertTrue(r.confidence >= .65f)
        for (t in r.timesSeconds) {
            val nearest = offset + ((t - offset) / .5f).roundToInt() * .5f
            assertTrue("phase error ${t - nearest}", abs(t - nearest) < .035f)
        }
    }
    @Test fun distinctTemposAndOffsets() {
        for ((bpm, offset) in listOf(90f to .13f, 140f to .31f)) {
            val r = BeatGridAnalyzer.analyze(clickTrack(bpm, offset), sr)
            assertTrue("missing grid at $bpm", r.timesSeconds.size >= 20)
            assertEquals(bpm, r.bpm!!, 2.5f)
        }
    }
    @Test fun randomNoiseAbstains() {
        val random = java.util.Random(7)
        assertTrue(BeatGridAnalyzer.analyze(FloatArray(sr * 24) { (random.nextFloat() - .5f) * .5f }, sr).timesSeconds.isEmpty())
    }
    @Test fun incompatibleTempoChangeAbstains() {
        val first = clickTrack(120f, .237f).copyOfRange(0, sr * 16)
        val second = clickTrack(90f, .237f).copyOfRange(0, sr * 16)
        assertTrue(BeatGridAnalyzer.analyze(first + second, sr).timesSeconds.isEmpty())
    }
    @Test fun nonfiniteInputAbstains() {
        val x = clickTrack(120f, .237f); x[4] = Float.NaN
        assertTrue(BeatGridAnalyzer.analyze(x, sr).timesSeconds.isEmpty())
    }
    @Test fun silenceAndShortInputAbstain() {
        assertTrue(BeatGridAnalyzer.analyze(FloatArray(sr * 24), sr).timesSeconds.isEmpty())
        assertTrue(BeatGridAnalyzer.analyze(FloatArray(sr), sr).timesSeconds.isEmpty())
    }
}
