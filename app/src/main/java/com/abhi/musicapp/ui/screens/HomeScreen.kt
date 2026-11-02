package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import com.abhi.musicapp.ui.pressScale
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
    fixture: com.abhi.musicapp.playback.PlayerUiState? = null,
    /** Render-only: Figma 'Home page- update avilable' shows an orange header pill. No updater exists, so the app never sets this. */
    updateAvailableFixture: Boolean = false,
    /** Render-only: Figma 'Download add in the home page' draws an orange progress arc around the Download button. No download engine exists. */
    downloadRingFixture: Boolean = false,
) {
    var chip by remember { mutableIntStateOf(0) }
    var showSort by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    val livePlayerState by PlayerManager.state.collectAsState()
    val playerState = fixture ?: livePlayerState
    var reload by remember { mutableIntStateOf(0) }
    androidx.compose.runtime.DisposableEffect(context) {
        val owner = context as? androidx.lifecycle.LifecycleOwner
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) reload++ }
        owner?.lifecycle?.addObserver(obs)
        onDispose { owner?.lifecycle?.removeObserver(obs) }
    }
    val audioGranted = remember(reload) { LibraryRepository.hasAudioPermission(context) }
    var scanning by remember { mutableStateOf(false) }
    var scanNotice by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(scanNotice) { if (scanNotice != null) { kotlinx.coroutines.delay(3500); scanNotice = null } }
    LaunchedEffect(reload) {
        songs = LibraryRepository.querySongs(context)
    }

    Box(Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Measured from his Figma home frame (390x844 dp, 68%-zoom crop rescaled 1:1):
        // header centre y~65, mosaic y 110-272 / 289-450, chips y 471-507, list from y 528.
        val blockBg = com.abhi.musicapp.ui.theme.GrayLight
        val circleBg = com.abhi.musicapp.ui.theme.FigmaWhite // Figma per-use fill style "White" READ on Search circle (63x63 r31.5 pad 20/19); other circles ASSUMED same
        val monoFont = com.abhi.musicapp.ui.theme.GeistMono
        Spacer(Modifier.height(46.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(37.dp).padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Playlist", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = InkBlack)
            Spacer(Modifier.weight(1f))
            Text(
                when { updateAvailableFixture -> "Update available"; fixture != null -> "Up to date"; else -> "Not checked" }, fontFamily = monoFont, fontSize = 14.sp, color = InkBlack, // shipped: no update check exists, so no "Up to date" claim (design fixtures keep Figma text)
                modifier = Modifier
                    .alpha(if (fixture == null && !updateAvailableFixture) 0.5f else 1f)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(if (updateAvailableFixture) com.abhi.musicapp.ui.theme.Orangs else blockBg)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(26.dp))

        Row(Modifier.padding(horizontal = 26.dp).height(162.dp)) {
            Box(
                modifier = Modifier
                    .width(162.dp).fillMaxHeight()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                    .background(blockBg)
                    .padding(12.dp),
            ) {
                Column {
                    Row {
                        CircleTile(Icons.Filled.Search, "Search", onOpenSearch, circleBg, Modifier.size(63.dp))
                        Spacer(Modifier.width(12.dp))
                        CircleTile(Icons.Filled.Sort, "Sort", { showSort = true }, circleBg, Modifier.size(63.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row {
                        CircleTile(Icons.Filled.Settings, "Settings", onOpenSettings, circleBg, Modifier.size(63.dp))
                        Spacer(Modifier.width(12.dp))
                        val ringColor = com.abhi.musicapp.ui.theme.Orangs
                        CircleTile(Icons.Filled.Download, "Downloads", onOpenDownloads, circleBg,
                            Modifier.size(63.dp).then(if (downloadRingFixture) Modifier.drawWithContent {
                                drawContent()
                                val w = 3.dp.toPx()
                                drawArc(ringColor, -90f, 100f, false,
                                    topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
                                    size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                            } else Modifier))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.width(147.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                ScanRays(modifier = Modifier.size(147.dp), scanning = scanning)
                Box(
                    modifier = Modifier.size(60.dp).clip(CircleShape).background(com.abhi.musicapp.ui.theme.GrayLight)
                        .clickable(enabled = !scanning) {
                            scanning = true
                            val before = songs.map { it.id }.toSet()
                            val hadPermission = LibraryRepository.hasAudioPermission(context)
                            var finished = false
                            fun finish() {
                                if (finished) return
                                finished = true
                                scope.launch {
                                    val after = if (hadPermission) withContext(kotlinx.coroutines.Dispatchers.IO) { LibraryRepository.querySongs(context) } else songs
                                    songs = after
                                    scanNotice = LibraryRepository.scanSummary(hadPermission, before, after.map { it.id }.toSet())
                                    scanning = false
                                    reload++
                                }
                            }
                            LibraryRepository.rescan(context) { finish() }
                            scope.launch { kotlinx.coroutines.delay(20000); finish() }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Folder, contentDescription = if (scanning) "Scanning for songs" else "Scan device",
                        tint = InkBlack, modifier = Modifier.size(24.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.padding(horizontal = 26.dp).height(162.dp)) {
            Column(
                modifier = Modifier
                    .width(162.dp).fillMaxHeight()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                    .background(blockBg)
                    .then(if (fixture != null) Modifier.clickable { } else Modifier), // shipped: Read Now is unavailable, no inert click target
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(32.dp))
                Text("Read Doc.", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = InkBlack)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(64.dp)
                        .then(if (fixture == null) Modifier.alpha(0.5f).semantics { disabled() } else Modifier) // shipped: unavailable (no Reader exists)
                        .clip(CircleShape).background(InkBlack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Videocam, contentDescription = if (fixture == null) "Read Now (unavailable)" else "Read Now",
                        tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(17.dp))
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .width(162.dp).fillMaxHeight()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                    .background(blockBg)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // His frame shows two brand-style circular buttons here; brand marks are not bundled,
                // so generic icons stand in until his assets are exported.
                CircleTile(Icons.Filled.ChatBubbleOutline, if (fixture == null) "Lyrics (unavailable)" else "Lyrics", {}, circleBg, Modifier.size(63.dp).then(if (fixture == null) Modifier.alpha(0.5f).semantics { disabled() } else Modifier))
                CircleTile(Icons.Filled.Videocam, if (fixture == null) "Video (unavailable)" else "Video", {}, circleBg, Modifier.size(63.dp).then(if (fixture == null) Modifier.alpha(0.5f).semantics { disabled() } else Modifier))
            }
        }
        Spacer(Modifier.height(16.dp))

        // Filter chips
        val chips = if (fixture != null) listOf("All" to "20", "Folders" to "02", "Playlist" to "06", "Artists" to null) // Figma sample numbers (fixture only)
        else listOf(
            "All" to songs.size.toString().padStart(2, '0'),
            "Folders" to LibraryRepository.folders(songs).size.toString().padStart(2, '0'),
            "Playlist" to remember(reload) { com.abhi.musicapp.library.PlaylistStore(context).load().size }.toString().padStart(2, '0'),
            "Artists" to null, // no artists view exists; chip is visual only
        )
        // Figma Frame 4: one white 475x46 pill (r10, 1px D3D3D1 stroke, pad 6, gap 12) holding the
        // chips; it overflows the 390 frame, so "Artists" is clipped there too (kept as reference).
        // User confirmed (2026-11-03): this bar slides horizontally to reveal further categories.
        Row(
            Modifier.fillMaxWidth().testTag("categoryBar")
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(20.dp))
            Row(
                modifier = Modifier.requiredWidth(475.dp).height(46.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .background(androidx.compose.ui.graphics.Color.White)
                    .border(1.dp, androidx.compose.ui.graphics.Color(0xFFD3D3D1), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                chips.forEachIndexed { i, c -> FilterChip(c.first, c.second, active = chip == i) { chip = i } }
            }
            Spacer(Modifier.width(20.dp))
        }

        Spacer(Modifier.height(16.dp))

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
            // Figma sample rows (DemoData) only in design fixtures (fixture != null). Shipped state shows real data or an honest empty state.
            val designFixture = fixture != null
            val realFolders = LibraryRepository.folders(songs).map { it.first to "${it.second.size} songs" }
            val rows: List<Pair<String, String>> = if (designFixture) when (chip) {
                1 -> DemoData.folders
                2 -> DemoData.playlists
                else -> DemoData.songs
            } else when (chip) {
                1 -> realFolders
                2 -> remember(reload, chip) { com.abhi.musicapp.library.PlaylistStore(context).load().map { it.name to "${it.songIds.size} songs" } }
                else -> emptyList()
            }
            if (rows.isEmpty()) {
                Text(
                    when (chip) {
                        2 -> "No playlists yet. Use Add to playlist in the player menu."
                        else -> if (!audioGranted) "Music permission is off, so your songs can't be read. Tap here to open system settings."
                            else if (chip == 1) "No folders found on this device." else "No songs found on this device."
                    },
                    color = GrayText, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 26.dp).then(
                        if (chip != 2 && !audioGranted) Modifier.clickable { LibraryRepository.openAppSettings(context) } else Modifier),
                )
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(rows.size) { i ->
                    val (title, sub) = rows[i]
                    SongRow(
                        title = title,
                        subtitle = sub,
                        trailing = {
                            if (chip == 0 && playerState.hasTrack && playerState.isPlaying && (playerState.title == title || (designFixture && i == 0))) { // design fixture: Figma song-playing frames show an orange indicator on row 1 (read 20:46); base frame shows chevrons
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

        // Figma base Home (36-12141) has no player bar; it appears only in "Home page - song playing". Show only when a track is loaded.
        androidx.compose.animation.AnimatedVisibility(
            visible = playerState.hasTrack,
            enter = if (com.abhi.musicapp.ui.reducedMotion()) androidx.compose.animation.EnterTransition.None else
                androidx.compose.animation.slideInVertically(androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)) { it / 2 } + androidx.compose.animation.fadeIn(),
        ) {
            MiniPlayer(
                title = playerState.title,
                time = formatMs(playerState.positionMs),
                darkVariant = true,
                onClick = onOpenPlayer,
            )
        }
    }
    scanNotice?.let { msg ->
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = if (playerState.hasTrack) 110.dp else 40.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).background(InkBlack)
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }.testTag("scanNotice"),
        ) { Text(msg, color = androidx.compose.ui.graphics.Color.White, fontSize = 14.sp, fontFamily = com.abhi.musicapp.ui.theme.GeistMono) }
    }
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

/** Circular icon button used inside his grey home blocks. */
@Composable
private fun CircleTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    bg: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.pressScale().clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = InkBlack, modifier = Modifier.size(24.dp))
    }
}

/** His scan dial: 12 thick rays, orange fading cluster at the upper left (decorative, not playback state). */
@Composable
private fun ScanRays(modifier: Modifier = Modifier, scanning: Boolean = false) {
    val spin = if (scanning && !com.abhi.musicapp.ui.reducedMotion()) {
        val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "scan")
        val a by t.animateFloat(0f, 360f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1400, easing = androidx.compose.animation.core.LinearEasing)), label = "scanAngle")
        a
    } else 0f
    val lit = Orangs
    val dim = com.abhi.musicapp.ui.theme.GrayLight
    androidx.compose.foundation.Canvas(modifier = modifier.graphicsLayer { rotationZ = spin }) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outer = size.minDimension / 2f
        val inner = outer * 0.62f
        val alphas = mapOf(9 to 0.45f, 10 to 1f, 11 to 0.8f, 0 to 0.55f, 1 to 0.35f)
        for (i in 0 until 12) {
            val angle = Math.toRadians(i * 30.0 - 90.0)
            val a = alphas[i]
            val color = if (a != null) lit.copy(alpha = a) else dim
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(
                    (cx + inner * Math.cos(angle)).toFloat(), (cy + inner * Math.sin(angle)).toFloat()),
                end = androidx.compose.ui.geometry.Offset(
                    (cx + outer * Math.cos(angle)).toFloat(), (cy + outer * Math.sin(angle)).toFloat()),
                strokeWidth = 8.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
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

/** Sort sheet per Figma node 47-14214: "Audio Sort", six Geist Mono 12 options, selected one orange with sort icon.
 *  Option labels copied verbatim (incl. "Sort Duration"). Sheet pad 20, gap 10, top radius 24 (Frame 43 read).
 *  Row pitch ~39dp is estimated from the 68% view. Selection is visual only: not yet wired to list order. */
@Composable
fun SortSheetContent(onDone: () -> Unit) {
    val options = listOf("Newest", "Most Played", "A to Z", "Artist", "Long Duration", "Sort Duration")
    var selected by remember { mutableIntStateOf(0) }
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 32.dp)) {
        Text("Audio Sort", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = InkBlack)
        Spacer(Modifier.height(14.dp))
        options.forEachIndexed { i, opt ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(39.dp)
                    .clickable { selected = i; onDone() }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    opt, fontFamily = com.abhi.musicapp.ui.theme.GeistMono, fontSize = 12.sp,
                    color = if (selected == i) Orangs else InkBlack.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                )
                if (selected == i) Icon(Icons.Filled.Sort, null, tint = Orangs, modifier = Modifier.size(16.dp))
            }
        }
    }
}
