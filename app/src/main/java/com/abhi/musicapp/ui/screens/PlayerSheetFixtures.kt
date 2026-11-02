package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.playback.PlayerUiState
import com.abhi.musicapp.ui.theme.GeistMono
import com.abhi.musicapp.ui.theme.GrayLight
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.Orangs
import com.abhi.musicapp.ui.theme.PureWhite

/**
 * FIXTURES ONLY. Figma Player sub-frames: Add to playlist (+ create new playlist dialog), Speed, Delete song, Lyrics (empty / with lines), Queue.
 * Not wired to the Player: no playlists store, speed control, delete/recycle logic, lyrics source or queue editing exist yet (open decisions).
 * Geometry is estimated from the 50%-zoom canvas view (sheet top y: playlist 558, speed 470, lyrics/queue 440 dp), not Design-panel reads.
 * Photos not shipped (neutral tiles). Delete dialog keeps the Figma copy including its typos ("thi song will be move", "Deleat") as source text.
 * The Figma Delete dialog icon is a brand/graphic mark: not shipped, neutral placeholder.
 */
enum class PlayerSheet { Menu, FocusOpen, DeviceOpen, AddToPlaylist, NewPlaylist, Speed, Delete, LyricsEmpty, Lyrics, Queue }

private val sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
private val SheetBg = Color(0xFFF0F0F0)

@Composable
fun PlayerSheetFixture(kind: PlayerSheet) {
    Box(Modifier.fillMaxSize()) {
        PlayerScreen(onBack = {}, initialOverlay = when (kind) { PlayerSheet.Menu -> 1; PlayerSheet.FocusOpen -> 2; PlayerSheet.DeviceOpen -> 3; else -> 0 }, fixture = PlayerUiState(title = "Malcolm Todd - Earrings", artist = "", isPlaying = true,
            positionMs = 61_000L, durationMs = 192_000L, hasTrack = true))
        val dim = kind == PlayerSheet.NewPlaylist || kind == PlayerSheet.Delete
        if (dim) Box(Modifier.fillMaxSize().background(Color(0x66000000)))
        when (kind) {
            PlayerSheet.Menu, PlayerSheet.FocusOpen, PlayerSheet.DeviceOpen -> Unit // rendered by PlayerScreen itself (real overlay)
            PlayerSheet.NewPlaylist -> Card(338.dp, 170.dp) {
                Text("New playlist", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = InkBlack)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF7F7F7)).border(1.dp, GrayLight, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                    Text("Playlist Name...", fontFamily = GeistMono, fontSize = 12.sp, color = GrayText)
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp)).background(GrayLight), contentAlignment = Alignment.Center) {
                    Text("Create", fontFamily = GeistMono, fontSize = 13.sp, color = InkBlack)
                }
            }
            PlayerSheet.Delete -> Card(298.dp, 252.dp, centered = true) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(InkBlack)) // graphic mark not shipped
                Spacer(Modifier.height(12.dp))
                Text("Delete song ?", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = InkBlack)
                Spacer(Modifier.height(10.dp))
                Text("thi song will be move to \"Recently delete\"\nand will be deleted after 30 days.", fontSize = 11.sp, color = InkBlack, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(6.dp)).border(1.dp, Color(0xFFE5484D), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                    Text("Deleat", fontFamily = GeistMono, fontSize = 12.sp, color = Color(0xFFE5484D))
                }
            }
            PlayerSheet.AddToPlaylist, PlayerSheet.Speed, PlayerSheet.LyricsEmpty, PlayerSheet.Lyrics, PlayerSheet.Queue -> {
                val top = when (kind) { PlayerSheet.AddToPlaylist -> 558; PlayerSheet.Speed -> 470; else -> 440 }
                Column(Modifier.fillMaxWidth().offset(y = top.dp).height((844 - top).dp).clip(sheetShape).background(SheetBg).padding(horizontal = 12.dp)) {
                    Box(Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally).size(width = 28.dp, height = 3.dp).clip(CircleShape).background(GrayLight))
                    Spacer(Modifier.height(16.dp))
                    Text(when (kind) { PlayerSheet.AddToPlaylist -> "Add to playlist"; PlayerSheet.Speed -> "Speed"; PlayerSheet.Queue -> "Queue"; else -> "Lyrics" },
                        fontSize = 17.sp, fontWeight = FontWeight.Medium, color = InkBlack)
                    Spacer(Modifier.height(14.dp))
                    when (kind) {
                        PlayerSheet.AddToPlaylist -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("New PLaylist" to true, "Love songs" to false).forEach { (t, add) ->
                                Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, GrayLight, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(32.dp).clip(CircleShape).background(GrayLight), contentAlignment = Alignment.Center) {
                                        Icon(if (add) Icons.Filled.AddCircleOutline else Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = InkBlack, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column { Text(t, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = InkBlack); Text("0 songs", fontFamily = GeistMono, fontSize = 11.sp, color = GrayText) }
                                }
                            }
                        }
                        PlayerSheet.Speed -> Column(Modifier.padding(start = 6.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            listOf("0.5", "0.75x", "1.0 (Default)", "1.25x", "1.5x", "2.0x").forEach { t ->
                                val sel = t.startsWith("1.0")
                                Row(Modifier.fillMaxWidth().height(40.dp).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(t, fontFamily = GeistMono, fontSize = 11.sp, color = if (sel) Orangs else InkBlack, modifier = Modifier.weight(1f))
                                    if (sel) Icon(Icons.Filled.Check, null, tint = Orangs, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        PlayerSheet.LyricsEmpty -> Box(Modifier.fillMaxWidth().weight(1f)) {
                            Column(Modifier.align(Alignment.TopCenter).padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(GrayLight)) // leaf illustration not shipped
                                Spacer(Modifier.height(8.dp))
                                Text("No lyrics", fontSize = 11.sp, color = InkBlack)
                            }
                            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(32.dp).clip(CircleShape).background(GrayLight), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Search, null, tint = InkBlack, modifier = Modifier.size(16.dp)) }
                                Box(Modifier.size(32.dp).clip(CircleShape).background(InkBlack), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, null, tint = PureWhite, modifier = Modifier.size(18.dp)) }
                            }
                        }
                        PlayerSheet.Lyrics -> Column {
                            // Placeholder text: the Figma sample lines are third-party lyrics, not reproduced here.
                            repeat(7) { i ->
                                Text("Sample lyric line number ${i + 1} goes here", fontSize = 11.sp, color = if (i == 3) Orangs else InkBlack, modifier = Modifier.padding(vertical = 3.dp))
                            }
                        }
                        else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { // Queue
                            listOf("Malcolm Todd - Earrings", "Die With A Smile", "Die With A Smile", "Judas").forEachIndexed { i, t ->
                                Row(Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, GrayLight, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.DragHandle, null, tint = GrayText, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(GrayLight)) // photo not shipped
                                    Spacer(Modifier.width(6.dp))
                                    Text(t, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = InkBlack, modifier = Modifier.weight(1f))
                                    when (i) {
                                        0 -> Box(Modifier.size(14.dp).clip(CircleShape).background(GrayLight), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Check, null, tint = PureWhite, modifier = Modifier.size(9.dp)) }
                                        1 -> Box(Modifier.size(10.dp).clip(CircleShape).background(Orangs)) // glyph approximated
                                        else -> Icon(Icons.Filled.ChevronRight, null, tint = GrayText, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Card(top: Dp, height: Dp, centered: Boolean = false, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier.offset(x = 20.dp, y = top).width(350.dp).height(height).clip(RoundedCornerShape(10.dp)).background(SheetBg).padding(14.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start, content = content,
    )
}
