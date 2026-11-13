package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.components.ScreenTitle
import com.abhi.musicapp.ui.components.SongRow
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack

@Composable
fun FoldersScreen(onBack: () -> Unit, onOpenPlayer: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxSize()) {
        Header("Folders", onBack)
        val context = androidx.compose.ui.platform.LocalContext.current
        var folders by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<Pair<String, List<com.abhi.musicapp.library.Song>>>>(emptyList()) }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            folders = com.abhi.musicapp.library.LibraryRepository.folders(com.abhi.musicapp.library.LibraryRepository.querySongs(context))
        }
        if (folders.isEmpty()) {
            Text(if (!com.abhi.musicapp.library.LibraryRepository.hasAudioPermission(context)) "Music permission is off, so folders can't be read. Enable it in system settings." else "No folders found on this device.",
                color = GrayText, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 24.dp))
        }
        folders.forEach { (name, list) ->
            SongRow(name, "${list.size} songs", trailing = {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GrayText)
            }, onClick = { com.abhi.musicapp.playback.PlayerManager.playAll(list, 0); onOpenPlayer() })
        }
    }
}

@Composable
fun PlaylistDetailScreen(name: String, onBack: () -> Unit, onOpenPlayer: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var songs by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<com.abhi.musicapp.library.Song>>(emptyList()) }
    androidx.compose.runtime.LaunchedEffect(name) {
        val ids = com.abhi.musicapp.library.PlaylistStore(context).load().firstOrNull { it.name == name }?.songIds ?: emptyList()
        val byId = com.abhi.musicapp.library.LibraryRepository.querySongs(context).associateBy { it.id }
        songs = ids.mapNotNull { byId[it] } // songs no longer on the device are skipped, not deleted from the playlist
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Header(name, onBack)
        if (songs.isEmpty()) {
            Text("No playable songs in this playlist. Add songs from the player menu (Add to playlist).",
                color = GrayText, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 24.dp))
        }
        PlaylistSongList(
            songs = songs,
            onPlay = { i -> com.abhi.musicapp.playback.PlayerManager.playAll(songs, i); onOpenPlayer() },
            onMove = { from, to ->
                val moved = com.abhi.musicapp.ui.components.moveItem(songs, from, to)
                songs = moved
                com.abhi.musicapp.library.PlaylistStore(context).setOrder(name, moved.map { it.id })
            },
        )
    }
}

/** Songs in the playlist's own order, opened as a list first (nothing plays until a song is tapped). The right-hand three-line handle drags a song up or down. */
@Composable
fun PlaylistSongList(songs: List<com.abhi.musicapp.library.Song>, onPlay: (Int) -> Unit, onMove: (Int, Int) -> Unit) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        com.abhi.musicapp.ui.components.ReorderColumn(songs, rowHeight = 76.dp, onMove = onMove) { song, i, handle, dragging ->
            SongRow(song.title, song.artist, trailing = {
                Icon(androidx.compose.material.icons.Icons.Filled.DragHandle, contentDescription = "Drag to reorder ${song.title}",
                    tint = if (dragging) com.abhi.musicapp.ui.theme.Orangs else GrayText,
                    modifier = Modifier.size(40.dp).then(handle).padding(8.dp).testTag("handle_$i"))
            }, onClick = { onPlay(i) })
        }
    }
}

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.ArrowBack, contentDescription = "Back",
            tint = InkBlack, modifier = Modifier.clickable(onClick = onBack))
    }
    ScreenTitle(title)
}
