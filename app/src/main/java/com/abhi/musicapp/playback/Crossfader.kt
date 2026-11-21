package com.abhi.musicapp.playback

import android.content.Context
import android.media.AudioDeviceInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.abhi.musicapp.analysis.AutoMix
import com.abhi.musicapp.analysis.AutoMixPlanner
import com.abhi.musicapp.analysis.TempoCache
import com.abhi.musicapp.analysis.TempoResult
import kotlin.math.PI
import kotlin.math.abs
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
 * AutoMix (beta, user opt-in on top of Crossfade): when both songs have a detected, close-enough tempo, the fade is timed to whole bars of
 * the playing song and the incoming player runs nudged (at most 6%) to the playing song's tempo during the overlap, then eases back to the
 * user's speed over ~1.5 s after the swap instead of stepping. Tempo rate only: NO beat-phase or downbeat alignment, so overlaps are
 * tempo-matched, not beat-matched. Unknown tempos fall back to exactly the plain crossfade; a WRONG tempo can sound worse than plain, which
 * is the beta risk a phone test must judge. See analysis/AutoMix.kt and docs/AUTOMIX_RESEARCH.md.
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
    private val onItemChanged: (ExoPlayer) -> Unit = {},
    private val autoMixActive: () -> Boolean = { AutoMix.active(ctx) },
    private val tempoOf: (androidx.media3.common.MediaItem) -> Float? = { item ->
        TempoCache.cached(ctx, TempoCache.keyFor(ctx, item.localConfiguration?.uri, item.mediaId))?.takeIf { it > 0f }
    },
) {
    private val handler = Handler(Looper.getMainLooper())
    private var incoming: ExoPlayer? = null
    private var incomingReady: Player.Listener? = null
    private var rampStart = 0L
    private var fadeMs = 4000L
    private var toIndex = C.INDEX_UNSET
    private var beganAt = 0L
    private var fadeChecked = false
    /** AutoMix tempo factor the incoming player is running at during the overlap (1 = plain crossfade). */
    private var nudge = 1f
    /** Post-swap ease of the new active player's speed back to the user's setting; null when not restoring. */
    private var restore: Restore? = null
    @Volatile var muted = false
        private set

    private class Restore(val from: Float, val to: Float, val start: Long, var last: Float)

    private val base get() = if (muted) 0f else 1f

    private val loop = object : Runnable {
        override fun run() {
            try { step() } catch (e: Exception) { cancel() }
            handler.postDelayed(this, if (incoming != null || restore != null) 40L else 250L)
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
            onItemChanged(main)
            if (incoming != null && reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) cancel()
        }
        override fun onPlaybackParametersChanged(p: PlaybackParameters) {
            val r = restore
            if (r != null) {
                // During the post-swap ease, any speed that is not the one we just applied is the user's: theirs wins, the ease stops.
                if (abs(p.speed - r.last) > 0.0005f) restore = null
                return
            }
            incoming?.playbackParameters = PlaybackParameters(p.speed * nudge, p.pitch)
        }
        override fun onRepeatModeChanged(repeatMode: Int) { if (repeatMode == Player.REPEAT_MODE_ONE) cancel() }
        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) { if (incoming != null) cancel() }
    }

    // Test seams (no audio): let a unit test put a player in the incoming role and drive the swap and cancel paths.
    internal val activePlayer: ExoPlayer get() = main
    internal val restorePendingForTest: Boolean get() = restore != null
    internal fun injectIncomingForTest(p: ExoPlayer, toIdx: Int, nudgeFactor: Float = 1f) { incoming = p; toIndex = toIdx; rampStart = 0L; nudge = nudgeFactor }
    internal fun swapForTest() = swap()
    internal fun cancelForTest() = cancel()

    fun start() { main.addListener(mainListener); handler.post(loop); onItemChanged(main) }
    fun stop() { handler.removeCallbacks(loop); main.removeListener(mainListener); cancel() }

    fun setMuted(m: Boolean) { muted = m; if (incoming == null) main.volume = base }

    private fun step() {
        val inc = incoming
        if (inc == null) { stepRestore(); maybeBegin(); return }
        if (rampStart == 0L) { // incoming not ready yet; main keeps playing at full volume, but do not wait forever
            if (shouldGiveUp(SystemClock.uptimeMillis() - beganAt)) cancel()
            return
        }
        if (!fadeChecked) { // now that the incoming song is ready its length is known: never fade longer than a third of it
            fadeChecked = true
            val e = effectiveFade(fadeMs, inc.duration)
            if (e == 0L) { cancel(); return }
            fadeMs = e
        }
        val t = ((SystemClock.uptimeMillis() - rampStart) / fadeMs.toFloat()).coerceIn(0f, 1f)
        val (out, into) = gains(t)
        main.volume = base * out
        inc.volume = base * into
        if (t >= 1f) swap()
    }

    /** Eases the post-swap player speed back to the user's setting. Runs on the 40 ms loop while a restore is pending. */
    private fun stepRestore() {
        val r = restore ?: return
        val s = restoreSpeedAt(r.from, r.to, SystemClock.uptimeMillis() - r.start)
        if (s == null) { r.last = r.to; applySpeed(r.to); restore = null }
        else { r.last = s; applySpeed(s) }
    }

    private fun applySpeed(s: Float) { main.playbackParameters = PlaybackParameters(s, main.playbackParameters.pitch) }

    private fun maybeBegin() {
        if (!enabled(ctx) || !main.isPlaying) return
        if (main.repeatMode == Player.REPEAT_MODE_ONE || main.shuffleModeEnabled || SleepTimer.isFinishingSong) return
        val next = main.nextMediaItemIndex
        if (next == C.INDEX_UNSET) return
        val dur = main.duration
        if (dur == C.TIME_UNSET || dur <= 0L) return
        val plan = plannedTransition(
            if (autoMixActive()) main.currentMediaItem?.let(tempoOf) else null,
            if (autoMixActive()) tempoOf(main.getMediaItemAt(next)) else null,
            seconds(ctx), dur,
        )
        val f = plan.first
        val remaining = dur - main.currentPosition
        if (!shouldBegin(remaining, f, main.playbackParameters.speed)) return
        fadeMs = f; nudge = plan.second; toIndex = next
        val p = newPlayer()
        p.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false)
        if (main.audioSessionId != C.AUDIO_SESSION_ID_UNSET) p.audioSessionId = main.audioSessionId
        AudioOutput.preferredDevice?.let { d: AudioDeviceInfo -> p.setPreferredAudioDevice(d) }
        // AutoMix: the incoming song starts at the nudged tempo so the overlap is tempo-matched (beats are NOT phase-aligned); pitch is preserved (Media3 Sonic).
        p.playbackParameters = PlaybackParameters(main.playbackParameters.speed * nudge, main.playbackParameters.pitch)
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
        incoming = p; rampStart = 0L; fadeChecked = false; beganAt = SystemClock.uptimeMillis()
    }

    /** The incoming player becomes the active one: point the session at it and release the old player. No seek, no replay. */
    private fun swap() {
        val inc = incoming ?: return
        val old = main
        old.removeListener(mainListener)
        incomingReady?.let { inc.removeListener(it) }
        incomingReady = null; incoming = null; rampStart = 0L; toIndex = C.INDEX_UNSET
        inc.volume = base
        // AutoMix: the incoming song ran nudged to the old song's tempo during the overlap. Ease it back to the user's speed
        // over ~1.5 s rather than stepping; a user speed change during the ease wins and stops it.
        val userParams = old.playbackParameters
        if (nudge != 1f && abs(inc.playbackParameters.speed - userParams.speed) > 0.0005f) {
            restore = Restore(inc.playbackParameters.speed, userParams.speed, SystemClock.uptimeMillis(), inc.playbackParameters.speed)
        } else if (inc.playbackParameters != userParams) {
            inc.playbackParameters = userParams
        }
        nudge = 1f
        main = inc
        onSwap(inc)
        inc.addListener(mainListener)
        onItemChanged(inc)
        old.volume = 0f; old.playWhenReady = false; old.release()
    }

    /** Abort: drop the incoming player, put the main player back at normal volume and settle any speed ease at the user's speed. */
    private fun cancel() {
        val inc = incoming
        if (inc != null) {
            incomingReady?.let { inc.removeListener(it) }
            incomingReady = null; incoming = null; rampStart = 0L; toIndex = C.INDEX_UNSET
            inc.release()
        }
        nudge = 1f
        restore?.let { it.last = it.to; main.playbackParameters = PlaybackParameters(it.to, main.playbackParameters.pitch); restore = null }
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
        /**
         * Fade length given the next song's duration. Unknown or invalid duration (unset, zero or negative) keeps the planned fade. A known
         * duration caps the fade at a third of it. Under 500 ms left means the song is too short to crossfade into: 0 = skip the fade.
         */
        internal fun effectiveFade(fadeMs: Long, nextDurationMs: Long): Long {
            if (nextDurationMs == C.TIME_UNSET || nextDurationMs <= 0L) return fadeMs
            val e = min(fadeMs, nextDurationMs / 3)
            return if (e < 500L) 0L else e
        }
        /** The incoming player has this long (wall clock) to become ready before the fade is abandoned. */
        internal fun shouldGiveUp(sinceBeginMs: Long) = sinceBeginMs > 4000L
        /** Equal-power gains for outgoing and incoming at ramp position t in 0..1. */
        internal fun gains(t: Float): Pair<Float, Float> {
            val x = t.coerceIn(0f, 1f)
            return cos(x * PI / 2).toFloat() to sin(x * PI / 2).toFloat()
        }
        /**
         * Fade length and incoming speed factor for one transition. Null tempo on either side (or AutoMix off, handled by the caller
         * passing null) means the plain crossfade: the user's seconds capped at a third of the PLAYING song. A tempo-matched plan is also
         * capped at a third of the playing song, and falls back to plain when that leaves under 500 ms. Pure maths for unit tests.
         */
        internal fun plannedTransition(outBpm: Float?, inBpm: Float?, userSecs: Int, outDurMs: Long): Pair<Long, Float> {
            val plain = min(userSecs.coerceIn(1, 12) * 1000L, if (outDurMs > 0L) outDurMs / 3 else Long.MAX_VALUE)
            if (outBpm == null || inBpm == null || outDurMs <= 0L) return plain to 1f
            val p = AutoMixPlanner.plan(TempoResult(outBpm, 1f), TempoResult(inBpm, 1f), userSecs)
            if (!p.beatMatched) return plain to 1f
            val f = min(p.fadeMs, outDurMs / 3)
            return if (f < 500L) plain to 1f else f to p.incomingSpeed
        }
        /**
         * Speed during the post-swap ease back to the user's setting: linear from [from] to [to] over [totalMs].
         * Null once the ease is done (caller then applies [to] exactly and stops).
         */
        internal fun restoreSpeedAt(from: Float, to: Float, elapsedMs: Long, totalMs: Long = 1500L): Float? {
            if (elapsedMs >= totalMs) return null
            val t = (elapsedMs.coerceAtLeast(0L) / totalMs.toFloat()).coerceIn(0f, 1f)
            return from + (to - from) * t
        }
        fun enabled(c: Context) = prefs(c).getBoolean("on", false)
        fun seconds(c: Context) = prefs(c).getInt("secs", 4).coerceIn(1, 12)
        fun setEnabled(c: Context, on: Boolean) { prefs(c).edit().putBoolean("on", on).apply() }
        fun setSeconds(c: Context, s: Int) { prefs(c).edit().putInt("secs", s.coerceIn(1, 12)).apply() }
    }
}
