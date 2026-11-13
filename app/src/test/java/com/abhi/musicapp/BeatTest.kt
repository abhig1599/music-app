package com.abhi.musicapp

import com.abhi.musicapp.playback.BandAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

class BeatTest {
    private val rate = 44100
    private fun pcm(seconds: Double, amp: (Double) -> Double, hz: Double): ByteBuffer {
        val n = (seconds * rate).toInt()
        val b = ByteBuffer.allocate(n * 2).order(ByteOrder.nativeOrder())
        for (i in 0 until n) { val t = i.toDouble() / rate; b.putShort((amp(t) * sin(2 * PI * hz * t) * 32767).toInt().toShort()) }
        b.flip(); return b
    }

    /** Max beat value seen while pushing [buf] through [a] in 50 ms slices. */
    private fun maxBeat(a: BandAnalyzer, buf: ByteBuffer): Float {
        var m = 0f
        val slice = rate / 20 * 2
        while (buf.remaining() > 0) {
            val n = minOf(slice, buf.remaining()); val part = buf.slice().limit(n) as ByteBuffer
            buf.position(buf.position() + n)
            if (a.push(part) != null) m = maxOf(m, a.beat)
        }
        return m
    }

    private fun analyzer() = BandAnalyzer().also { it.configure(rate, 1, androidx.media3.common.C.ENCODING_PCM_16BIT) }

    @Test fun steadyBassDoesNotPulse() {
        val a = analyzer()
        maxBeat(a, pcm(1.0, { 0.4 }, 70.0)) // let the average settle
        assertEquals(0f, maxBeat(a, pcm(1.5, { 0.4 }, 70.0)), 0.01f)
    }

    @Test fun silenceDoesNotPulse() = assertEquals(0f, maxBeat(analyzer(), pcm(1.0, { 0.0 }, 70.0)), 0.0001f)

    @Test fun kickAfterQuietPulsesAndDecays() {
        val a = analyzer()
        maxBeat(a, pcm(1.0, { 0.02 }, 70.0)) // quiet bed
        val hit = maxBeat(a, pcm(0.12, { 0.8 }, 70.0)) // kick
        assertTrue("beat $hit", hit > 0.45f)
        maxBeat(a, pcm(0.0, { 0.0 }, 70.0))
        val later = maxBeat(a, pcm(0.0, { 0.0 }, 70.0))
        assertTrue(later <= hit)
    }
}
