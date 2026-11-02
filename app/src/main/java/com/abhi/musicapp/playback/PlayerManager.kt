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
    val mediaId: Long = -1L,
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

    /**
     * The controller lives in this process-wide singleton, so it must be bound with the
     * application context. Binding it with an Activity context ties the service connection to
     * that Activity, which Android force-unbinds when the Activity is destroyed; Media3 then
     * unbinds again and crashes with "Service not registered" (see androidx/media issue 239).
     */
    internal fun controllerContext(context: Context): Context = context.applicationContext

    fun connect(activityOrAppContext: Context) {
        if (controller != null) return
        val context = controllerContext(activityOrAppContext)
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

    /**
     * Visibility: drops UPCOMING queue items whose folder is now hidden. The track playing right now is not
     * touched (it finishes), and nothing is deleted from playlists or the phone. Returns how many were removed.
     */
    fun pruneUpcomingHidden(hiddenFolders: Set<String>): Int {
        val c = controller ?: return 0
        if (hiddenFolders.isEmpty()) return 0
        val folderById = queue.associate { it.id.toString() to it.folder }
        var removed = 0
        for (i in c.mediaItemCount - 1 downTo c.currentMediaItemIndex + 1) {
            val f = folderById[c.getMediaItemAt(i).mediaId]
            if (f != null && f.isNotEmpty() && f in hiddenFolders) { c.removeMediaItem(i); removed++ }
        }
        if (removed > 0) queue = queue.filterIndexed { idx, sg -> !(idx > c.currentMediaItemIndex && sg.folder in hiddenFolders) }
        return removed
    }

    private var seekHoldJob: kotlinx.coroutines.Job? = null

    /**
     * Press-and-hold seek: moves the position at about 2x speed (forward) or 2x rewind (backward) in 100 ms steps.
     * This is seeking, not an audio-rate change, so pitch and speed settings are untouched, and play/pause state
     * is left as it was. It stops at 1 s before the end / at 0 so a hold never skips to another track.
     */
    fun startSeekHold(direction: Int) {
        val c = controller ?: return
        seekHoldJob?.cancel()
        seekHoldJob = scope.launch {
            while (isActive) {
                val dur = c.duration
                val playing = c.isPlaying
                // playing already advances 100 ms per step: forward adds +100 more (net 2x); backward needs -300 (net -2x)
                val step = if (direction > 0) (if (playing) 100L else 200L) else (if (playing) -300L else -200L)
                val maxPos = if (dur > 1000) dur - 1000 else 0L
                val target = (c.currentPosition + step).coerceIn(0L, maxPos)
                if (direction > 0 && c.currentPosition >= maxPos) break
                c.seekTo(target)
                if (direction < 0 && target == 0L) break
                delay(100)
            }
        }
    }

    fun stopSeekHold() { seekHoldJob?.cancel(); seekHoldJob = null }

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
            mediaId = c.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L,
        )
    }
}
