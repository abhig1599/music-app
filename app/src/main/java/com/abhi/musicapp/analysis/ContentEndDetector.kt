package com.abhi.musicapp.analysis

/**
 * Estimates where a song's audio content ends inside a decoded TAIL window, from near-zero SAMPLE evidence.
 *
 * What this is: a conservative heuristic that acts only when the trailing samples are essentially zero.
 * What this is NOT: proof of "digital silence", a perceptual loudness judgement, or a guarantee that no
 * quiet sound is ever trimmed. Any doubt abstains (contentEndMs = null) and the caller keeps the file end.
 *
 * Rules (all must hold, else abstain):
 *  - Evidence is per-frame PEAK (max |sample| over EVERY channel, before any mono mixdown, so out-of-phase
 *    content cannot cancel to zero), never an average that could hide sparse transients.
 *  - Every frame of the trailing region, through the end of the decoded window, must peak below [PEAK_FLOOR].
 *  - The decoded window must provably cover the file end: [windowCoverEndMs] within [COVER_TOLERANCE_MS] of
 *    [durationMs]. A clipped or truncated decode cannot prove the tail is quiet, so it abstains.
 *  - The trim must be between [MIN_TRIM_MS] and [MAX_TRIM_MS]. Outside that range the file is not the case
 *    this feature exists for.
 *  - Any NaN or infinite sample, a non-positive or inconsistent duration, or a cancellation request aborts
 *    to abstain.
 */
object ContentEndDetector {
    /** Peak threshold per frame: 1e-4 linear (-80 dBFS). Near-zero-sample evidence, not an audibility claim. */
    const val PEAK_FLOOR = 1e-4f
    const val FRAME_MS = 50
    const val MIN_TRIM_MS = 200L
    const val MAX_TRIM_MS = 10_000L
    /** Quiet audio always kept inside the boundary as a safety margin: the blend never ends at the exact first-quiet frame. */
    const val RETAINED_QUIET_MS = 250L
    /** Declared duration may exceed the EOS-established stream end by at most this much; more means a broken file. */
    const val MAX_DECLARED_OVERHANG_MS = 1_000L
    data class Result(val contentEndMs: Long?, val trimMs: Long, val frames: Int, val reason: String)

    private val ABSTAIN = { frames: Int, why: String -> Result(null, 0, frames, why) }

    /** Max |sample| per [frameLen]-sample frame over interleaved [channels]; NaN/Inf propagate so detection abstains. */
    fun framePeaks(interleaved: FloatArray, channels: Int, frameLen: Int): FloatArray {
        if (channels <= 0 || frameLen <= 0) return FloatArray(0)
        val frames = interleaved.size / (frameLen * channels)
        return FloatArray(frames) { f ->
            var peak = 0f
            val start = f * frameLen * channels
            for (i in start until start + frameLen * channels) {
                val v = interleaved[i]
                val a = if (v < 0f) -v else v
                if (a > peak || a.isNaN()) peak = a
            }
            peak
        }
    }

    /**
     * [framePeaks] covers a window starting at [windowStartMs] whose decoded audio ends at [windowCoverEndMs]
     * on the song timeline (NOT the requested end - the actual decoded coverage); [durationMs] is the file's
     * current duration. Returns the content end in song ms, or null with a reason to abstain.
     */
    /**
     * Coverage rule: [windowCoverEndMs] beyond the declared [durationMs] is clamped. ANY undercoverage abstains
     * unless [eosReached] - a signaled end-of-stream establishes the decoded end as the ACTUAL media end from
     * sample timestamps - and even then a mismatch over [MAX_DECLARED_OVERHANG_MS] means a broken file. There is
     * no tolerance window: unobserved samples never count as quiet. [RETAINED_QUIET_MS] of the quiet tail is
     * always kept inside the boundary as a safety margin.
     */
    fun detect(
        framePeaks: FloatArray,
        windowStartMs: Long,
        windowCoverEndMs: Long,
        durationMs: Long,
        eosReached: Boolean,
        cancelled: () -> Boolean = { false },
    ): Result {
        val frames = framePeaks.size
        if (frames < 2) return ABSTAIN(frames, "window too short to judge")
        if (durationMs <= 0 || windowStartMs < 0 || windowStartMs >= durationMs) return ABSTAIN(frames, "inconsistent duration")
        if (cancelled()) return ABSTAIN(frames, "cancelled")
        val mediaEndMs: Long
        if (windowCoverEndMs >= durationMs) {
            mediaEndMs = durationMs
        } else {
            if (!eosReached) return ABSTAIN(frames, "decode does not cover the file end")
            if (durationMs - windowCoverEndMs > MAX_DECLARED_OVERHANG_MS) return ABSTAIN(frames, "declared duration far exceeds decoded stream")
            mediaEndMs = windowCoverEndMs
        }

        for (p in framePeaks) if (p.isNaN() || p.isInfinite()) return ABSTAIN(frames, "non-finite sample")

        var firstSilent = frames // exclusive index where the trailing quiet region starts
        while (firstSilent > 0) {
            if (cancelled()) return ABSTAIN(frames, "cancelled")
            if (framePeaks[firstSilent - 1] >= PEAK_FLOOR) break
            firstSilent--
        }

        if (firstSilent == frames) return ABSTAIN(frames, "no quiet tail")
        if (firstSilent == 0) return ABSTAIN(frames, "whole window quiet")

        val overhangMs = (windowCoverEndMs - mediaEndMs).coerceAtLeast(0L)
        val trimMs = (frames - firstSilent) * FRAME_MS.toLong() - overhangMs - RETAINED_QUIET_MS
        if (trimMs < MIN_TRIM_MS) return ABSTAIN(frames, "quiet tail shorter than minimum")
        if (trimMs > MAX_TRIM_MS) return ABSTAIN(frames, "quiet tail longer than maximum")
        return Result(mediaEndMs - trimMs, trimMs, frames, "trailing near-zero samples")
    }
}
