package com.abhi.musicapp

import com.abhi.musicapp.analysis.ContentEndDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow
import kotlin.math.sin

/**
 * The tail detector must trim ONLY encoder-level digital silence. Loud tails, quiet musical tails, reverb,
 * hiss and fade-outs must survive; anything ambiguous abstains (null) so the file end is used.
 */
class ContentEndDetectorTest {
    private val sr = 44100
    private val durMs = 200_000L

    /** Window of [windowSec] seconds ending at the file end; [levelAt] gives amplitude (dBFS) per second fraction. */
    private fun window(windowSec: Int, windowStartMs: Long, levelAt: (Double) -> Double): FloatArray {
        val n = windowSec * sr
        return FloatArray(n) { i ->
            val t = i.toDouble() / sr
            val db = levelAt(t)
            if (db < -120.0) 0f else (10.0.pow(db / 20.0).toFloat() * sin(2 * Math.PI * 220 * t)).toFloat()
        }
    }

    @Test fun digitalSilenceTailIsTrimmed() {
        // 10 s window: 7 s of music at -6 dBFS, then 3 s of pure zeros.
        val w = window(10, durMs - 10_000) { t -> if (t < 7.0) -6.0 else -999.0 }
        val r = ContentEndDetector.detect(w, sr, durMs - 10_000, durMs)
        assertEquals(3000L, r.trimMs)
        assertEquals(durMs - 3000L, r.contentEndMs)
    }

    @Test fun subMinimumSilenceIsLeftAlone() {
        // Only 100 ms of trailing silence: below MIN_TRIM_MS, not worth touching.
        val w = window(10, durMs - 10_000) { t -> if (t < 9.9) -6.0 else -999.0 }
        assertNull(ContentEndDetector.detect(w, sr, durMs - 10_000, durMs).contentEndMs)
    }

    @Test fun quietMusicalTailSurvives() {
        // -40 dBFS sustained tail: quiet but audible music, must NOT be trimmed.
        val w = window(10, durMs - 10_000) { t -> if (t < 7.0) -6.0 else -40.0 }
        assertNull(ContentEndDetector.detect(w, sr, durMs - 10_000, durMs).contentEndMs)
    }

    @Test fun linearFadeOutKeepsEssentiallyEverything() {
        // Amplitude-linear fade to zero over the last 3 s: only the final sub-floor sliver may go.
        val w = window(10, durMs - 10_000) { t ->
            if (t < 7.0) -6.0 else -6.0 + 20.0 * kotlin.math.log10(1.0 - (t - 7.0) / 3.0 + 1e-9)
        }
        val r = ContentEndDetector.detect(w, sr, durMs - 10_000, durMs)
        if (r.contentEndMs != null) assertTrue("fade trim ${r.trimMs} must stay tiny", r.trimMs <= 100L)
    }

    @Test fun noiseFloorTailSurvivesAboveFloor() {
        // Mastering hiss at -60 dBFS: above the floor, must NOT be trimmed.
        val w = window(10, durMs - 10_000) { t -> if (t < 7.0) -6.0 else -60.0 }
        assertNull(ContentEndDetector.detect(w, sr, durMs - 10_000, durMs).contentEndMs)
    }

    @Test fun loudTailSurvives() {
        val w = window(10, durMs - 10_000) { -6.0 }
        assertNull(ContentEndDetector.detect(w, sr, durMs - 10_000, durMs).contentEndMs)
    }

    @Test fun allQuietWindowAbstains() {
        val w = window(10, durMs - 10_000) { -999.0 }
        assertNull(ContentEndDetector.detect(w, sr, durMs - 10_000, durMs).contentEndMs)
    }

    @Test fun emptyInputAbstains() {
        assertNull(ContentEndDetector.detect(FloatArray(0), sr, durMs - 10_000, durMs).contentEndMs)
    }

    @Test fun reverbTailAboveFloorSurvives() {
        // Exponential decay ending at -55 dBFS (reverb ring-out): above the floor at the end, must survive.
        val w = window(10, durMs - 10_000) { t -> if (t < 7.0) -6.0 else -6.0 - (t - 7.0) * 16.33 }
        assertNull(ContentEndDetector.detect(w, sr, durMs - 10_000, durMs).contentEndMs)
    }
}
