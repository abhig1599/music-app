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
    const val MIN_BAR_CONFIDENCE = .25f

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
        outBarPos0: Int? = null,
        inBarPos0: Int? = null,
        outBarConf: Float = 0f,
        inBarConf: Float = 0f,
        /** Diagnostics only: invoked once with a short reason constant when planning falls back. Never affects the decision. */
        onDecision: ((String) -> Unit)? = null,
    ): Plan? {
        if (outDurationMs <= 0L || userFadeMs < 500L) return null.also { onDecision?.invoke("bad-duration-or-fade") }
        if (!valid(outBeatsMs, outBpm, outConfidence)) return null.also { onDecision?.invoke("out-grid-weak") }
        if (!valid(inBeatsMs, inBpm, inConfidence)) return null.also { onDecision?.invoke("in-grid-weak") }
        val ob = outBeatsMs!!
        val ib = inBeatsMs!!
        val oBpm = outBpm!!
        val iBpm = inBpm!!

        // Tempo match, allowing half/double time, with the existing 6% nudge cap.
        val ratio = AutoMixPlanner.matchRatio(oBpm, iBpm) ?: return null.also { onDecision?.invoke("tempo-unmatched") }
        if (abs(ratio - 1f) > AutoMixPlanner.MAX_NUDGE) return null.also { onDecision?.invoke("nudge-cap-exceeded") }

        val beatMs = averageBeat(ob) ?: return null.also { onDecision?.invoke("no-beat-period") }
        if (beatMs !in 250.0..1200.0) return null.also { onDecision?.invoke("beat-period-out-of-range") }
        val end = min(outContentEndMs?.takeIf { it in 1..outDurationMs } ?: outDurationMs, outDurationMs)

        // Whole bars (4 beats), largest of 8/4/2/1 bars fitting the user's limit and both duration caps.
        val limit = userFadeMs.coerceIn(500L, 12_000L)
        val outCap = outDurationMs / 3
        val inCap = inDurationMs?.takeIf { it > 0L }?.div(3) ?: Long.MAX_VALUE
        val cap = min(limit, min(outCap, inCap))
        val bars = listOf(8, 4, 2, 1).firstOrNull { (it * 4 * beatMs).roundToLong() + (beatMs / 2).roundToLong() <= cap && it * 4 * beatMs >= 500.0 }
            ?: return null.also { onDecision?.invoke("no-whole-bar-overlap-fits-cap") }
        val target = bars * 4 * beatMs

        // The fade starts on an outgoing beat near (end - target). Try the three closest beats and keep the
        // one whose beat train best overlays the incoming train across the whole overlap.
        val anchor = end - target
        val candidates = ob.indices
            .filter { abs(ob[it] - anchor) <= beatMs * 1.5 && ob[it] in 1..end }
            .sortedBy { abs(ob[it] - anchor) }
            .take(3)
        if (candidates.isEmpty()) return null.also { onDecision?.invoke("no-anchor-beat-near-content-end") }
        // Optional bar evidence (tag-only, from BarPhaseAnalyzer accents on the real audio): when BOTH
        // grids carry confident bar positions, prefer the candidate whose index-locked beat pairs share a
        // bar position (downbeat lands on downbeat). Never a reason to reject a candidate that already
        // passed every beat gate, and never changes anything when evidence is missing.
        val barEvidence = outBarPos0 != null && inBarPos0 != null &&
            outBarConf >= MIN_BAR_CONFIDENCE && inBarConf >= MIN_BAR_CONFIDENCE
        val (outStride, inStride) = strides(ratio, oBpm, iBpm)
        var best: Plan? = null
        var bestBar = -1
        var skippedWindow = 0
        var skippedFewBeats = 0
        var skippedNoResidual = 0
        var skippedPhase = 0
        for (c in candidates) {
            val start = ob[c]
            val overlap = end - start
            if (overlap !in 500L..cap) { skippedWindow++; continue }
            val outInOverlap = ob.filter { it >= start && it < end }
            if (outInOverlap.size < 4) { skippedFewBeats++; continue }
            val residual = maxResidual(outInOverlap, start, ib, ratio, oBpm, iBpm)
            if (residual == null) { skippedNoResidual++; continue }
            if (residual > MAX_PHASE_ERROR_MS) { skippedPhase++; continue }
            val barScore = if (barEvidence)
                alignedPairs(outInOverlap.size, c, outStride, inStride, ib.size, outBarPos0!!, inBarPos0!!) else -1
            val plan = Plan(start, end, overlap, ratio, gainTrim(outRms, inRms), residual,
                "beat-aligned ${bars * 4}-beat overlap ending at content end, nudge %.4f".format(ratio) +
                    if (barScore >= 0) ", bar-matched $barScore pairs" else "")
            if (best == null || barScore > bestBar ||
                (barScore == bestBar && plan.maxResidualMs < best!!.maxResidualMs)) {
                best = plan
                bestBar = barScore
            }
        }
        if (best == null) {
            val why = listOf(skippedWindow to "candidate-window", skippedFewBeats to "few-overlap-beats",
                skippedNoResidual to "residual-unknown", skippedPhase to "phase-too-large")
                .maxByOrNull { it.first }?.second ?: "no-candidate"
            onDecision?.invoke(why)
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
     * Compare actual beat timestamps from the cue at zero. With half/double-time
     * detection, every other beat on the faster grid is a subdivision.
     */
    private fun maxResidual(
        outgoing: List<Long>, startMs: Long, incoming: List<Long>,
        ratio: Float, outgoingBpm: Float, incomingBpm: Float,
    ): Double? {
        val (outStride, inStride) = strides(ratio, outgoingBpm, incomingBpm)
        var worst = 0.0
        for (i in outgoing.indices step outStride) {
            val beat = incoming.getOrNull(i / outStride * inStride) ?: return null
            val offset = (outgoing[i] - startMs).toDouble()
            val error = abs(offset - beat / ratio.toDouble())
            if (error > MAX_PHASE_ERROR_MS) return null
            worst = maxOf(worst, error)
        }
        return worst
    }


    /** Half/double-time stride handling: every other beat on the faster grid is a subdivision. */
    private fun strides(ratio: Float, outgoingBpm: Float, incomingBpm: Float): Pair<Int, Int> {
        val matchedIncomingBpm = incomingBpm * ratio
        val outStride = if (outgoingBpm > matchedIncomingBpm * 1.5f) 2 else 1
        val inStride = if (matchedIncomingBpm > outgoingBpm * 1.5f) 2 else 1
        return outStride to inStride
    }

    /** How many index-locked beat pairs across the overlap share the same position within the bar. */
    private fun alignedPairs(outOverlapSize: Int, outStartIndex: Int, outStride: Int, inStride: Int, inBeatCount: Int, outPos0: Int, inPos0: Int): Int {
        var n = 0
        var i = 0
        while (i < outOverlapSize) {
            val j = i / outStride * inStride
            if (j >= inBeatCount) break
            if (Math.floorMod(outPos0 + outStartIndex + i, BarPhaseAnalyzer.BEATS_PER_BAR) ==
                Math.floorMod(inPos0 + j, BarPhaseAnalyzer.BEATS_PER_BAR)) n++
            i += outStride
        }
        return n
    }

    /** Linear trim so the incoming deck's head loudness matches the outgoing tail; clamped to +/-[MAX_GAIN_DB]. */
    internal fun gainTrim(outRms: Float?, inRms: Float?): Float {
        if (outRms == null || inRms == null || !outRms.isFinite() || !inRms.isFinite() || outRms <= 0f || inRms <= 0f) return 1f
        val db = (20f * log10(outRms / inRms)).coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB)
        return 10f.pow(db / 20f)
    }
}
