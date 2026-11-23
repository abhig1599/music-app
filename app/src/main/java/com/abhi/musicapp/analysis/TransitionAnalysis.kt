package com.abhi.musicapp.analysis

import android.content.Context
import android.net.Uri

/** Experimental off-thread pair analysis. Not connected to the service or Crossfader. */
object TransitionAnalysis {
    data class Result(val outgoing: PhaseTransitionPlanner.Grid?, val incoming: PhaseTransitionPlanner.Grid?, val plan: PhaseTransitionPlanner.Plan)

    private data class CacheKey(val outgoing: String, val incoming: String, val durationMs: Long, val fadeMs: Long)
    private val cache = object : LinkedHashMap<CacheKey, Result>(4, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, Result>?) = size > 4
    }
    private fun identity(c: Context, uri: Uri): String? {
        val k = TempoCache.keyFor(c, uri, uri.toString())
        return k.value.takeIf { k.persistable }
    }

    fun analyze(c: Context, outgoing: Uri, incoming: Uri, durationMs: Long, fadeMs: Long, cancelled: () -> Boolean = { false }): Result? {
        if (cancelled() || durationMs <= 0) return null
        val outId = identity(c, outgoing)
        val inId = identity(c, incoming)
        val key = if (outId != null && inId != null) CacheKey(outId, inId, durationMs, fadeMs) else null
        if (key != null) synchronized(cache) { cache[key] }?.let { return it }
        val tailStart = (durationMs - TempoCache.MAX_SECONDS * 1000L).coerceAtLeast(0)
        val tailGrid = analyzeWindow(c, outgoing, tailStart, cancelled)
        if (cancelled()) return null
        val headGrid = analyzeWindow(c, incoming, 0, cancelled)
        if (cancelled()) return null
        val unknown = PhaseTransitionPlanner.Grid(emptyList(), null, 0f)
        val plan = PhaseTransitionPlanner.plan(tailGrid ?: unknown, headGrid ?: unknown, durationMs, fadeMs)
        val result = Result(tailGrid, headGrid, plan)
        if (cancelled()) return null
        // File replacement during decoding must never publish an old result under new metadata.
        if (key != null) {
            if (identity(c, outgoing) != outId || identity(c, incoming) != inId) return null
            synchronized(cache) { cache[key] = result }
        }
        return result
    }

    private fun analyzeWindow(c: Context, uri: Uri, startMs: Long, cancelled: () -> Boolean): PhaseTransitionPlanner.Grid? {
        val w = TempoCache.decodeWindow(c, uri, startMs, cancelled = cancelled) ?: return null
        if (cancelled()) return null
        val grid = BeatGridAnalyzer.analyze(w.samples, w.sampleRate)
        val lengthMs = w.samples.size * 1000L / w.sampleRate
        return BeatWindow.absoluteGrid(grid, w.startMs, lengthMs)
    }
}
