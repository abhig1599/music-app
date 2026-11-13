package com.abhi.musicapp

import com.abhi.musicapp.playback.BandAnalyzer
import com.abhi.musicapp.playback.FFT_SIZE
import com.abhi.musicapp.playback.LEVEL_BANDS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class BandAnalyzerTest {
    private fun tone(hz: Double, rate: Int = 44100) = FloatArray(FFT_SIZE) { (0.5 * sin(2 * PI * hz * it / rate)).toFloat() }

    // Low bands are narrower than one FFT bin, so several bands can share a bin: check the winning band covers the tone's bin.
    private fun inBand(k: Int, hz: Double, rate: Int = 44100): Boolean {
        val e = BandAnalyzer.binEdges(rate); val bin = (hz * FFT_SIZE / rate).toInt()
        return bin >= e[k] && bin < maxOf(e[k] + 1, e[k + 1])
    }

    @Test fun silenceIsAllZero() {
        val l = BandAnalyzer().analyze(FloatArray(FFT_SIZE), 44100)
        assertTrue(l.all { it == 0f })
    }

    @Test fun lowToneLightsLowBandsHighToneLightsHighBands() {
        val a = BandAnalyzer()
        val low = a.analyze(tone(100.0), 44100)
        val lowPeak = low.indices.maxByOrNull { low[it] }!!
        assertTrue(inBand(lowPeak, 100.0))
        val b = BandAnalyzer()
        val high = b.analyze(tone(8000.0), 44100)
        val highPeak = high.indices.maxByOrNull { high[it] }!!
        assertTrue(inBand(highPeak, 8000.0))
        assertTrue(highPeak > lowPeak + 20)
        assertEquals(1f, high[highPeak], 1e-3f)
        assertTrue(high[2] < 0.2f)
    }

    @Test fun edgesAreMonotonicAndInRange() {
        for (r in listOf(22050, 44100, 48000)) {
            val e = BandAnalyzer.binEdges(r)
            for (i in 1 until e.size) assertTrue(e[i] >= e[i - 1])
            assertTrue(e.last() < FFT_SIZE / 2)
        }
    }
}
