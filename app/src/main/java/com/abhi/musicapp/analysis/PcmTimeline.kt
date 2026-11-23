package com.abhi.musicapp.analysis

import kotlin.math.abs

/** A decoded window may be flattened only while presentation timestamps remain continuous. */
object PcmTimeline {
    fun isContinuous(firstUs: Long, framesWritten: Int, nextUs: Long, sampleRate: Int): Boolean {
        if (firstUs < 0 || framesWritten < 0 || nextUs < 0 || sampleRate !in 8000..192000) return false
        val elapsed = framesWritten * 1_000_000L / sampleRate
        if (firstUs > Long.MAX_VALUE - elapsed) return false
        val expected = firstUs + elapsed
        // Codec timestamps can be rounded. A gap or repeated block is not contiguous PCM.
        return abs(nextUs - expected) <= 2000L
    }
}
