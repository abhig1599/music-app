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
 * two-player approach used by other Media3 apps, with a role swap so no song is ever replayed:
 * the active player [main] owns the queue and the media session. Near the end of a song a second player [incoming], holding the same queue
 * starting at the next song, begins on the SAME audio session while the volumes follow an equal-power curve. When the ramp ends the media
 * session is pointed at the incoming player (MediaSession.setPlayer), which simply keeps playing, and the old player is released. There is no
 * seek and no second copy of the same song, so nothing can be offset. Off by default (Settings > Song Transitions).
 *
 * NOT hardware-verified: there is no way to hear audio in CI. Whether the session hand-over leaves the notification, the controls and the
 * visualiser consistent is only checked by compiling and by the maths tests. Pause, seek, next/previous, repeat-one, shuffle and the
 * "finish this song" sleep timer all cancel or skip the crossfade so normal playback keeps working.
 */
@kotlin.OptIn(UnstableApi::class)
class Crossfader(
    private val ctx: Context,
    private var main: ExoPlayer,
    private val newPlayer: () -> ExoPlayer,
    private val onSwap: (ExoPlayer) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var incoming: ExoPlayer? = null
    private var incomingReady: Player.Listener? = null
    private var rampStart = 0L
    private var fadeMs = 4000L
    private var toIndex = C.INDEX_UNSET
    private var beganAt = 0L
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
            if (!playWhenReady && incoming != null) cancel()
        }
        override fun onPositionDiscontinuity(old: Player.PositionInfo, new: Player.PositionInfo, reason: Int) {
            if (incoming == null) return
            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION && main.currentMediaItemIndex == toIndex) {
                // Main reached the next song itself. Hand over only if the incoming player is actually playing; otherwise main just carries on.
                if (rampStart != 0L) swap() else cancel()
            }
            else if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT || reason == Player.DISCONTINUITY_REASON_REMOVE) cancel()
        }
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            if (incoming != null && reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) cancel()
        }
        override fun onPlaybackParametersChanged(p: androidx.media3.common.PlaybackParameters) { incoming?.playbackParameters = p }
        override fun onRepeatModeChanged(repeatMode: Int) { if (repeatMode == Player.REPEAT_MODE_ONE) cancel() }
        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) { if (incoming != null) cancel() }
    }

    // Test seams (no audio): let a unit test put a player in the incoming role and drive the swap and cancel paths.
    internal val activePlayer: ExoPlayer get() = main
    internal fun injectIncomingForTest(p: ExoPlayer, toIdx: Int) { incoming = p; toIndex = toIdx; rampStart = 0L }
    internal fun swapForTest() = swap()
    internal fun cancelForTest() = cancel()

    fun start() { main.addListener(mainListener); handler.post(loop) }
    fun stop() { handler.removeCallbacks(loop); main.removeListener(mainListener); cancel() }

    fun setMuted(m: Boolean) { muted = m; if (incoming == null) main.volume = base }

    private fun step() {
        val inc = incoming
        if (inc == null) { maybeBegin(); return }
        if (rampStart == 0L) { // incoming not ready yet; main keeps playing at full volume, but do not wait forever
            if (shouldGiveUp(SystemClock.uptimeMillis() - beganAt)) cancel()
            return
        }
        val t = ((SystemClock.uptimeMillis() - rampStart) / fadeMs.toFloat()).coerceIn(0f, 1f)
        val (out, into) = gains(t)
        main.volume = base * out
        inc.volume = base * into
        if (t >= 1f) swap()
    }

    private fun maybeBegin() {
        if (!enabled(ctx) || !main.isPlaying) return
        if (main.repeatMode == Player.REPEAT_MODE_ONE || main.shuffleModeEnabled || SleepTimer.isFinishingSong) return
        val next = main.nextMediaItemIndex
        if (next == C.INDEX_UNSET) return
        val dur = main.duration
        if (dur == C.TIME_UNSET || dur <= 0L) return
        val f = min(seconds(ctx) * 1000L, dur / 3)
        val remaining = dur - main.currentPosition
        if (!shouldBegin(remaining, f, main.playbackParameters.speed)) return
        fadeMs = f; toIndex = next
        val p = newPlayer()
        p.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false)
        if (main.audioSessionId != C.AUDIO_SESSION_ID_UNSET) p.audioSessionId = main.audioSessionId
        AudioOutput.preferredDevice?.let { d: AudioDeviceInfo -> p.setPreferredAudioDevice(d) }
        p.playbackParameters = main.playbackParameters
        p.repeatMode = main.repeatMode
        p.volume = 0f
        // Same queue as the active player, starting at the next song, so after the swap next/previous behave exactly as before.
        p.setMediaItems((0 until main.mediaItemCount).map { main.getMediaItemAt(it) }, next, 0L)
        val l = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_READY && rampStart == 0L) rampStart = SystemClock.uptimeMillis() }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) { cancel() } // unreadable next file: skip the fade
        }
        incomingReady = l; p.addListener(l)
        p.prepare(); p.playWhenReady = true
        incoming = p; rampStart = 0L; beganAt = SystemClock.uptimeMillis()
    }

    /** The incoming player becomes the active one: point the session at it and release the old player. No seek, no replay. */
    private fun swap() {
        val inc = incoming ?: return
        val old = main
        old.removeListener(mainListener)
        incomingReady?.let { inc.removeListener(it) }
        incomingReady = null; incoming = null; rampStart = 0L; toIndex = C.INDEX_UNSET
        inc.volume = base
        main = inc
        onSwap(inc)
        inc.addListener(mainListener)
        old.volume = 0f; old.playWhenReady = false; old.release()
    }

    /** Abort: drop the incoming player and put the main player back at normal volume. */
    private fun cancel() {
        val inc = incoming
        if (inc != null) {
            incomingReady?.let { inc.removeListener(it) }
            incomingReady = null; incoming = null; rampStart = 0L; toIndex = C.INDEX_UNSET
            inc.release()
        }
        main.volume = base
    }

    companion object {
        private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("transitions", Context.MODE_PRIVATE)
        /** Start the incoming song when the wall-clock time left (media time / speed) is within the fade plus 1.5 s, and not already over. */
        internal fun shouldBegin(remainingMediaMs: Long, fadeMs: Long, speed: Float): Boolean {
            val sp = if (speed > 0f) speed else 1f
            val wall = remainingMediaMs / sp
            return wall <= fadeMs + 1500f && wall >= 300f
        }
        /** The incoming player has this long (wall clock) to become ready before the fade is abandoned. */
        internal fun shouldGiveUp(sinceBeginMs: Long) = sinceBeginMs > 4000L
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
