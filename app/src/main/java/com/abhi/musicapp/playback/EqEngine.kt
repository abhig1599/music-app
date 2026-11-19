package com.abhi.musicapp.playback

import android.content.Context
import android.media.audiofx.Equalizer
import androidx.media3.common.C

/**
 * Real equaliser: android.media.audiofx.Equalizer attached to the player's audio session. Band count, frequencies, dB range and
 * presets come from the phone's own effect (they differ between phones). Saved settings are re-applied whenever playback has a session.
 * If the phone has no equaliser effect or refuses it, [info] is null and the screen says so; nothing is faked.
 */
@kotlin.OptIn(androidx.media3.common.util.UnstableApi::class)
object EqEngine {
    data class Info(val bands: Int, val minMb: Int, val maxMb: Int, val centreHz: List<Int>, val presets: List<String>)

    private var eq: Equalizer? = null
    private var session = -1
    @Volatile var lastError: String? = null
        private set

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("eq", Context.MODE_PRIVATE)

    fun enabled(c: Context): Boolean = prefs(c).getBoolean("enabled", false)
    fun level(c: Context, band: Int): Int = prefs(c).getInt("band_$band", 0)

    private fun ensure(): Equalizer? {
        val p = PlaybackService.exo ?: run { lastError = "Start a song first: the equaliser attaches to the player."; return null }
        val sid = p.audioSessionId
        if (sid == C.AUDIO_SESSION_ID_UNSET || sid == 0) { lastError = "No audio session yet. Start a song first."; return null }
        if (eq == null || sid != session) {
            try { eq?.release() } catch (_: Exception) {}
            eq = try { Equalizer(0, sid) } catch (e: Exception) { lastError = "This phone did not give an equaliser effect (${e.javaClass.simpleName})."; null }
            session = sid
        }
        if (eq != null) lastError = null
        return eq
    }

    fun info(): Info? {
        val e = ensure() ?: return null
        val r = e.bandLevelRange
        return Info(e.numberOfBands.toInt(), r[0].toInt(), r[1].toInt(),
            (0 until e.numberOfBands).map { e.getCenterFreq(it.toShort()) / 1000 },
            (0 until e.numberOfPresets).map { e.getPresetName(it.toShort()) })
    }

    /** Applies saved on/off and band levels to the live effect. Safe to call often. */
    fun apply(c: Context) {
        val e = ensure() ?: return
        try {
            val r = e.bandLevelRange
            for (b in 0 until e.numberOfBands) e.setBandLevel(b.toShort(), level(c, b).coerceIn(r[0].toInt(), r[1].toInt()).toShort())
            e.enabled = enabled(c)
        } catch (ex: Exception) { lastError = "Could not apply: ${ex.javaClass.simpleName}" }
    }

    fun setEnabled(c: Context, on: Boolean) { prefs(c).edit().putBoolean("enabled", on).apply(); apply(c) }

    fun setBand(c: Context, band: Int, mb: Int) { prefs(c).edit().putInt("band_$band", mb).apply(); apply(c) }

    /** Loads one of the phone's own presets and stores the resulting band levels. */
    fun usePreset(c: Context, preset: Int) {
        val e = ensure() ?: return
        try {
            e.usePreset(preset.toShort())
            val ed = prefs(c).edit()
            for (b in 0 until e.numberOfBands) ed.putInt("band_$b", e.getBandLevel(b.toShort()).toInt())
            ed.putInt("preset", preset).apply()
        } catch (ex: Exception) { lastError = "Preset failed: ${ex.javaClass.simpleName}" }
    }

    fun preset(c: Context): Int = prefs(c).getInt("preset", -1)
}
