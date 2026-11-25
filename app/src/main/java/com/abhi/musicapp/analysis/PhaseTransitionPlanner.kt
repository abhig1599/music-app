package com.abhi.musicapp.analysis

import kotlin.math.abs
import kotlin.math.roundToLong

/** Analysis-only plan. Both songs retain all content: incoming starts at0, outgoing reaches its natural end. */
object PhaseTransitionPlanner {
    data class Grid(
        val beatsMs: List<Long>,
        val bpm: Float?,
        val confidence: Float,
        /** Bar position (0 = downbeat) of beatsMs[0] from measured accent evidence; null when unknown. */
        val firstBeatBarPosition: Int? = null,
        val barConfidence: Float = 0f,
    )
    data class Plan(
        val aligned: Boolean,
        val startOutgoingMs: Long,
        val fadeMs: Long,
        val incomingSpeedRatio: Float,
        val predictedMaxPhaseErrorMs: Float?,
        val reason: String,
    )

    /** Positions use original media timelines. UI speed is applied later; this plan is at1x outgoing speed. */
    fun plan(out: Grid, incoming: Grid, outDurationMs: Long, userFadeMs: Long): Plan {
        val duration = outDurationMs.coerceAtLeast(0L)
        val limit = userFadeMs.coerceIn(0L, 12000L).coerceAtMost(duration / 3)
        fun fallback(reason: String) = Plan(false, duration - limit, limit, 1f, null, reason)
        if (outDurationMs <= 0 || limit < 500L) return fallback("short or unknown duration")
        if (!valid(out) || !valid(incoming)) return fallback("unreliable grid")
        val ratio = out.bpm!! / incoming.bpm!!
        // Do not reinterpret half/double tempo as phase certainty.
        if (!ratio.isFinite() || abs(ratio - 1f) > .06f) return fallback("incompatible tempo")
        var best: Plan? = null
        for (a in out.beatsMs) {
            if (a < outDurationMs - limit || a >= outDurationMs) continue
            for (b in incoming.beatsMs.take(12)) {
                val start = (a.toDouble() - b.toDouble() / ratio).roundToLong()
                val fade = outDurationMs - start
                if (fade !in 500L..limit) continue
                val transformed = incoming.beatsMs.map { (start.toDouble() + it.toDouble() / ratio).roundToLong().toDouble() }
                val matched = out.beatsMs.filter { it >= start && it < outDurationMs }
                // Need evidence across the entire overlap, not one coincident transient.
                if (matched.size < 4) continue
                val errors = matched.map { t -> transformed.minOf { abs(it - t) } }
                var maxError = errors.maxOrNull() ?: continue
                if (maxError > 30f) continue
                val reverse = transformed.filter { it >= start && it < outDurationMs }
                if (reverse.size < 4) continue
                maxError = maxOf(maxError, reverse.maxOf { t -> matched.minOf { abs(it - t) } })
                if (maxError > 30.0) continue
                val candidate = Plan(true, start, fade, ratio, maxError.toFloat(), "high-confidence beat positions, full content preserved")
                val old = best
                if (old == null || candidate.fadeMs > old.fadeMs || (candidate.fadeMs == old.fadeMs && maxError < old.predictedMaxPhaseErrorMs!!)) best = candidate
            }
        }
        return best ?: fallback("no safe phase alignment within duration limit")
    }

    private fun valid(g: Grid): Boolean {
        val bpm = g.bpm ?: return false
        return bpm.isFinite() && bpm in 60f..180f && g.confidence.isFinite() && g.confidence in .75f..1f &&
            g.beatsMs.size >= 4 && g.beatsMs.first() >= 0 && g.beatsMs.zipWithNext().all { (a,b) -> b > a }
    }
}
