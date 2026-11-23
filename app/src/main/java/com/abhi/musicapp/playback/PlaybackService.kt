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
        crossfader = Crossfader(this, player, { buildPlayer() }, { p -> session.player = p; exo = p }, { p -> onTrackChanged(p) }, phasePlanOf = { currentPhasePlan() }).also { f -> f.contentEndOf = { uri, dur -> currentTailIdentity?.takeIf { it.uri == uri && it.durationMs == dur && it == tailRevalidatedFor }?.let { validatedContentEnd } }; f.start() }
        playbackHandler.post(analysisReconcile)
    }

    private val tempoPrefetcher = com.abhi.musicapp.analysis.TempoPrefetcher()
    private val phaseWorker = com.abhi.musicapp.analysis.TransitionWorker()
    private val tailWorker = com.abhi.musicapp.analysis.TransitionWorker()
    private var tailKey: String? = null
    @Volatile private var currentTailIdentity: com.abhi.musicapp.analysis.TailCache.FileIdentity? = null
    private var tailRevalidatedFor: com.abhi.musicapp.analysis.TailCache.FileIdentity? = null
    @Volatile private var validatedContentEnd: Long? = null
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
        tailRevalidatedFor = null; validatedContentEnd = null
        refreshTailAnalysis(p)
        refreshPhaseAnalysis(p)
    }

    /**
     * One-shot identity revalidation shortly before a cached trim can be consumed. The crossfader only sees the
     * trim after the provider identity is confirmed fresh (a file replaced during playback abstains); the
     * provider is queried once per song near the transition, never on the 40 ms audio tick.
     */
    private fun revalidateTailIdentity(p: ExoPlayer) {
        val id = currentTailIdentity ?: return
        if (tailRevalidatedFor == id) return
        val dur = p.duration
        if (dur == androidx.media3.common.C.TIME_UNSET || dur <= 0L) return
        if (dur - p.currentPosition > 20_000L) return
        tailRevalidatedFor = id
        val fresh = com.abhi.musicapp.analysis.TailCache.fileIdentity(applicationContext, android.net.Uri.parse(id.uri), dur)
        validatedContentEnd = if (fresh == id) com.abhi.musicapp.analysis.TailCache.contentEndMs(id) else null
    }

    /**
     * Decodes the current song's last seconds on the background worker and records a conservative content end,
     * which the crossfader uses to end the outgoing song at the start of a near-zero-sample tail instead of the
     * file end. Skips during transitions (decode resources belong to the live decks), for short files, and when
     * a valid entry already exists. Cancellation carries into the codec loop; any irregularity abstains.
     */
    private fun refreshTailAnalysis(p: ExoPlayer) {
        if (destroyed || crossfader?.mixing == true) return
        val uri = p.currentMediaItem?.localConfiguration?.uri ?: return
        val duration = p.duration
        if (duration == androidx.media3.common.C.TIME_UNSET || duration < 30_000L) return
        // File identity = uri + provider version metadata + duration: a replaced file (even same length) never
        // reuses a stale content end. No metadata available means no trimming for that file.
        val identity = com.abhi.musicapp.analysis.TailCache.fileIdentity(applicationContext, uri, duration) ?: return
        currentTailIdentity = identity
        if (com.abhi.musicapp.analysis.TailCache.contentEndMs(identity) != null) return
        val jobKey = identity.toString()
        if (tailKey == jobKey) return
        tailKey = jobKey
        tailWorker.request(jobKey, { cancelled ->
            val w = com.abhi.musicapp.analysis.TempoCache.decodeTailWindow(applicationContext, uri, duration, 15, cancelled) ?: return@request null
            val peaks = com.abhi.musicapp.analysis.ContentEndDetector.framePeaks(w.interleaved, w.channels, w.sampleRate / 20)
            // eosReached = true: decodeTailWindow above returns null unless the codec signaled end-of-stream.
            val r = com.abhi.musicapp.analysis.ContentEndDetector.detect(peaks, w.windowStartMs, w.coverEndMs, duration, true, cancelled)
            if (cancelled()) return@request null
            // Re-validate identity AFTER the decode: a file replaced during analysis must not keep the old reading.
            if (com.abhi.musicapp.analysis.TailCache.fileIdentity(applicationContext, uri, duration) == identity) {
                r.contentEndMs?.let { com.abhi.musicapp.analysis.TailCache.put(identity, it) }
            }
            null
        }) { }
    }

    private fun analysisKey(p: ExoPlayer): String? {
        if (destroyed || !com.abhi.musicapp.analysis.AutoMix.active(this)) return null
        if (getSystemService(android.os.PowerManager::class.java)?.isPowerSaveMode == true || p.duration <= 0) return null
        return signature(p)
    }

    /** Reconciles duration becoming known, queue edits and settings while a track is already playing. */
    private fun refreshPhaseAnalysis(p: ExoPlayer) {
        // Never start or churn the MediaCodec decode + DSP while two decks are live: the transition window needs
        // every codec/audio resource, and an in-flight plan stays valid for the arm it was computed for.
        if (crossfader?.mixing == true) return
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
            exo?.let { revalidateTailIdentity(it); refreshPhaseAnalysis(it) }
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
        destroyed = true; phaseGate.clear(); phaseWorker.shutdown(); tailWorker.shutdown(); phaseResult = null
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
