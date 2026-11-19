package com.abhi.musicapp.playback

import android.content.Context
import android.media.AudioDeviceInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Real overlapping crossfade (not a fade-out then fade-in). Media3 has no built-in crossfade (androidx/media issue 2), so this follows the
 * two-player approach used by other Media3 apps: the normal player [main] keeps the queue, the media session, the UI state, the equaliser and
 * the visualiser. Near the end of a song a second player [incoming] starts the next song on the SAME audio session while the volumes follow an
 * equal-power curve. When the ramp ends, [main] seeks into the next song at the incoming player's position, is faded up while the incoming
 * player is faded out and released. Off by default; user turns it on in Settings > Song Transitions.
 *
 * NOT hardware-verified: there is no way to hear audio in CI. A short hand-over of roughly 0.1-0.3 s between the two players can be audible.
 * Pause, seek, next/previous, repeat-one and "finish this song" sleep timer all cancel or skip the crossfade so normal playback keeps working.
 */
@kotlin.OptIn(UnstableApi::class)
class Crossfader(private val ctx: Context, private val main: ExoPlayer) {
    private val handler = Handler(Looper.getMainLooper())
    private var incoming: ExoPlayer? = null
    private var rampStart = 0L
    private var fadeMs = 4000L
    private var toIndex = C.INDEX_UNSET
    private var handingOff = false
    private var handOffStart = 0L
    private var backStart = 0L
    @Volatile var muted = false
        private set

    private val base get() = if (muted) 0f else 1f

    private val loop = object : Runnable {
        override fun run() {
            try { step() } catch (e: Exception) { cancel() }
            handler.postDelayed(this, if (incoming != null) 40L else 250L)
        }
    }

    private val mainListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady && incoming != null && !handingOff) cancel()
        }
        override fun onPositionDiscontinuity(old: Player.PositionInfo, new: Player.PositionInfo, reason: Int) {
            if (incoming == null || handingOff) return
            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION && main.currentMediaItemIndex == toIndex) handOff()
            else if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT || reason == Player.DISCONTINUITY_REASON_REMOVE) cancel()
        }
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            if (incoming != null && !handingOff && reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) cancel()
        }
        override fun onPlaybackParametersChanged(p: androidx.media3.common.PlaybackParameters) { incoming?.playbackParameters = p }
        override fun onRepeatModeChanged(repeatMode: Int) { if (repeatMode == Player.REPEAT_MODE_ONE) cancel() }
    }

    fun start() { main.addListener(mainListener); handler.post(loop) }
    fun stop() { handler.removeCallbacks(loop); main.removeListener(mainListener); cancel() }

    fun setMuted(m: Boolean) { muted = m; if (incoming == null) main.volume = base }

    private fun step() {
        val inc = incoming
        if (inc == null) { maybeBegin(); return }
        val now = SystemClock.uptimeMillis()
        if (handingOff) {
            if (main.playbackState == Player.STATE_READY && main.isPlaying) {
                if (backStart == 0L) backStart = now
                val t = ((now - backStart) / 250f).coerceIn(0f, 1f)
                main.volume = base * t; inc.volume = base * (1f - t)
                if (t >= 1f) finish()
            } else if (now - handOffStart > 3000L) finish()
            return
        }
        if (rampStart == 0L) return // incoming not ready yet; main keeps playing at full volume
        val t = ((now - rampStart) / fadeMs.toFloat()).coerceIn(0f, 1f)
        main.volume = base * cos(t * PI / 2).toFloat()
        inc.volume = base * sin(t * PI / 2).toFloat()
        if (t >= 1f) handOff()
    }

    private fun maybeBegin() {
        if (!enabled(ctx) || !main.isPlaying) return
        if (main.repeatMode == Player.REPEAT_MODE_ONE || SleepTimer.isFinishingSong) return
        val next = main.nextMediaItemIndex
        if (next == C.INDEX_UNSET) return
        val dur = main.duration
        if (dur == C.TIME_UNSET || dur <= 0L) return
        val f = min(seconds(ctx) * 1000L, dur / 3)
        val remaining = dur - main.currentPosition
        if (!shouldBegin(remaining, f, main.playbackParameters.speed)) return
        fadeMs = f; toIndex = next
        val p = ExoPlayer.Builder(ctx).build()
        p.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false)
        if (main.audioSessionId != C.AUDIO_SESSION_ID_UNSET) p.audioSessionId = main.audioSessionId
        AudioOutput.preferredDevice?.let { d: AudioDeviceInfo -> p.setPreferredAudioDevice(d) }
        p.playbackParameters = main.playbackParameters
        p.volume = 0f
        p.setMediaItem(main.getMediaItemAt(next))
        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_READY && rampStart == 0L && !handingOff) rampStart = SystemClock.uptimeMillis() }
        })
        p.prepare(); p.playWhenReady = true
        incoming = p; rampStart = 0L; handingOff = false; backStart = 0L
    }

    /** Ramp finished (or main reached the end by itself): continue the queue on [main] at the incoming player's position. */
    private fun handOff() {
        val inc = incoming ?: return
        handingOff = true; handOffStart = SystemClock.uptimeMillis(); backStart = 0L
        main.volume = 0f; inc.volume = base
        main.seekTo(toIndex, inc.currentPosition)
        main.playWhenReady = true
    }

    private fun finish() {
        incoming?.release(); incoming = null
        handingOff = false; rampStart = 0L; backStart = 0L; toIndex = C.INDEX_UNSET
        main.volume = base
    }

    /** Abort: drop the incoming player and put the main player back at normal volume. */
    private fun cancel() {
        if (incoming == null) { main.volume = base; return }
        finish()
    }

    companion object {
        private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("transitions", Context.MODE_PRIVATE)
        /** Start the incoming song when the wall-clock time left (media time / speed) is within the fade plus 1.5 s, and not already over. */
        internal fun shouldBegin(remainingMediaMs: Long, fadeMs: Long, speed: Float): Boolean {
            val sp = if (speed > 0f) speed else 1f
            val wall = remainingMediaMs / sp
            return wall <= fadeMs + 1500f && wall >= 300f
        }
        /** Equal-power gains for outgoing and incoming at ramp position t in 0..1. */
        internal fun gains(t: Float): Pair<Float, Float> {
            val x = t.coerceIn(0f, 1f)
            return cos(x * PI / 2).toFloat() to sin(x * PI / 2).toFloat()
        }
        fun enabled(c: Context) = prefs(c).getBoolean("on", false)
        fun seconds(c: Context) = prefs(c).getInt("secs", 4).coerceIn(1, 12)
        fun setEnabled(c: Context, on: Boolean) { prefs(c).edit().putBoolean("on", on).apply() }
        fun setSeconds(c: Context, s: Int) { prefs(c).edit().putInt("secs", s.coerceIn(1, 12)).apply() }
    }
}
