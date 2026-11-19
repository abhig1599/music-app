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
        // Same sink as the default, plus a pass-through tap that feeds the Player dial's frequency levels.
        val factory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink =
                DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(AudioLevels.newProcessor()))
                    .build()
        }
        val player = ExoPlayer.Builder(this, factory).build()
        exo = player
        crossfader = Crossfader(this, player).also { it.start() }
        mediaSession = MediaSession.Builder(this, player).build()
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
        exo = null
        super.onDestroy()
    }
}
