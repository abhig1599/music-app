package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
                color = GrayText, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp))
        }
        folders.forEach { (name, list) ->
            SongRow(name, "${list.size} songs", trailing = {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GrayText)
            }, onClick = { com.abhi.musicapp.playback.PlayerManager.playAll(list, 0); onOpenPlayer() })
        }
    }
}

@Composable
fun PlaylistDetailScreen(name: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Header(name, onBack)
        Text("Songs in this playlist will appear here.",
            color = GrayText, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp))
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
