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
    val speed: Float = 1f,
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
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // Sleep timer "play last one to end": the song finished on its own, stop before the next one is heard.
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && SleepTimer.isFinishingSong) { player().pause(); SleepTimer.onSongEnded() }
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) SleepTimer.onSongEnded()
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
        val ids = (0 until c.mediaItemCount).map { c.getMediaItemAt(it).mediaId }
        val toRemove = upcomingIndicesToRemove(ids, c.currentMediaItemIndex, folderById, hiddenFolders)
        for (i in toRemove) c.removeMediaItem(i)
        val removed = toRemove.size
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
                val target = seekHoldTarget(direction, c.isPlaying, c.currentPosition, c.duration, c.playbackParameters.speed) ?: break
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

    /** Current play queue as (mediaId, title, artist) plus the index being played; empty when nothing is loaded. */
    fun queueSnapshot(): Pair<List<Triple<String, String, String>>, Int> {
        val c = controller ?: return emptyList<Triple<String, String, String>>() to -1
        val items = (0 until c.mediaItemCount).map { val m = c.getMediaItemAt(it); Triple(m.mediaId, m.mediaMetadata.title?.toString() ?: "", m.mediaMetadata.artist?.toString() ?: "") }
        return items to c.currentMediaItemIndex
    }

    /** Moves a queue item; the playing song keeps playing (Media3 moveMediaItem). Only the queue changes, no playlist or file. */
    fun moveQueueItem(from: Int, to: Int) {
        val c = controller ?: return
        if (from !in 0 until c.mediaItemCount || to !in 0 until c.mediaItemCount || from == to) return
        c.moveMediaItem(from, to)
        if (queue.size == c.mediaItemCount) queue = com.abhi.musicapp.ui.components.moveItem(queue, from, to)
        pushState()
    }

    /** Jump to a queue entry and play it. */
    fun playQueueIndex(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        c.seekTo(index, 0L); c.play()
    }

    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()

    fun seekTo(positionMs: Long) = controller?.seekTo(positionMs)

    private fun player(): MediaController = controller!!
    fun pauseNow() { controller?.pause() }

    /** Playback speed (0.5x..2.0x). Media3 keeps pitch by default. */
    fun setSpeed(speed: Float) { controller?.setPlaybackSpeed(speed.coerceIn(0.5f, 2.0f)); pushState() }

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
            speed = c.playbackParameters.speed,
        )
    }
}


/**
 * One 100 ms step of a hold-seek (examples below are at 1.0x; they scale with the playback speed). Playing already advances 100 ms per step: forward adds +100 more (net 2x),
 * backward needs -300 (net -2x). Paused: +-200. Returns null when the hold must stop (end reached or duration unknown),
 * so a hold never skips to another track: the target stays 1 s before the end and at 0.
 */
internal fun seekHoldTarget(direction: Int, playing: Boolean, position: Long, duration: Long, speed: Float = 1f): Long? {
    // Net rate is 2x the current playback speed (identical to before at 1.0x): playing already moves 100*speed per step.
    val s = speed.coerceIn(0.5f, 2.0f)
    val step = Math.round(if (direction > 0) (if (playing) 100f * s else 200f * s) else (if (playing) -300f * s else -200f * s)).toLong()
    val maxPos = if (duration > 1000) duration - 1000 else 0L
    if (direction > 0 && position >= maxPos) return null
    return (position + step).coerceIn(0L, maxPos)
}

/** Queue indices (descending, safe to remove in order) after the current item whose folder is hidden. The current item is never listed. */
internal fun upcomingIndicesToRemove(mediaIds: List<String>, currentIndex: Int, folderById: Map<String, String>, hidden: Set<String>): List<Int> =
    (mediaIds.size - 1 downTo currentIndex + 1).filter { i ->
        val f = folderById[mediaIds[i]]
        f != null && f.isNotEmpty() && f in hidden
    }
