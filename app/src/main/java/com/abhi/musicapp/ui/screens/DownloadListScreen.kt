package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.theme.GeistMono
import com.abhi.musicapp.ui.theme.GrayLight
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.Orangs
import com.abhi.musicapp.ui.theme.PureWhite

/**
 * Figma "Download Detail" (59-2079). FIXTURE ONLY: no download engine exists (source/ToS question open), so no route points here.
 * Geometry estimated from the 68% view (rows ~72dp, gap 6); not Design-panel reads. Row states: 0 error "!", 1 progress+pause, 2 done check, 3 X.
 */
private val Bg2Fill = androidx.compose.ui.graphics.Color(0xFFE3E3E1) // Figma token Bg 2

@Composable
fun DownloadListFixture(onBack: () -> Unit = {}) {
    FigmaPage("Downloads", onBack, titleSize = 30.sp, trailing = {
        Text("Pause all", fontFamily = GeistMono, fontSize = 13.sp, color = InkBlack,
            modifier = Modifier.clip(RoundedCornerShape(8.72.dp)).background(Bg2Fill).padding(7.dp))
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("All 20" to true, "Pending 02" to false, "Downloaded 06" to false).forEach { (t, sel) ->
                    Text(t, fontFamily = GeistMono, fontSize = 12.sp, color = if (sel) PureWhite else InkBlack,
                        modifier = Modifier.clip(RoundedCornerShape(8.72.dp)).background(if (sel) Orangs else Bg2Fill).padding(7.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0, 1, 2, 3).forEach { state ->
                    Row(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(10.dp)).background(PureWhite).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(GrayLight)) // photo not shipped
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Rolling in the Deep", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = InkBlack)
                            Text("Playlist Name", fontFamily = GeistMono, fontSize = 10.sp, color = InkBlack)
                        }
                        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                            when (state) {
                                0 -> Box(Modifier.size(24.dp).border(1.5.dp, InkBlack, CircleShape), contentAlignment = Alignment.Center) { Text("!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = InkBlack) }
                                1 -> { CircularProgressIndicator(progress = { 0.6f }, color = Orangs, trackColor = GrayLight, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
                                    Box(Modifier.width(2.dp).height(8.dp).background(InkBlack)) }
                                2 -> Box(Modifier.size(24.dp).clip(CircleShape).background(InkBlack), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Check, null, tint = PureWhite, modifier = Modifier.size(14.dp)) }
                                else -> Icon(Icons.Filled.Close, null, tint = InkBlack, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
