package com.abhi.musicapp

import com.abhi.musicapp.analysis.ContentEndDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

/**
 * The tail detector acts only on near-zero SAMPLE evidence (per-frame peak over all channels), requires
 * complete coverage of the media end (clamped overhang; undercoverage only via EOS proof), and always
 * retains [ContentEndDetector.RETAINED_QUIET_MS] of quiet inside the boundary. Quiet music, reverb, hiss,
 * sparse transients, out-of-phase content, broken input and truncated decodes must all abstain.
 */
class ContentEndDetectorTest {
    private val sr = 44100
    private val durMs = 200_000L
    private val frameLen = sr * ContentEndDetector.FRAME_MS / 1000
    private val retain = ContentEndDetector.RETAINED_QUIET_MS

    /** Interleaved PCM for a [windowSec] window; [peakAt] gives the sine amplitude (linear) per second, per channel. */
    private fun pcm(windowSec: Int, channels: Int = 1, peakAt: (Double, Int) -> Double): FloatArray {
        val frames = windowSec * sr
        return FloatArray(frames * channels) { i ->
            val f = i / channels; val c = i % channels
            val t = f.toDouble() / sr
            (peakAt(t, c) * sin(2 * Math.PI * 220 * t)).toFloat()
        }
    }

    private fun peaks(samples: FloatArray, channels: Int = 1) = ContentEndDetector.framePeaks(samples, channels, frameLen)
    // Production always passes eos = true: decodeTailWindow returns null unless the codec signaled end-of-stream.
    private fun detect(p: FloatArray, startMs: Long = durMs - 10_000, coverEndMs: Long = durMs, dur: Long = durMs, eos: Boolean = true) =
        ContentEndDetector.detect(p, startMs, coverEndMs, dur, eos)

    @Test fun nearZeroTailIsTrimmedWithRetainedMargin() {
        val p = peaks(pcm(10) { t, _ -> if (t < 7.0) 0.5 else 0.0 })
        val r = detect(p)
        assertEquals("trailing near-zero samples", r.reason)
        assertEquals(3000L - retain, r.trimMs) // 250 ms of quiet stays inside the boundary
        assertEquals(durMs - (3000L - retain), r.contentEndMs)
    }

    @Test fun quietMusicalTailAbstains() {
        val p = peaks(pcm(10) { t, _ -> if (t < 7.0) 0.5 else 0.01 }) // -40 dBFS peak music
        assertNull(detect(p).contentEndMs)
    }

    @Test fun hissAboveFloorAbstains() {
        val p = peaks(pcm(10) { t, _ -> if (t < 7.0) 0.5 else 3.2e-4 }) // -70 dBFS peak hiss
        assertNull(detect(p).contentEndMs)
    }

    @Test fun belowThresholdNoiseIsTrimmed_andThatIsTheDocumentedBehavior() {
        // -85 dBFS peak: near-zero sample evidence. Trimmed by design; NOT a claim of inaudibility.
        val p = peaks(pcm(10) { t, _ -> if (t < 7.0) 0.5 else 5.6e-5 })
        assertEquals(3000L - retain, detect(p).trimMs)
    }

    @Test fun sparseTransientIsNotTrimmed() {
        // One -20 dBFS click half a second before the end: per-frame PEAK evidence catches it (an RMS frame
        // could have averaged it away). The click and 250 ms of the quiet after it survive.
        val p = peaks(pcm(10) { t, _ ->
            when {
                t < 7.0 -> 0.5
                t in 9.45..9.55 -> 0.1
                else -> 0.0
            }
        })
        val r = detect(p)
        assertEquals(450L - retain, r.trimMs)
        assertEquals(durMs - (450L - retain), r.contentEndMs)
    }

    @Test fun outOfPhaseStereoTailIsNotCancelledToSilence() {
        // L = -R tail: a mono mixdown would read zero; per-channel peak evidence must not.
        val p = peaks(pcm(10, channels = 2) { t, c -> if (t < 7.0) 0.5 else if (c == 0) 0.02 else -0.02 }, channels = 2)
        assertNull(detect(p).contentEndMs)
    }

    @Test fun nanAbstains() {
        val p = peaks(pcm(10) { t, _ -> if (t < 7.0) 0.5 else 0.0 })
        p[p.size - 3] = Float.NaN
        assertNull(detect(p).contentEndMs)
    }

    @Test fun infinityAbstains() {
        val p = peaks(pcm(10) { t, _ -> if (t < 7.0) 0.5 else 0.0 })
        p[p.size - 2] = Float.POSITIVE_INFINITY
        assertNull(detect(p).contentEndMs)
    }

