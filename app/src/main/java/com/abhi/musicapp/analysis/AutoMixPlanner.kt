package com.abhi.musicapp.analysis

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * EXPERIMENTAL, not used by playback or the UI. Turns two tempo estimates into a transition plan, as pure maths so it can be unit tested.
 * It does not decide what is audible: nothing calls it until a phone listening test says the result sounds right.
 *
 * Plan: only when both tempos are known and close (within [MAX_RATIO_GAP], after allowing a half or double tempo match), fade over a whole
 * number of beats (a multiple of 4 beats, the usual bar) that fits the user's 1..12 s limit, and nudge the incoming speed by at most
 * [MAX_NUDGE] so both songs run at the same tempo during the overlap. Otherwise fall back to a plain crossfade of the user's length.
 */
object AutoMixPlanner {
    const val MAX_RATIO_GAP = 0.08f
    const val MAX_NUDGE = 0.06f

    data class Plan(val beatMatched: Boolean, val fadeMs: Long, val incomingSpeed: Float)

    fun plan(out: TempoResult, into: TempoResult, userFadeSec: Int): Plan {
        val plain = Plan(false, userFadeSec.coerceIn(1, 12) * 1000L, 1f)
        val a = out.bpm ?: return plain
        val b = into.bpm ?: return plain
        val ratio = matchRatio(a, b) ?: return plain // incoming speed needed to match the outgoing tempo
        val nudge = ratio - 1f
        if (abs(nudge) > MAX_NUDGE) return plain
        val beatMs = 60000f / a
        val limit = userFadeSec.coerceIn(1, 12) * 1000f
        val bars = (limit / (4 * beatMs)).toInt() // whole bars that fit in the user's limit
        val fade = if (bars >= 1) bars * 4 * beatMs else max(beatMs, (limit / beatMs).toInt() * beatMs) // under one bar: whole beats
        return Plan(true, min(fade.roundToInt().toLong(), 12000L), ratio)
    }

    /** Speed factor for the incoming song so its tempo equals the outgoing one, trying equal, half and double tempo. Null if none is close. */
    internal fun matchRatio(outBpm: Float, inBpm: Float): Float? {
        var best: Float? = null
        for (k in floatArrayOf(1f, 0.5f, 2f)) {
            val r = outBpm * k / inBpm
            if (abs(r - 1f) <= MAX_RATIO_GAP && (best == null || abs(r - 1f) < abs(best - 1f))) best = r
        }
        return best
    }
}
