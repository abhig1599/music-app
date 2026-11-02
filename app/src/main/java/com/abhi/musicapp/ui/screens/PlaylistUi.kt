package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.library.PlaylistStore
import com.abhi.musicapp.ui.theme.GrayLight
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack

/**
 * Shipped (non-fixture) add-to-playlist flow, backed by [PlaylistStore]. Visual style borrows the Figma
 * 'Add to playlist' sheet / 'New playlist' card colours, but the layout is NOT measured from Figma (adaptation).
 * Never deletes audio files.
 */
@Composable
fun AddToPlaylistSheet(songId: Long, onDone: () -> Unit) {
    val context = LocalContext.current
    val store = remember { PlaylistStore(context) }
    val visibleIds = remember { com.abhi.musicapp.library.LibraryRepository.querySongs(context).map { it.id }.toSet() }
    var playlists by remember { mutableStateOf(store.load()) }
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var doneName by remember { mutableStateOf<String?>(null) } // Figma 'Add to playlist - done' confirmation
    Box(Modifier.fillMaxSize().background(Color(0x66000000)).clickable { onDone() }) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color(0xFFF0F0F0)).clickable(enabled = false) {}.padding(12.dp),
        ) {
            Text(if (creating) "New playlist" else "Add to playlist", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = InkBlack)
            Spacer(Modifier.height(12.dp))
            if (songId < 0) {
                Text("No song is loaded.", fontSize = 14.sp, color = GrayText)
            } else if (creating) {
                Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF7F7F7))
                    .border(1.dp, GrayLight, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                    if (name.isEmpty()) Text("Playlist Name...", fontSize = 14.sp, color = GrayText)
                    BasicTextField(name, { name = it; error = null }, singleLine = true, textStyle = TextStyle(fontSize = 14.sp, color = InkBlack))
                }
                error?.let { Text(it, fontSize = 13.sp, color = GrayText, modifier = Modifier.padding(top = 6.dp)) }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp)).background(GrayLight).clickable {
                    if (store.create(name)) { store.addSong(name.trim(), songId); doneName = name.trim() }
                    else error = "Enter a name that isn't used already."
                }, contentAlignment = Alignment.Center) { Text("Create", fontSize = 14.sp, color = InkBlack) }
            } else {
                Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, GrayLight, RoundedCornerShape(8.dp))
                    .clickable { creating = true }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("New playlist", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = InkBlack)
                }
                playlists.forEach { p ->
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, GrayLight, RoundedCornerShape(8.dp))
                        .clickable { store.addSong(p.name, songId); doneName = p.name }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = InkBlack)
                            Text("${p.songIds.count { it in visibleIds }} songs", fontSize = 12.sp, color = GrayText)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    doneName?.let { n ->
        // Figma "Add to playlist - done" (read 20:31, view-only): dimmed player, centred light card with an orange badge graphic
        // (asset NOT shipped), text "Add to the [playlist name]" (Figma wording kept, name filled in), and a "Back" button.
        // Card size/fonts not measured.
        Box(Modifier.fillMaxSize().background(Color(0x66000000)).clickable { onDone() }, contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(horizontal = 24.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFF0F0F0))
                    .clickable(enabled = false) {}.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(48.dp)) // badge graphic not shipped
                Text("Add to the\n$n", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = InkBlack,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp)).background(GrayLight).clickable { onDone() }, contentAlignment = Alignment.Center) {
                    Text("Back", fontSize = 14.sp, color = InkBlack)
                }
            }
        }
    }
}
