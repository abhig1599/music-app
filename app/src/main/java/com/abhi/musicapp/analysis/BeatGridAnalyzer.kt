package com.abhi.musicapp.analysis

import kotlin.math.*

/** Experimental analysis only, never called by playback. Beat positions are not downbeats or phrases. */
data class BeatGrid(val timesSeconds: List<Float>, val confidence: Float, val bpm: Float?)

object BeatGridAnalyzer {
    private const val HOP = 256
    private const val WIN = 1024
    fun analyze(samples: FloatArray, sampleRate: Int): BeatGrid {
        if (sampleRate <= 0 || samples.any { !it.isFinite() }) return BeatGrid(emptyList(), 0f, null)
        val tempo = TempoAnalyzer.analyze(samples, sampleRate)
        val bpm = tempo.bpm ?: return BeatGrid(emptyList(), 0f, null)
        if (sampleRate <= 0 || samples.size < sampleRate * 8) return BeatGrid(emptyList(), 0f, null)
        // A single global grid must not bridge incompatible local tempi with invented beats.
        // Local BPM checks do not prove phase accuracy, but reject obvious variable-tempo passages.
        val span = sampleRate * 10
        if (samples.size >= span * 2) {
            var start = 0
            while (start + span <= samples.size) {
                val local = TempoAnalyzer.analyze(samples.copyOfRange(start, start + span), sampleRate)
                if (local.bpm != null && local.confidence >= .5f) {
                    val gap = floatArrayOf(.5f, 1f, 2f).minOf { abs(local.bpm * it / bpm - 1f) }
                    if (gap > .06f) return BeatGrid(emptyList(), 0f, bpm)
                }
                start += span / 2
            }
        }
        val frames = (samples.size - WIN) / HOP + 1
        val envelope = FloatArray(frames)
        var previous = 0f
        for (i in envelope.indices) {
            var energy = 0.0
            for (j in 0 until WIN) { val v = samples[i * HOP + j]; if (!v.isFinite()) return BeatGrid(emptyList(), 0f, null); energy += v * v }
            val rms = sqrt(energy / WIN).toFloat()
            envelope[i] = max(0f, rms - previous); previous = rms
        }
        val peak = envelope.maxOrNull() ?: 0f
        if (peak < 1e-6f) return BeatGrid(emptyList(), 0f, null)
        val fps = sampleRate.toFloat() / HOP
        val period = 60f * fps / bpm
        val score = FloatArray(frames)
        val back = IntArray(frames) { -1 }
        for (i in 0 until frames) {
            var best = 0f; var predecessor = -1
            val from = max(0, i - (period * 1.5f).roundToInt())
            val until = i - (period * .65f).roundToInt()
            if (until >= from) for (j in from..until) {
                val penalty = 8f * ln((i - j) / period).pow(2)
                val candidate = score[j] - penalty
                if (candidate > best) { best = candidate; predecessor = j }
            }
            score[i] = envelope[i] / peak + best
            back[i] = predecessor
        }
        // End on an onset in the final two periods, not an arbitrary score plateau.
        val start = max(0, frames - (period * 2).roundToInt())
        val end = (start until frames).filter { envelope[it] >= peak * .1f }.maxByOrNull { score[it] }
            ?: return BeatGrid(emptyList(), 0f, bpm)
        val path = ArrayList<Int>(); var i = end
        while (i >= 0) { path.add(i); i = back[i] }
        path.reverse()
        if (path.size < 8) return BeatGrid(emptyList(), 0f, bpm)
        // Energy-rise frames lead the physical onset because windows look ahead. Estimate actual onset inside each window.
        val times = path.map { frame ->
            val lo = frame * HOP
            val hi = min(samples.size, lo + WIN)
            var maximum = 0f
            for (j in lo until hi) maximum = max(maximum, abs(samples[j]))
            val onset = (lo until hi).firstOrNull { abs(samples[it]) >= maximum * .2f } ?: lo
            onset.toFloat() / sampleRate
        }
        val support = path.count { envelope[it] >= peak * .1f }.toFloat() / path.size
        val intervals = times.zipWithNext { a, b -> b - a }
        val target = 60f / bpm
        val error = intervals.map { abs(it / target - 1f) }.average().toFloat()
        val confidence = min(tempo.confidence, support * (1f - error.coerceIn(0f, 1f)))
        return if (confidence >= .65f) BeatGrid(times, confidence, bpm) else BeatGrid(emptyList(), confidence, bpm)
    }
}
