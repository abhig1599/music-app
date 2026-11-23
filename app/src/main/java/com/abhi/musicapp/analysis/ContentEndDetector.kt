package com.abhi.musicapp.analysis

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Finds where a song's audible content ends inside a decoded TAIL window, so a transition can skip only
 * encoder-level digital silence - never musical content.
 *
 * Assumptions (conservative by design):
 *  - A trailing region counts as silence only when EVERY 50 ms frame in it, up to the file end, is quieter
 *    than [FLOOR_DB] dBFS RMS. That floor (-66 dBFS, ~0.05% of full scale) is far below audibility in normal
 *    listening; encoded digital silence measures well under it, while quiet music, reverb tails and hiss do not.
 *  - The region must be contiguous and reach the very end of the file, at least [MIN_TRIM_MS] long (shorter is
 *    not worth touching), and at most [MAX_TRIM_MS] (longer means the file is mostly empty - not our case).
 *  - Anything ambiguous - no frames, an all-quiet window, a window too short to judge - means ABSTAIN
 *    (contentEndMs = null) and the transition keeps using the real file end.
 * The detector never claims to preserve every quiet ending; it only acts when the evidence says "digital silence".
 */
object ContentEndDetector {
    const val FLOOR_DB = -66.0
    const val FRAME_MS = 50
    const val MIN_TRIM_MS = 200L
    const val MAX_TRIM_MS = 10_000L

    data class Result(val contentEndMs: Long?, val trimMs: Long, val floorDb: Double, val frames: Int)

    /**
     * [samples] mono PCM of a window that ends at the file's end; [windowStartMs] is where the window starts
     * on the song timeline; [durationMs] the file's real duration. Returns the content end in song ms, or null to abstain.
     */
    fun detect(samples: FloatArray, sampleRate: Int, windowStartMs: Long, durationMs: Long): Result {
        val frameLen = (sampleRate * FRAME_MS) / 1000
        val frames = if (frameLen > 0) samples.size / frameLen else 0
        if (frames < 2 || durationMs <= 0) return Result(null, 0, FLOOR_DB, frames)
        val floorLin = Math.pow(10.0, FLOOR_DB / 20.0).toFloat()

        // Walk back from the last frame while frames stay under the floor.
        var lastSilent = frames // exclusive index of the first trailing-silent frame
        while (lastSilent > 0) {
            val start = (lastSilent - 1) * frameLen
            var sum = 0.0
            for (i in start until start + frameLen) { val v = samples[i]; sum += v * v }
            val rms = sqrt(sum / frameLen).toFloat()
            if (rms > floorLin) break
            lastSilent--
        }

        if (lastSilent == frames) return Result(null, 0, FLOOR_DB, frames) // nothing trailing found -> no trim
        if (lastSilent == 0) return Result(null, 0, FLOOR_DB, frames)     // whole window quiet -> abstain, cannot judge

        // The content end is the end of the last above-floor frame, on the song timeline.
        val windowLenMs = (frames * FRAME_MS).toLong()
        val windowEndMs = windowStartMs + windowLenMs
        val trimMs = (frames - lastSilent) * FRAME_MS.toLong() - (windowEndMs - durationMs).coerceAtLeast(0L)
        if (trimMs < MIN_TRIM_MS || trimMs > MAX_TRIM_MS) return Result(null, 0, FLOOR_DB, frames)
        return Result(durationMs - trimMs, trimMs, FLOOR_DB, frames)
    }
}
