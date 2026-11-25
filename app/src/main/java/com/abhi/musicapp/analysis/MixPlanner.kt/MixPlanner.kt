package com.abhi.musicapp.analysis

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Beat-aligned transition plan from two beat grids, in original-media coordinates. Pure maths, unit-testable.
 *
 * Scope (user authorization boundary): the only content ever skipped is the detected silent tail, via the
 * existing content-end detection. The outgoing overlap ends exactly at that boundary and the incoming song
 * starts at 0, so every audible part of both songs plays. What this planner adds over the plain crossfade:
 *  - the overlap lasts a whole number of bars (8, 4, 2 or 1) inside the user's fade limit and starts ON an
 *    outgoing beat, instead of starting at an arbitrary offset (the exact fade length flexes by up to 1.5
 *    beats so it can both start on a beat and end exactly at the content end; when both cannot hold inside
 *    the user's cap the pair falls back to today's crossfade);
 *  - both songs run at one tempo during the overlap (incoming nudged, existing 6% cap, half/double time ok);
 *  - the two beat trains are required to land on top of each other across the whole overlap
 *    (max residual within [MAX_PHASE_ERROR_MS]); if they cannot, the pair falls back to today's behaviour;
 *  - the overlap is level-matched (RMS trim, clamped to +/-[MAX_GAIN_DB] dB).
 * Reliability of beat grids on real songs is unverified until the phone listening test; every weak-evidence
 * path returns null, which keeps the current crossfade.
 */
object MixPlanner {
    const val MAX_PHASE_ERROR_MS = 45.0
    const val MAX_GAIN_DB = 6f

    data class Plan(
        /** Outgoing media position where the fade begins (an outgoing beat). */
        val outStartMs: Long,
        /** Outgoing media position where the old deck is released: the detected content end or file end. */
        val outEndMs: Long,
        /** Fade length in outgoing media time: [outEndMs] - [outStartMs], a whole number of bars within the beat-selection window (the content end is not itself a beat). */
        val overlapMs: Long,
        /** Speed factor for the incoming deck during the overlap (both decks at the outgoing tempo). */
        val incomingSpeed: Float,
        /** Linear level trim applied to the incoming deck's fade-in (level-matched overlap). */
        val inGain: Float,
        /** Largest beat-train misalignment inside the overlap, in ms. */
        val maxResidualMs: Double,
        val reason: String,
    )

