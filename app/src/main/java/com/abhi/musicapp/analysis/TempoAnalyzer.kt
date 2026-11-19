package com.abhi.musicapp.analysis

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Result of tempo analysis. [bpm] is null when no steady tempo was found. Nothing in playback uses this yet. */
data class TempoResult(val bpm: Float?, val confidence: Float)

/**
 * Local, offline tempo estimate from mono PCM: energy-flux onset envelope, then autocorrelation over 60..180 BPM.
 * Own code (no GPL library). Returns bpm = null below [MIN_CONFIDENCE] so callers must treat "unknown" as normal.
 * Tempo only: no key, no beat-matching, not used for any audible feature.
 */
object TempoAnalyzer {
    const val MIN_BPM = 60f
    const val MAX_BPM = 180f
    const val MIN_CONFIDENCE = 0.35f
    private const val HOP = 512
    private const val WIN = 1024

    fun analyze(samples: FloatArray, sampleRate: Int): TempoResult {
        if (sampleRate <= 0 || samples.size < sampleRate * 8) return TempoResult(null, 0f) // need >= 8 s
        val frames = (samples.size - WIN) / HOP
        if (frames < 64) return TempoResult(null, 0f)
        val energy = FloatArray(frames)
        for (f in 0 until frames) {
            var e = 0.0
            val o = f * HOP
            for (i in 0 until WIN) { val v = samples[o + i]; e += v * v }
            energy[f] = sqrt(e / WIN).toFloat()
        }
        // Positive flux, then remove the local mean so steady loudness does not look like rhythm.
        val flux = FloatArray(frames)
        for (f in 1 until frames) flux[f] = max(0f, energy[f] - energy[f - 1])
        val env = FloatArray(frames)
        for (f in 0 until frames) {
            var m = 0f; var c = 0
            for (k in max(0, f - 8)..minOf(frames - 1, f + 8)) { m += flux[k]; c++ }
            env[f] = max(0f, flux[f] - m / c)
        }
        var zero = 0.0
        for (v in env) zero += v * v
        if (zero < 1e-9) return TempoResult(null, 0f)
        val fps = sampleRate.toFloat() / HOP
        val minLag = (fps * 60f / MAX_BPM).roundToInt()
        val maxLag = (fps * 60f / MIN_BPM).roundToInt().coerceAtMost(frames / 2)
        val ac = FloatArray(maxLag + 2)
        for (lag in minLag - 1..maxLag + 1) {
            var s = 0.0
            for (i in 0 until frames - lag) s += env[i] * env[i + lag]
            ac[lag] = (s / zero).toFloat()
        }
        var best = minLag; var bestV = -1f
        for (lag in minLag..maxLag) if (ac[lag] > bestV && ac[lag] >= ac[lag - 1] && ac[lag] >= ac[lag + 1]) { bestV = ac[lag]; best = lag }
        if (bestV <= 0f) return TempoResult(null, 0f)
        // Parabolic refinement of the peak.
        val a = ac[best - 1]; val b = ac[best]; val c = ac[best + 1]
        val d = a - 2 * b + c
        val shift = if (abs(d) > 1e-9f) 0.5f * (a - c) / d else 0f
        val bpm = 60f * fps / (best + shift)
        // Confidence: peak height against the mean of the lag range.
        var mean = 0f
        for (lag in minLag..maxLag) mean += ac[lag]
        mean /= (maxLag - minLag + 1)
        val conf = ((bestV - mean) / max(bestV, 1e-6f)).coerceIn(0f, 1f)
        return if (conf >= MIN_CONFIDENCE) TempoResult(bpm, conf) else TempoResult(null, conf)
    }
}
