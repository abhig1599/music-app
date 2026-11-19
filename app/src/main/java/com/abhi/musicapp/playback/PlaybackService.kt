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
        crossfader = Crossfader(this, player, { buildPlayer() }, { p -> session.player = p; exo = p }, { p -> prefetchNext(p) }).also { it.start() }
    }

    private val tempoPrefetcher = com.abhi.musicapp.analysis.TempoPrefetcher()

    /** Hidden, off by default: analyse the tempo of the NEXT queued local song in the background and cache it. Nothing audible uses it. */
    private fun prefetchNext(p: ExoPlayer) {
        if (!com.abhi.musicapp.analysis.TempoCache.prefetchEnabled(this)) return
        val pm = getSystemService(android.os.PowerManager::class.java)
        if (pm?.isPowerSaveMode == true) return
        val next = p.nextMediaItemIndex
        if (next == androidx.media3.common.C.INDEX_UNSET) return
        val item = p.getMediaItemAt(next)
        val uri = item.localConfiguration?.uri ?: return
        val key = item.mediaId
        tempoPrefetcher.request(key) { cancelled -> com.abhi.musicapp.analysis.TempoCache.analyse(this, uri, key, cancelled) }
    }

    /** Same sink as the default, plus a pass-through tap that feeds the Player dial's frequency levels. Used for every player this service creates. */
    private fun buildPlayer(): ExoPlayer {
        val factory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink =
                DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(AudioLevels.newProcessor()))
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
