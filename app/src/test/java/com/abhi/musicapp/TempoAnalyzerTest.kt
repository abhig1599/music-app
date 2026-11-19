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
        assertNotNull(r.bpm); assertEquals(bpm, r.bpm!!, 2.5f)
    }
    @Test fun noiseIsUnknown() {
        val rnd = Random(7); val x = FloatArray(sr * 30) { (rnd.nextFloat() - 0.5f) * 0.5f }
        assertNull(TempoAnalyzer.analyze(x, sr).bpm)
    }
    @Test fun silenceAndShortAreUnknown() {
        assertNull(TempoAnalyzer.analyze(FloatArray(sr * 30), sr).bpm)
        assertNull(TempoAnalyzer.analyze(clicks(120f, 3), sr).bpm)
    }
    /** Kick, snare, hats, a pad and a melody tone, encoded to 96 kbps MP3 and decoded back. Still synthetic, not a real song. */
    @Test fun findsTempoInMp3RoundTripFixture() {
        val b = javaClass.classLoader!!.getResourceAsStream("synthetic_128bpm_mp3_roundtrip.wav")!!.readBytes()
        val rate = java.nio.ByteBuffer.wrap(b, 24, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int
        var o = 12; var dataAt = -1; var dataLen = 0
        while (o + 8 <= b.size) {
            val id = String(b, o, 4); val len = java.nio.ByteBuffer.wrap(b, o + 4, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int
            if (id == "data") { dataAt = o + 8; dataLen = len; break }
            o += 8 + len
        }
        val sb = java.nio.ByteBuffer.wrap(b, dataAt, dataLen.coerceAtMost(b.size - dataAt)).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val x = FloatArray(sb.remaining()) { sb.get(it) / 32768f }
        val r = TempoAnalyzer.analyze(x, rate)
        assertNotNull(r.bpm); assertEquals(128f, r.bpm!!, 3f)
    }
}