    @Test fun truncatedDecodeAbstains() {
        val p = peaks(pcm(10) { t, _ -> if (t < 7.0) 0.5 else 0.0 })
        // Decoder gave up 2 s before the file end (no EOS): the quiet tail cannot be proven.
        assertNull(detect(p, coverEndMs = durMs - 2_000, eos = false).contentEndMs)
        // ANY undercoverage without EOS abstains - even 20 ms.
        assertNull(detect(p, coverEndMs = durMs - 20, eos = false).contentEndMs)
        // With EOS, the decoded end IS the actual media end: 100 ms of container overstatement is accepted
        // and the boundary lands on the actual end minus the trim.
        val r = detect(p, coverEndMs = durMs - 100, eos = true)
        assertEquals(3000L - retain, r.trimMs)
        assertEquals(durMs - 100 - (3000L - retain), r.contentEndMs)
        // With EOS but the declared duration far exceeding the stream, the file is broken: abstain.
        assertNull(detect(p, coverEndMs = durMs - 2_000, eos = true).contentEndMs)
    }

    @Test fun overhangBeyondFileEndIsClampedAndSubtracted() {
        val p = peaks(pcm(10) { t, _ -> if (t < 7.0) 0.5 else 0.0 })
        val r = detect(p, coverEndMs = durMs + 500, eos = false) // decoder padded 500 ms past the file end
        assertEquals(2500L - retain, r.trimMs)
        assertEquals(durMs - (2500L - retain), r.contentEndMs)
    }

    @Test fun staleDurationAbstains() {
        val p = peaks(pcm(10) { t, _ -> if (t < 7.0) 0.5 else 0.0 })
        assertNull(detect(p, dur = 0).contentEndMs)
        assertNull(detect(p, startMs = 300_000).contentEndMs) // window starts past the file end
    }

    @Test fun cancellationAbstains() {
        val p = peaks(pcm(10) { t, _ -> if (t < 7.0) 0.5 else 0.0 })
        assertNull(ContentEndDetector.detect(p, durMs - 10_000, durMs, durMs, true) { true }.contentEndMs)
    }

    @Test fun shortSilenceBelowMinimumAbstains() {
        // 500 ms of quiet minus the 250 ms retained margin is below MIN_TRIM: not worth trimming.
        val p = peaks(pcm(10) { t, _ -> if (t < 9.5) 0.5 else 0.0 })
        assertNull(detect(p).contentEndMs)
    }

    @Test fun overlongSilenceAbstains() {
        val p = peaks(pcm(15) { t, _ -> if (t < 2.0) 0.5 else 0.0 }) // 13 s quiet: not the target case
        assertNull(detect(p, startMs = durMs - 15_000).contentEndMs)
    }

    @Test fun wholeWindowQuietAbstains() {
        val p = peaks(pcm(10) { _, _ -> 0.0 })
        assertNull(detect(p).contentEndMs)
    }

    @Test fun fadeOutTrimsAtMostItsSubFloorSliver() {
        // Linear-in-amplitude 3 s fade to zero: only the genuinely near-zero ending may go, below MIN_TRIM here.
        val p = peaks(pcm(10) { t, _ -> if (t < 7.0) 0.5 else 0.5 * (1.0 - (t - 7.0) / 3.0).coerceAtLeast(0.0) })
        val r = detect(p)
        if (r.contentEndMs != null) assertTrue("fade trim ${r.trimMs}", r.trimMs <= 200L)
    }

    @Test fun differentSampleRateSameVerdict() {
        val sr48 = 48000
        val fl = sr48 * ContentEndDetector.FRAME_MS / 1000
        val frames = 10 * sr48
        val samples = FloatArray(frames) { i ->
            val t = i.toDouble() / sr48
            (if (t < 7.0) 0.5 * sin(2 * Math.PI * 220 * t) else 0.0).toFloat()
        }
        val p = ContentEndDetector.framePeaks(samples, 1, fl)
        assertEquals(3000L - retain, ContentEndDetector.detect(p, durMs - 10_000, durMs, durMs, true).trimMs)
    }

    @Test fun emptyInputAbstains() {
        assertNull(ContentEndDetector.detect(FloatArray(0), 0, 1000, 1000, true).contentEndMs)
    }

    @Test fun framePeaksTakesMaxAcrossChannels() {
        val stereo = floatArrayOf(0.001f, -0.5f, 0.002f, 0.4f)
        val p = ContentEndDetector.framePeaks(stereo, 2, 1)
        assertEquals(0.5f, p[0], 1e-6f)
        assertEquals(0.4f, p[1], 1e-6f)
    }
}