    fun plan(
        outBeatsMs: List<Long>?,
        outBpm: Float?,
        outConfidence: Float,
        outDurationMs: Long,
        outContentEndMs: Long?,
        inBeatsMs: List<Long>?,
        inBpm: Float?,
        inConfidence: Float,
        inDurationMs: Long?,
        userFadeMs: Long,
        outRms: Float?,
        inRms: Float?,
    ): Plan? {
        if (outDurationMs <= 0L || userFadeMs < 500L) return null
        if (!valid(outBeatsMs, outBpm, outConfidence) || !valid(inBeatsMs, inBpm, inConfidence)) return null
        val ob = outBeatsMs!!
        val ib = inBeatsMs!!
        val oBpm = outBpm!!
        val iBpm = inBpm!!

        // Tempo match, allowing half/double time, with the existing 6% nudge cap.
        val ratio = AutoMixPlanner.matchRatio(oBpm, iBpm) ?: return null
        if (abs(ratio - 1f) > AutoMixPlanner.MAX_NUDGE) return null

        val beatMs = averageBeat(ob) ?: return null
        if (beatMs !in 250.0..1200.0) return null
        val end = min(outContentEndMs?.takeIf { it in 1..outDurationMs } ?: outDurationMs, outDurationMs)

        // Whole bars (4 beats), largest of 8/4/2/1 bars fitting the user's limit and both duration caps.
        val limit = userFadeMs.coerceIn(500L, 12_000L)
        val outCap = outDurationMs / 3
        val inCap = inDurationMs?.takeIf { it > 0L }?.div(3) ?: Long.MAX_VALUE
        val cap = min(limit, min(outCap, inCap))
        val bars = listOf(8, 4, 2, 1).firstOrNull { (it * 4 * beatMs).roundToLong() + (beatMs / 2).roundToLong() <= cap && it * 4 * beatMs >= 500.0 }
            ?: return null
        val target = bars * 4 * beatMs

        // The fade starts on an outgoing beat near (end - target). Try the three closest beats and keep the
        // one whose beat train best overlays the incoming train across the whole overlap.
        val anchor = end - target
        val candidates = ob.indices
            .filter { abs(ob[it] - anchor) <= beatMs * 1.5 && ob[it] in 1..end }
            .sortedBy { abs(ob[it] - anchor) }
            .take(3)
        if (candidates.isEmpty()) return null
        var best: Plan? = null
        for (c in candidates) {
            val start = ob[c]
            val overlap = end - start
            if (overlap !in 500L..cap) continue
            val outInOverlap = ob.filter { it >= start && it < end }
            if (outInOverlap.size < 4) continue
            val inCoverage = ib.filter { (it / ratio) <= overlap }
            if (inCoverage.size < 4) continue
            val residual = maxResidual(outInOverlap, start, inCoverage, ratio, beatMs) ?: continue
            if (residual > MAX_PHASE_ERROR_MS) continue
            val plan = Plan(start, end, overlap, ratio, gainTrim(outRms, inRms), residual,
                "beat-aligned ${bars * 4}-beat overlap ending at content end, nudge %.4f".format(ratio))
            if (best == null || plan.maxResidualMs < best.maxResidualMs) best = plan
        }
        return best
    }

    private fun valid(beatsMs: List<Long>?, bpm: Float?, confidence: Float): Boolean {
        if (beatsMs == null || bpm == null) return false
        if (!bpm.isFinite() || bpm !in 60f..180f) return false
        if (!confidence.isFinite() || confidence !in .75f..1f) return false
        if (beatsMs.size < 8) return false
        if (beatsMs.first() < 0) return false
        if (beatsMs.zipWithNext().any { (a, b) -> b <= a }) return false
        return true
    }

    private fun averageBeat(beatsMs: List<Long>): Double? {
        val intervals = beatsMs.zipWithNext { a, b -> (b - a).toDouble() }
        if (intervals.isEmpty()) return null
        return intervals.average()
    }

    /**
     * Largest misalignment between the two beat trains inside the overlap. During the overlap the incoming
     * deck runs at [ratio], so its beat at media time b is heard b/[ratio] into the overlap. Each outgoing
     * beat is compared with the nearest incoming beat (modulo one beat, so only sub-beat phase counts).
     */
    internal fun maxResidual(outInOverlap: List<Long>, outStartMs: Long, inBeats: List<Long>, ratio: Float, beatMs: Double): Double? {
        if (outInOverlap.isEmpty() || inBeats.isEmpty() || ratio <= 0.0 || !ratio.isFinite()) return null
        val period = beatMs
        var worst = -1.0
        for (a in outInOverlap) {
            val off = (a - outStartMs).toDouble()
            var bestForBeat = Double.MAX_VALUE
            for (b in inBeats) {
                val d = abs(off - b / ratio) % period
                val wrapped = min(d, period - d)
                if (wrapped < bestForBeat) bestForBeat = wrapped
            }
            if (bestForBeat == Double.MAX_VALUE) return null
            if (bestForBeat > worst) worst = bestForBeat
        }
        return worst
    }

    /** Linear trim so the incoming deck's head loudness matches the outgoing tail; clamped to +/-[MAX_GAIN_DB]. */
    internal fun gainTrim(outRms: Float?, inRms: Float?): Float {
        if (outRms == null || inRms == null || !outRms.isFinite() || !inRms.isFinite() || outRms <= 0f || inRms <= 0f) return 1f
        val db = (20f * log10(outRms / inRms)).coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB)
        return 10f.pow(db / 20f)
    }
}
