package com.abhi.musicapp.analysis

import android.content.Context
import android.net.Uri
import kotlin.math.sqrt

/** Off-thread pair analysis, consumed only under the existing AutoMix opt-in. */
object TransitionAnalysis {

    private data class CacheKey(val outgoing: String, val incoming: String, val durationMs: Long, val fadeMs: Long)
    private val cache = object : LinkedHashMap<CacheKey, TransitionAnalysisResult>(4, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, TransitionAnalysisResult>?) = size > 4
    }
    private fun identity(c: Context, uri: Uri): String? {
        val k = TempoCache.keyFor(c, uri, uri.toString())
        return k.value.takeIf { k.persistable }
    }

    fun analyze(c: Context, outgoing: Uri, incoming: Uri, durationMs: Long, fadeMs: Long, cancelled: () -> Boolean = { false }): TransitionAnalysisResult? {
        if (cancelled() || durationMs <= 0) return null
        val outId = identity(c, outgoing)
        val inId = identity(c, incoming)
        val key = if (outId != null && inId != null) CacheKey(outId, inId, durationMs, fadeMs) else null
        if (key != null) synchronized(cache) { cache[key] }?.let { return it }
        val tailStart = (durationMs - TempoCache.MAX_SECONDS * 1000L).coerceAtLeast(0)
        val tail = analyzeWindow(c, outgoing, tailStart, cancelled)
        if (cancelled()) return null
        val head = analyzeWindow(c, incoming, 0, cancelled)
        if (cancelled()) return null
        val unknown = PhaseTransitionPlanner.Grid(emptyList(), null, 0f)
        val plan = PhaseTransitionPlanner.plan(tail?.grid ?: unknown, head?.grid ?: unknown, durationMs, fadeMs)
        // Beat-aligned whole-bar plan. Content boundary: the same detected silent-tail end (or file end) the
        // crossfader already uses; the incoming song starts at 0. No audible content is skipped.
        val contentEnd = TailCache.fileIdentity(c, outgoing, durationMs)?.let { TailCache.contentEndMs(it) }
        val mix = MixPlanner.plan(
            tail?.grid?.beatsMs, tail?.grid?.bpm, tail?.grid?.confidence ?: 0f,
            durationMs, contentEnd,
            head?.grid?.beatsMs, head?.grid?.bpm, head?.grid?.confidence ?: 0f,
            null, fadeMs, tail?.rms, head?.rms,
        )
        val result = TransitionAnalysisResult(tail?.grid, head?.grid, plan, mix)
        if (cancelled()) return null
        // File replacement during decoding must never publish an old result under new metadata.
        if (key != null) {
            if (identity(c, outgoing) != outId || identity(c, incoming) != inId) return null
            synchronized(cache) { cache[key] = result }
        }
        return result
    }

    private class WindowEvidence(val grid: PhaseTransitionPlanner.Grid?, val rms: Float)

    private fun analyzeWindow(c: Context, uri: Uri, startMs: Long, cancelled: () -> Boolean): WindowEvidence? {
        val w = TempoCache.decodeWindow(c, uri, startMs, cancelled = cancelled) ?: return null
        if (cancelled()) return null
        val grid = BeatGridAnalyzer.analyze(w.samples, w.sampleRate)
        val lengthMs = w.samples.size * 1000L / w.sampleRate
        return WindowEvidence(BeatWindow.absoluteGrid(grid, w.startMs, lengthMs), rms(w.samples))
    }

    /** Mean level of a decoded window; 0 when unusable, which makes the level trim abstain (gain 1). */
    internal fun rms(samples: FloatArray): Float {
        if (samples.isEmpty()) return 0f
        var sum = 0.0
        for (v in samples) { if (!v.isFinite()) return 0f; sum += v * v }
        return sqrt(sum / samples.size).toFloat()
    }
}
