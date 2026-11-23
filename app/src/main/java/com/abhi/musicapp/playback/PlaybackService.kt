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
        crossfader = Crossfader(this, player, { buildPlayer() }, { p -> session.player = p; exo = p }, { p -> onTrackChanged(p) }, phasePlanOf = { currentPhasePlan() }).also { it.start() }
        playbackHandler.post(analysisReconcile)
    }

    private val tempoPrefetcher = com.abhi.musicapp.analysis.TempoPrefetcher()
    private val phaseWorker = com.abhi.musicapp.analysis.TransitionWorker()
    private val playbackHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val phaseGate = com.abhi.musicapp.analysis.AnalysisRequestGate()
    private var destroyed = false
    /** Conservative pair evidence, consumed only under the existing AutoMix opt-in. */
    private var phaseResult: com.abhi.musicapp.analysis.TransitionAnalysisResult? = null
    private var phaseSignature: String? = null
    private fun signature(p: ExoPlayer): String? {
        val n=p.nextMediaItemIndex
        if (n==androidx.media3.common.C.INDEX_UNSET) return null
        return "${p.currentMediaItem?.localConfiguration?.uri}|${p.getMediaItemAt(n).localConfiguration?.uri}|$n|${p.duration}|${Crossfader.seconds(this)}"
    }
    private fun currentPhasePlan(): com.abhi.musicapp.analysis.PhaseTransitionPlanner.Plan? {
        val p=exo ?: return null
        return phaseResult?.plan?.takeIf { phaseSignature!=null && phaseSignature==analysisKey(p) }
    }


    private fun onTrackChanged(p: ExoPlayer) {
        prefetchNext(p)
        refreshPhaseAnalysis(p)
    }

    private fun analysisKey(p: ExoPlayer): String? {
        if (destroyed || !com.abhi.musicapp.analysis.AutoMix.active(this)) return null
        if (getSystemService(android.os.PowerManager::class.java)?.isPowerSaveMode == true || p.duration <= 0) return null
        return signature(p)
    }

    /** Reconciles duration becoming known, queue edits and settings while a track is already playing. */
    private fun refreshPhaseAnalysis(p: ExoPlayer) {
        val key = analysisKey(p)
        if (!phaseGate.update(key)) return
        phaseWorker.cancel(); phaseResult = null; phaseSignature = null
        if (key == null) return
        val generation = phaseGate.generation
        val out = p.currentMediaItem?.localConfiguration?.uri ?: return
        val nextIndex = p.nextMediaItemIndex
        if (nextIndex == androidx.media3.common.C.INDEX_UNSET) return
        val into = p.getMediaItemAt(nextIndex).localConfiguration?.uri ?: return
        val duration = p.duration
        val fade = Crossfader.seconds(this) * 1000L
        phaseWorker.request(key, { cancelled ->
            com.abhi.musicapp.analysis.TransitionAnalysis.analyze(applicationContext,out,into,duration,fade,cancelled)
        }) { result ->
            playbackHandler.post {
                if (destroyed || !phaseGate.accepts(key,generation) || exo !== p || analysisKey(p) != key) return@post
                phaseResult = result; phaseSignature = signature(p)
            }
        }
    }

    private val analysisReconcile = object : Runnable {
        override fun run() {
            if (destroyed) return
            exo?.let { refreshPhaseAnalysis(it) }
            playbackHandler.postDelayed(this,1000L)
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
        destroyed = true; phaseGate.clear(); phaseWorker.shutdown(); phaseResult = null
        playbackHandler.removeCallbacksAndMessages(null)
        crossfader?.stop(); crossfader = null
        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null
        tempoPrefetcher.shutdown()
        exo = null
        super.onDestroy()
    }
}
