package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.theme.GeistMono
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.PureWhite
import com.abhi.musicapp.ui.theme.SurfaceSoft

/** Figma states of the Download playlist flow (nodes 59-888 / 59-1056 / 59-1147 / list frames). Fixtures render state only. */
enum class DlFixture { None, LinkAdded, Loading, Songs }

/**
 * "Download playlist" page (Figma 59-888): Frame 81 x22 y41 gap 24: back 44, title row 351x37, source chips row 351x63 gap 12,
 * input row 344x48 gap 8 (field + download button). Title spelled "Download Playlist" here; Figma text reads "Downlaod Playlist" (typo).
 * No download source is chosen (DECISIONS.md), so the real app never starts a download or network call: the button stays disabled.
 * Chip and field fills/radii, chip text sizes and the loading/list frames' measurements are estimates (not read from the Design panel).
 */
@Composable
fun DownloadsScreen(onBack: () -> Unit, fixture: DlFixture = DlFixture.None) {
    var link by remember { mutableStateOf(if (fixture == DlFixture.None) "" else "www.youtube/playlist") }
    // Fixtures keep the Figma source spelling "Downlaod Playlist" (source typo); runtime wording is corrected.
    FigmaPage(if (fixture == DlFixture.None) "Download Playlist" else "Downlaod Playlist", onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(Modifier.fillMaxWidth().height(63.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(InkBlack).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Text("Spotify", fontFamily = GeistMono, fontSize = 12.sp, color = PureWhite)
                }
                // YouTube chip: brand asset not shipped; blank circle placeholder (NOT visually matched).
                Box(Modifier.size(44.dp).clip(CircleShape).background(SurfaceSoft))
            }
            Row(Modifier.width(344.dp).height(48.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceSoft).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                    if (link.isEmpty()) Text("Add playlist link...", fontSize = 13.sp, color = GrayText)
                    BasicTextField(link, { link = it }, singleLine = true, textStyle = TextStyle(fontSize = 13.sp, color = InkBlack))
                }
                Box(Modifier.size(44.dp).alpha(0.5f).clip(RoundedCornerShape(10.dp)).background(PureWhite), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Download, "Download (not available yet)", tint = InkBlack, modifier = Modifier.size(20.dp))
                }
            }
            if (fixture == DlFixture.Loading || fixture == DlFixture.Songs) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (fixture == DlFixture.Loading) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(SurfaceSoft))
                        Text("Loading Playlist", fontSize = 12.sp, color = InkBlack)
                        Text("found 10 songs", fontFamily = GeistMono, fontSize = 10.sp, color = InkBlack)
                    } else {
                        Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceSoft), contentAlignment = Alignment.Center) {
                            Text("+ add all Songs", fontFamily = GeistMono, fontSize = 12.sp, color = InkBlack)
                        }
                        listOf("Rolling in the Deep", "Shape of You", "Shape of You", "Stay").forEach { n ->
                            Row(Modifier.fillMaxWidth().height(53.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceSoft).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(PureWhite))
                                Spacer(Modifier.width(10.dp))
                                Column { Text(n, fontSize = 14.sp, color = InkBlack); Text("by", fontFamily = GeistMono, fontSize = 10.sp, color = InkBlack) }
                            }
                        }
                    }
                }
            }
        }
    }
}
