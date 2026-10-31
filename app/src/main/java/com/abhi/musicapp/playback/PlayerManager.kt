package com.abhi.musicapp.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.abhi.musicapp.library.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val hasTrack: Boolean = false,
)

/**
 * App-wide connection to the PlaybackService MediaSession.
 * UI observes [state]; controls call play/pause/skip.
 */
object PlayerManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state

    private var queue: List<Song> = emptyList()

    fun connect(context: Context) {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    pushState()
                }
            })
            scope.launch {
                while (isActive) {
                    pushState()
                    delay(500)
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun playAll(songs: List<Song>, startIndex: Int) {
        queue = songs
        val c = controller ?: return
        c.setMediaItems(
            songs.map {
                MediaItem.Builder()
                    .setUri(it.uri)
                    .setMediaId(it.id.toString())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(it.title)
                            .setArtist(it.artist)
                            .build()
                    )
                    .build()
            },
            startIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0)),
            0L,
        )
        c.prepare()
        c.play()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()

    fun seekTo(positionMs: Long) = controller?.seekTo(positionMs)

    private fun pushState() {
        val c = controller ?: return
        val md = c.currentMediaItem?.mediaMetadata
        _state.value = PlayerUiState(
            title = md?.title?.toString() ?: "",
            artist = md?.artist?.toString() ?: "",
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.coerceAtLeast(0L),
            hasTrack = c.currentMediaItem != null,
        )
    }
}
