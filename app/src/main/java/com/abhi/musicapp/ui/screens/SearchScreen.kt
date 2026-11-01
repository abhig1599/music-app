package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.theme.GeistMono
import com.abhi.musicapp.ui.theme.GrayLight
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.Orangs
import com.abhi.musicapp.ui.theme.PureWhite
import com.abhi.musicapp.ui.theme.SurfaceSoft

/** Figma frames "Search" / "Search with past records" (47-13667) / "Search with recommendations" (47-13936). Render states only. */
enum class SearchFixture { None, PastRecords, Recommendations }

/**
 * Search per Figma: no back button; header "Search..." bold + "Up to date" pill (same header as Home), field row with orange
 * search button, rows with photo thumb, title, mono subtitle; past records have X + "Remove all"; recommendations have chevrons.
 * Positions/sizes are estimated from the 68% view (not Design-panel reads). The frame shows an Android keyboard (system, not drawn).
 * Real app: no history is stored, so past records only appear in the fixture; results come from the on-device (demo) library.
 */
@Composable
fun SearchScreen(onBack: () -> Unit, fixture: SearchFixture = SearchFixture.None, onOpenPlayer: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var library by remember { mutableStateOf<List<com.abhi.musicapp.library.Song>>(emptyList()) }
    androidx.compose.runtime.LaunchedEffect(Unit) { if (fixture == SearchFixture.None) library = com.abhi.musicapp.library.LibraryRepository.querySongs(context) }
    var typed by remember { mutableStateOf("") }
    val query = if (fixture == SearchFixture.Recommendations) "Hello" else typed
    Column(Modifier.fillMaxSize().padding(start = 26.dp, end = 20.dp)) { // content width 344 (Figma selection badge 344x48 on field row); left 26 and right 20 are derived from the capture, not read
        Spacer(Modifier.height(46.dp))
        Row(Modifier.fillMaxWidth().height(37.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Search...", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = InkBlack)
            Spacer(Modifier.weight(1f))
            Text("Up to date", fontFamily = GeistMono, fontSize = 18.sp, lineHeight = 23.sp, color = InkBlack, // Figma read: Geist Mono Regular 18, chip 121x36.95, pad 6.97, radius 8.72, fill Bg 2
                modifier = Modifier.clip(RoundedCornerShape(8.72.dp)).background(androidx.compose.ui.graphics.Color(0xFFE3E3E1)).padding(6.97.dp))
        }
        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(10.dp)).background(androidx.compose.ui.graphics.Color(0xFFEAEAE8)).border(1.dp, androidx.compose.ui.graphics.Color(0xFFE3E3E1), RoundedCornerShape(10.dp)).padding(start = 12.dp, end = 8.dp), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) Text("Search what you need baby...", fontSize = 18.sp, color = GrayText)
                if (fixture == SearchFixture.None) BasicTextField(typed, { typed = it }, singleLine = true, textStyle = TextStyle(fontSize = 18.sp, color = InkBlack))
                else if (query.isNotEmpty()) Text(query, fontSize = 18.sp, color = GrayText)
            }
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Orangs), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Search, "Search", tint = InkBlack, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val matches = if (typed.isBlank()) emptyList() else library.filter { it.title.contains(typed, ignoreCase = true) || it.artist.contains(typed, ignoreCase = true) }
            val rows: List<Pair<String, String>> = when (fixture) {
                SearchFixture.None -> if (typed.isBlank()) emptyList() else matches.map { it.title to it.artist } // Figma 47-13151: empty list until typing
                else -> listOf("[songs name ]" to "File or folder name", "Love songs" to "10 songs", "Love songs" to "10 songs")
            }
            rows.forEachIndexed { rowIdx, (t, sub) ->
                Row(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(10.dp)).clickable(enabled = fixture == SearchFixture.None) { com.abhi.musicapp.playback.PlayerManager.playAll(matches, rowIdx); onOpenPlayer() }.background(androidx.compose.ui.graphics.Color(0xFFF0F0F0)).border(1.dp, androidx.compose.ui.graphics.Color(0xFFD9D9D9), RoundedCornerShape(10.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(PureWhite)) // Figma read: 50x50 radius 10, Image fill (photo not shipped = non-match)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { // Figma read: text column gap 4
                        Text(t, fontSize = 18.sp, color = InkBlack)
                        Text(sub, fontFamily = GeistMono, fontSize = 14.sp, color = InkBlack) // DERIVED not read: Figma text column hugs 160 wide = 19 chars x 0.6em x 14sp
                    }
                    when (fixture) {
                        SearchFixture.PastRecords -> { val xInk = InkBlack; Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { // Figma: Vector 2px Black stroke; 12x12 size ASSUMED same as Download Detail X
                            androidx.compose.foundation.Canvas(Modifier.size(12.dp)) {
                                val w = 2.dp.toPx()
                                drawLine(xInk, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, size.height), w, androidx.compose.ui.graphics.StrokeCap.Round)
                                drawLine(xInk, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(0f, size.height), w, androidx.compose.ui.graphics.StrokeCap.Round)
                            }
                        } }
                        else -> Icon(Icons.Filled.ChevronRight, null, tint = InkBlack, modifier = Modifier.size(24.dp))
                    }
                }
            }
            if (fixture == SearchFixture.PastRecords) {
                Box(Modifier.fillMaxWidth().height(51.dp), contentAlignment = Alignment.Center) {
                    Text("Remove all", fontSize = 16.sp, color = InkBlack)
                }
            }
        }
    }
}
