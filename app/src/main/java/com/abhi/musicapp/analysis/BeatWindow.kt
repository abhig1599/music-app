package com.abhi.musicapp.analysis

import kotlin.math.roundToLong

/** Explicit original-media coordinates. Never a seek instruction. */
object BeatWindow {
    fun absoluteGrid(grid: BeatGrid, startMs: Long, windowLengthMs: Long): PhaseTransitionPlanner.Grid? {
        if (startMs < 0 || windowLengthMs <= 0 || startMs > Long.MAX_VALUE - windowLengthMs) return null
        if (!grid.confidence.isFinite() || grid.confidence !in 0f..1f) return null
        val bpm = grid.bpm ?: return null
        if (!bpm.isFinite() || bpm !in 60f..180f) return null
        if (grid.timesSeconds.size < 4) return null
        val times = grid.timesSeconds.map { seconds ->
            if (!seconds.isFinite() || seconds < 0f) return null
            val relative = (seconds.toDouble() * 1000).roundToLong()
            if (relative >= windowLengthMs) return null
            startMs + relative
        }
        if (times.zipWithNext().any { (a,b) -> b <= a }) return null
        return PhaseTransitionPlanner.Grid(times, bpm, grid.confidence)
    }
}
