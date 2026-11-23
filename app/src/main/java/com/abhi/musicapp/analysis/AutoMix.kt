package com.abhi.musicapp.analysis

import android.content.Context

/**
 * User opt-in over Crossfade. High-confidence outgoing-tail/incoming-head beat grids can
 * choose a full-content overlap start and a bounded tempo nudge. Weak, incompatible,
 * late or stale evidence falls back to plain crossfade. One-shot start requests are
 * best effort: Android sink latency and audible alignment need phone listening.
 * No Apple parity, downbeat/phrase or sample-synchronization claim. Default OFF.
 */
object AutoMix {
    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("transitions", Context.MODE_PRIVATE)

    /** The user's switch state, independent of Crossfade. */
    fun switchedOn(c: Context): Boolean = prefs(c).getBoolean("automix", false)

    fun setSwitchedOn(c: Context, on: Boolean) { prefs(c).edit().putBoolean("automix", on).apply() }

    /** AutoMix only ever acts while Crossfade is on; the switch alone changes nothing audible. */
    fun active(c: Context): Boolean = switchedOn(c) && com.abhi.musicapp.playback.Crossfader.enabled(c)
}
