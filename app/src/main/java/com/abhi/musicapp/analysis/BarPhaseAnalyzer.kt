package com.abhi.musicapp.analysis

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Bar-phase (downbeat) evidence from the audio itself, tagged onto an existing beat grid. Beats are never
 * moved: this only estimates WHICH beat index starts a bar, from the low-band accent measured near each
 * given beat time. A genuine onset delay stays a delayed beat; it is never relabeled to look aligned.
 *
 * Model (4/4 assumed): the first beat of a bar tends to carry the strongest low-frequency (kick) accent.
 * For each beat, the accent is the low-band RMS in [t-30ms, t+100ms] minus the baseline RMS in
 * [t-160ms, t-60ms]; these windows tolerate +/-30 ms of beat-grid phase error. Accents are averaged per
 * bar position (beat index mod 4). The strong pair (positions p and p+2, the two kick positions in common
 * patterns) must clear the odd positions by a margin; a p/p+2 tie is reported as
 * [BarPhase.twoBeatAmbiguity] at reduced confidence, because either choice still lands strong beats
 * together. Weak or absent evidence returns null, so callers keep beat-only behaviour. Analysis only,
 * never called by playback directly.
 */
object BarPhaseAnalyzer {
    const val BEATS_PER_BAR = 4
    const val MIN_BARS = 8
    const val MIN_CONFIDENCE = .25f
    private const val LOWPASS_HZ = 120.0
    private const val BASE_START_S = .160
    private const val BASE_END_S = .060
    private const val ACCENT_START_S = .030
    private const val ACCENT_END_S = .100

    data class BarPhase(
        /** Beat k is a bar start when (k - downbeatOffset) mod BEATS_PER_BAR == 0. */
        val downbeatOffset: Int,
        val confidence: Float,
        val bars: Int,
        /** True when positions p and p+2 tie; either choice pairs strong beats, so confidence is discounted. */
        val twoBeatAmbiguity: Boolean,
    )

    /** [beatTimesSeconds] index into [samples] (same decoded window), strictly increasing. */
    fun analyze(samples: FloatArray, sampleRate: Int, beatTimesSeconds: List<Float>): BarPhase? {
        if (sampleRate <= 0 || beatTimesSeconds.size < BEATS_PER_BAR * MIN_BARS) return null
        if (beatTimesSeconds.any { !it.isFinite() || it < 0f }) return null
        if (beatTimesSeconds.zipWithNext().any { (a, b) -> b <= a }) return null
        // One-pole low-pass isolates the kick band; cheap and deterministic on the analysis thread.
        val low = FloatArray(samples.size)
        val a = (1.0 - exp(-2.0 * Math.PI * LOWPASS_HZ / sampleRate)).toFloat()
        var y = 0f
        for (i in samples.indices) { y += a * (samples[i] - y); low[i] = y }
        val accents = DoubleArray(beatTimesSeconds.size)
        for (k in beatTimesSeconds.indices) {
            val t = beatTimesSeconds[k].toDouble()
            val base = rms(low, sampleRate, t - BASE_START_S, t - BASE_END_S)
            val peak = rms(low, sampleRate, t - ACCENT_START_S, t + ACCENT_END_S)
            accents[k] = (peak - base).coerceAtLeast(0.0)
        }
        val mean = accents.average()
        if (mean <= 1e-9) return null
        val score = DoubleArray(BEATS_PER_BAR)
        val count = IntArray(BEATS_PER_BAR)
        for (k in accents.indices) { score[k % BEATS_PER_BAR] += accents[k]; count[k % BEATS_PER_BAR]++ }
        for (p in 0 until BEATS_PER_BAR) score[p] = if (count[p] > 0) score[p] / count[p] / mean else 0.0
        var best = 0
        for (p in 1 until BEATS_PER_BAR) if (score[p] > score[best]) best = p
        // The bar-start position must be above-average accented at all, then clear the odd positions.
        if (score[best] <= 1.0) return null
        val strong = (score[best] + score[(best + 2) % BEATS_PER_BAR]) / 2
        val weak = (score[(best + 1) % BEATS_PER_BAR] + score[(best + 3) % BEATS_PER_BAR]) / 2
        if (strong <= 1e-9) return null
        val margin = (strong - weak) / strong
        val ambiguity = abs(score[best] - score[(best + 2) % BEATS_PER_BAR]) / max(score[best], 1e-9) < .10
        val conf = (margin * if (ambiguity) .7 else 1.0).toFloat().coerceIn(0f, 1f)
        if (conf < MIN_CONFIDENCE) return null
        return BarPhase(best, conf, beatTimesSeconds.size / BEATS_PER_BAR, ambiguity)
    }

    private fun rms(samples: FloatArray, sampleRate: Int, t0: Double, t1: Double): Double {
        val lo = max(0, (t0 * sampleRate).toInt())
        val hi = min(samples.size, (t1 * sampleRate).toInt())
        if (hi <= lo) return 0.0
        var sum = 0.0
        for (i in lo until hi) {
            val v = samples[i]
            if (!v.isFinite()) return 0.0
            sum += v.toDouble() * v
        }
        return sqrt(sum / (hi - lo))
    }
}
