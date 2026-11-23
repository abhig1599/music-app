package com.abhi.musicapp.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Media3 session service. Runs as a mediaPlayback foreground service while
 * playing so local audio keeps going in the background.
 * Platform mechanics verified against:
 * https://developer.android.com/about/versions/14/changes/fgs-types-required#media-playback
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    companion object {
        /** The one in-process player, for output preference and mute (same process as the UI; used on the main thread only). */
        @Volatile var exo: ExoPlayer? = null
        @Volatile var crossfader: Crossfader? = null
    }

    override fun onCreate() {
        super.onCreate()
        val player = buildPlayer()
        exo = player
        val session = MediaSession.Builder(this, player).build()
        mediaSession = session
        crossfader = Crossfader(this, player, { buildPlayer() }, { p -> session.player = p; exo = p }, { p -> onTrackChanged(p) }, phasePlanOf = { currentShadowPlan() }).also { it.start() }
    }

    private val tempoPrefetcher = com.abhi.musicapp.analysis.TempoPrefetcher()
    private val phaseWorker = com.abhi.musicapp.analysis.TransitionWorker()
    private val playbackHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var phaseGeneration = 0L
    private var destroyed = false
    /** Shadow evidence only. No audible path reads this until the timing/output gate is met. */
    private var shadowPhase: com.abhi.musicapp.analysis.TransitionAnalysisResult? = null
    private var shadowSignature: String? = null
    private fun signature(p: ExoPlayer): String? {
        val n=p.nextMediaItemIndex
        if (n==androidx.media3.common.C.INDEX_UNSET) return null
        return "${p.currentMediaItem?.localConfiguration?.uri}|${p.getMediaItemAt(n).localConfiguration?.uri}|$n|${p.duration}|${Crossfader.seconds(this)}"
    }
    private fun currentShadowPlan(): com.abhi.musicapp.analysis.PhaseTransitionPlanner.Plan? {
        val p=exo ?: return null
        return shadowPhase?.plan?.takeIf { shadowSignature!=null && shadowSignature==signature(p) }
    }


    private fun onTrackChanged(p: ExoPlayer) {
        prefetchNext(p)
        analyseForAutoMix(p)
        analyzePhaseShadow(p)
    }

    /**
     * AutoMix needs both tempos cached before the overlap starts. Analyse the PLAYING song first (it sets the target tempo and has a
     * whole song's length to finish in), then the next queued one. Results land in TempoCache; the Crossfader only reads the cache,
     * so a song that is not analysed in time simply gets the plain crossfade. Skipped under battery saver, like the prefetch.
     */
    private fun analyseForAutoMix(p: ExoPlayer) {
        if (!com.abhi.musicapp.analysis.AutoMix.active(this)) return
        val pm = getSystemService(android.os.PowerManager::class.java)
        if (pm?.isPowerSaveMode == true) return
        val cur = p.currentMediaItem ?: return
        val curUri = cur.localConfiguration?.uri ?: return
        val nextIdx = p.nextMediaItemIndex
        val next = if (nextIdx != androidx.media3.common.C.INDEX_UNSET) p.getMediaItemAt(nextIdx) else null
        val curKey = com.abhi.musicapp.analysis.TempoCache.keyFor(this, curUri, cur.mediaId)
        tempoPrefetcher.request(curKey.value, { cancelled -> com.abhi.musicapp.analysis.TempoCache.analyse(this, curUri, curKey, cancelled) }) { _ ->
            if (!com.abhi.musicapp.analysis.AutoMix.active(this)) return@request
            val u = next?.localConfiguration?.uri ?: return@request
            val nextKey = com.abhi.musicapp.analysis.TempoCache.keyFor(this, u, next!!.mediaId)
            tempoPrefetcher.request(nextKey.value, { c2 -> com.abhi.musicapp.analysis.TempoCache.analyse(this, u, nextKey, c2) })
        }
    }

    private fun analyzePhaseShadow(p: ExoPlayer) {
        val generation = ++phaseGeneration
        phaseWorker.cancel(); shadowPhase = null; shadowSignature = null
        // Development evidence only, disabled in ordinary installs to avoid duplicate decoding.
        if (!getSharedPreferences("transitions", Context.MODE_PRIVATE).getBoolean("phase_shadow", false)) return
        if (!com.abhi.musicapp.analysis.AutoMix.active(this)) return
        if (getSystemService(android.os.PowerManager::class.java)?.isPowerSaveMode == true) return
        val out = p.currentMediaItem?.localConfiguration?.uri ?: return
        val nextIndex = p.nextMediaItemIndex
        if (nextIndex == androidx.media3.common.C.INDEX_UNSET) return
        val into = p.getMediaItemAt(nextIndex).localConfiguration?.uri ?: return
        val duration = p.duration
        if (duration <= 0) return
        val fade = Crossfader.seconds(this) * 1000L
        phaseWorker.request("$generation", { cancelled ->
            com.abhi.musicapp.analysis.TransitionAnalysis.analyze(applicationContext,out,into,duration,fade,cancelled)
        }) { result ->
            playbackHandler.post {
                if (destroyed || generation != phaseGeneration || exo !== p ||
                    !com.abhi.musicapp.analysis.AutoMix.active(this) || p.duration != duration ||
                    p.currentMediaItem?.localConfiguration?.uri != out || p.nextMediaItemIndex != nextIndex ||
                    p.getMediaItemAt(nextIndex).localConfiguration?.uri != into || Crossfader.seconds(this)*1000L != fade) return@post
                shadowPhase = result; shadowSignature = signature(p)
                android.util.Log.i("PhaseShadow", "analysis only: aligned=${result.plan.aligned}, reason=${result.plan.reason}")
            }
        }
    }

    /** Hidden, off by default: analyse the tempo of the NEXT queued local song in the background and cache it. Nothing audible uses it. */
    private fun prefetchNext(p: ExoPlayer) {
        if (!com.abhi.musicapp.analysis.TempoCache.prefetchEnabled(this)) return
        val pm = getSystemService(android.os.PowerManager::class.java)
        if (pm?.isPowerSaveMode == true) return
        val next = p.nextMediaItemIndex
        if (next == androidx.media3.common.C.INDEX_UNSET) return
        val item = p.getMediaItemAt(next)
        val uri = item.localConfiguration?.uri ?: return
        val key = com.abhi.musicapp.analysis.TempoCache.keyFor(this, uri, item.mediaId)
        tempoPrefetcher.request(key.value, { cancelled -> com.abhi.musicapp.analysis.TempoCache.analyse(this, uri, key, cancelled) })
    }

    /** Same sink as the default, plus a pass-through tap that feeds the Player dial's frequency levels. Used for every player this service creates. */
    private fun buildPlayer(): ExoPlayer {
        val factory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink =
                DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(
                        // PcmTap exists only when the build was invoked with -PpcmTap=true (the emulator transition smoke). Candidate
                        // APKs are built without it: no tap in the audio chain, no pref to arm, nothing restorable from a backup.
                        if (com.abhi.musicapp.BuildConfig.PCM_TAP) arrayOf(AudioLevels.newProcessor(), PcmTap.newProcessor(context))
                        else arrayOf(AudioLevels.newProcessor()),
                    )
                    .build()
        }
        return ExoPlayer.Builder(this, factory).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        destroyed = true; phaseGeneration++; phaseWorker.shutdown(); shadowPhase = null
        playbackHandler.removeCallbacksAndMessages(null)
        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null
        crossfader?.stop(); crossfader = null
        tempoPrefetcher.shutdown()
        exo = null
        super.onDestroy()
    }
}
