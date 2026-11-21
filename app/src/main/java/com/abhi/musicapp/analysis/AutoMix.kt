package com.abhi.musicapp.analysis

import android.content.Context

/**
 * AutoMix (beta): the user's opt-in on top of Crossfade (Settings > Song Transitions). When the playing song
 * and the next one both have a detected tempo that is close enough to match (see [AutoMixPlanner]), the overlap
 * is timed to whole bars and the incoming song is nudged (at most [AutoMixPlanner.MAX_NUDGE]) to the playing
 * song's tempo during the overlap, then eased back to the user's speed. When any tempo is unknown or too far
 * apart the transition is exactly the plain crossfade. Tempo rate only: there is no beat-phase or downbeat alignment, so overlaps are tempo-matched, not beat-matched.
 *
 * This is NOT Apple's AutoMix and makes no parity claim: it runs on local files with on-device tempo analysis
 * only. Default OFF. Not heard on a phone yet - the maths is unit tested, the sound is not.
 */
object AutoMix {
    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("transitions", Context.MODE_PRIVATE)

    /** The user's switch state, independent of Crossfade. */
    fun switchedOn(c: Context): Boolean = prefs(c).getBoolean("automix", false)

    fun setSwitchedOn(c: Context, on: Boolean) { prefs(c).edit().putBoolean("automix", on).apply() }

    /** AutoMix only ever acts while Crossfade is on; the switch alone changes nothing audible. */
    fun active(c: Context): Boolean = switchedOn(c) && com.abhi.musicapp.playback.Crossfader.enabled(c)
}
