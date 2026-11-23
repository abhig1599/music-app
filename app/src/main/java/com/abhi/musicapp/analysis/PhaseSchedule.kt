package com.abhi.musicapp.analysis

import kotlin.math.abs
import kotlin.math.roundToLong

/** Converts an original-media plan to wall-clock scheduling, rejecting conditions that invalidate phase evidence. */
object PhaseSchedule {
    data class Timing(val delayMs: Long, val fadeWallMs: Long, val incomingSpeed: Float)
    fun prepare(plan: PhaseTransitionPlanner.Plan, outgoingPositionMs: Long, outgoingDurationMs: Long,
                incomingDurationMs: Long, userSpeed: Float, userFadeMs: Long): Timing? {
        if (!plan.aligned || !userSpeed.isFinite() || userSpeed !in .25f..4f || outgoingPositionMs < 0) return null
        if (!plan.incomingSpeedRatio.isFinite() || abs(plan.incomingSpeedRatio - 1f) > .06f) return null
        val error = plan.predictedMaxPhaseErrorMs ?: return null
        if (!error.isFinite() || error !in 0f..30f) return null
        if (plan.fadeMs < 500 || plan.startOutgoingMs < outgoingPositionMs || outgoingDurationMs <= 0 || incomingDurationMs <= 0) return null
        if (plan.startOutgoingMs > outgoingDurationMs || plan.fadeMs != outgoingDurationMs - plan.startOutgoingMs) return null
        val fadeWall = (plan.fadeMs / userSpeed.toDouble()).roundToLong()
        // Never shorten or move a phase plan to fit another duration: that destroys its evidence.
        if (fadeWall < 500 || fadeWall > userFadeMs.coerceIn(0,12000)) return null
        val incomingSpeed = userSpeed * plan.incomingSpeedRatio
        if (incomingSpeed !in .25f..4f || fadeWall * incomingSpeed > incomingDurationMs / 3.0) return null
        return Timing(((plan.startOutgoingMs - outgoingPositionMs) / userSpeed.toDouble()).roundToLong(),fadeWall,incomingSpeed)
    }
    /** Late deck starts invalidate the prediction; a caller must choose plain crossfade instead. */
    fun canStart(actualPositionMs: Long, plannedStartMs: Long, userSpeed: Float): Boolean =
        userSpeed.isFinite() && userSpeed > 0 && abs(actualPositionMs.toDouble() - plannedStartMs) / userSpeed <= 10.0
}
