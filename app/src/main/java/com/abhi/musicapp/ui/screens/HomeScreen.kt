package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.abhi.musicapp.library.LibraryRepository
import com.abhi.musicapp.library.Song
import com.abhi.musicapp.playback.PlayerManager
import com.abhi.musicapp.ui.components.FilterChip
import com.abhi.musicapp.ui.components.IconTile
import com.abhi.musicapp.ui.components.MiniPlayer
import com.abhi.musicapp.ui.components.ScreenTitle
import com.abhi.musicapp.ui.components.SectionHeader
import com.abhi.musicapp.ui.components.SongRow
import com.abhi.musicapp.ui.components.TickDial
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.Orangs

/**
 * Home, matching his Figma "Playlist" home page:
 * header + icon grid + scan dial + Read Doc. row + filter chips + list + mini player.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenPlayer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
) {
    var chip by remember { mutableIntStateOf(0) }
    var showSort by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    val playerState by PlayerManager.state.collectAsState()
    LaunchedEffect(Unit) {
        songs = LibraryRepository.querySongs(context)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitle("Playlist", trailing = "Up to date")

        // Mosaic matching his Figma home frame (2026-10-31): left = 2x2 small
        // icon tiles (Search / Sort / Settings / Downloads), right = tall scan
        // card with the tick-dial; then Read Doc. card + chat/video card.
        Row(Modifier.padding(horizontal = 20.dp).height(172.dp)) {
            Column(Modifier.weight(1f)) {
                Row(Modifier.weight(1f)) {
                    GridTile(Icons.Filled.Search, "Search", onOpenSearch, Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    GridTile(Icons.Filled.Sort, "Sort", { showSort = true }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.weight(1f)) {
                    GridTile(Icons.Filled.Settings, "Settings", onOpenSettings, Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    GridTile(Icons.Filled.Download, "Downloads", onOpenDownloads, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(com.abhi.musicapp.ui.theme.SurfaceSoft)
                    .clickable { },
                contentAlignment = Alignment.Center,
            ) {
                TickDial(progress = 0.7f, modifier = Modifier.size(120.dp)) {
                    Icon(Icons.Filled.Folder, contentDescription = "Scan device",
                        tint = GrayText, modifier = Modifier.size(28.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.padding(horizontal = 20.dp).height(120.dp)) {
            // Read Doc. card (his exact label kept)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(com.abhi.musicapp.ui.theme.SurfaceSoft)
                    .clickable { }
                    .padding(14.dp),
            ) {
                Text("Read Doc.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = InkBlack)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(InkBlack)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Videocam, contentDescription = "Read Now",
                        tint = Orangs, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.width(12.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(com.abhi.musicapp.ui.theme.SurfaceSoft),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "Lyrics",
                    tint = InkBlack, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(20.dp))
                Icon(Icons.Filled.Videocam, contentDescription = "Video",
                    tint = InkBlack, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(12.dp))

        // Filter chips
        val chips = listOf("All" to "20", "Folders" to "02", "Playlist" to "06", "Artists" to null)
        LazyRow(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            items(chips.size) { i ->
                FilterChip(chips[i].first, chips[i].second, active = chip == i) { chip = i }
            }
        }

        // List content follows the active chip. "All" shows the real on-device
        // library (empty -> demo placeholder rows until permission/scan lands).
        if (chip == 0 && songs.isNotEmpty()) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(songs.size) { i ->
                    val song = songs[i]
                    SongRow(
                        title = song.title,
                        subtitle = song.artist,
                        trailing = {
                            if (playerState.title == song.title && playerState.isPlaying) {
                                Icon(Icons.Filled.GraphicEq, contentDescription = "Playing", tint = Orangs)
                            } else {
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GrayText)
                            }
                        },
                        onClick = {
                            PlayerManager.playAll(songs, i)
                            onOpenPlayer()
                        },
                    )
                }
            }
        } else {
            val rows: List<Pair<String, String>> = when (chip) {
                1 -> DemoData.folders
                2 -> DemoData.playlists
                else -> DemoData.songs
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(rows.size) { i ->
                    val (title, sub) = rows[i]
                    SongRow(
                        title = title,
                        subtitle = sub,
                        trailing = {
                            if (chip == 0) {
                                Icon(Icons.Filled.GraphicEq, contentDescription = "Playing", tint = Orangs)
                            } else {
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GrayText)
                            }
                        },
                        onClick = {
                            when (chip) {
                                1 -> onOpenFolders()
                                2 -> onOpenPlaylist(title)
                                else -> onOpenPlayer()
                            }
                        },
                    )
                }
            }
        }

        MiniPlayer(
            title = if (playerState.hasTrack) playerState.title else "Song name in a line...",
            time = formatMs(playerState.positionMs),
            darkVariant = true,
            onClick = onOpenPlayer,
        )
    }

    if (showSort) {
        ModalBottomSheet(
            onDismissRequest = { showSort = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            SortSheetContent(onDone = { showSort = false })
        }
    }
}

/** Small icon-only square tile used in the home mosaic grid. */
@Composable
private fun GridTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(com.abhi.musicapp.ui.theme.SurfaceSoft)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = InkBlack, modifier = Modifier.size(26.dp))
    }
}

/** Sort options bottom sheet (his Sort design; options verified against the Figma sheet). */
@Composable
fun SortSheetContent(onDone: () -> Unit) {
    val options = listOf("Name A to Z", "Name Z to A", "Newest first", "Oldest first", "Longest first", "Shortest first")
    var selected by remember { mutableIntStateOf(0) }
    Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text("Sort", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = InkBlack)
        Spacer(Modifier.height(10.dp))
        options.forEachIndexed { i, opt ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selected = i }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(opt, fontSize = 15.sp, color = InkBlack, modifier = Modifier.weight(1f))
                if (selected == i) Text("✓", color = Orangs, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                .background(Orangs)
                .clickable(onClick = onDone)
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("Done", color = com.abhi.musicapp.ui.theme.PureWhite, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}
