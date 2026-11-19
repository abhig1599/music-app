package com.abhi.musicapp

import com.abhi.musicapp.analysis.TempoAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Synthetic click tracks only. Real songs are unverified until the user tests them. */
class TempoAnalyzerTest {
    private val sr = 22050
    private fun clicks(bpm: Float, seconds: Int, noise: Float = 0.02f): FloatArray {
        val out = FloatArray(sr * seconds); val rnd = Random(1)
        for (i in out.indices) out[i] = (rnd.nextFloat() - 0.5f) * noise
        val period = sr * 60f / bpm; var t = 0f
        while (t < out.size - 2000) {
            for (i in 0 until 1500) out[t.toInt() + i] += (exp(-i / 300.0) * sin(2 * PI * 1000 * i / sr)).toFloat() * 0.8f
            t += period
        }
        return out
    }
    @Test fun finds90() = check(90f)
    @Test fun finds120() = check(120f)
    @Test fun finds140() = check(140f)
    private fun check(bpm: Float) {
        val r = TempoAnalyzer.analyze(clicks(bpm, 30), sr)
        assertNotNull(r.bpm); assertEquals(bpm, r.bpm!!, 2f)
    }
    @Test fun noiseIsUnknown() {
        val rnd = Random(7); val x = FloatArray(sr * 30) { (rnd.nextFloat() - 0.5f) * 0.5f }
        assertNull(TempoAnalyzer.analyze(x, sr).bpm)
    }
    @Test fun silenceAndShortAreUnknown() {
        assertNull(TempoAnalyzer.analyze(FloatArray(sr * 30), sr).bpm)
        assertNull(TempoAnalyzer.analyze(clicks(120f, 3), sr).bpm)
    }
}
