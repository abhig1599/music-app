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
        crossfader = Crossfader(this, player, { buildPlayer() }, { p -> session.player = p; exo = p }, { p -> onTrackChanged(p) }).also { it.start() }
    }

    private val tempoPrefetcher = com.abhi.musicapp.analysis.TempoPrefetcher()

    private fun onTrackChanged(p: ExoPlayer) {
        prefetchNext(p)
        analyseForAutoMix(p)
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
                        // PcmTap is wired only into debuggable builds and writes nothing unless the smoke arms its pref; release builds get the level tap alone.
                        if ((context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) arrayOf(AudioLevels.newProcessor(), PcmTap.newProcessor(context))
                        else arrayOf(AudioLevels.newProcessor()),
                    )
                    .build()
        }
        return ExoPlayer.Builder(this, factory).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
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
