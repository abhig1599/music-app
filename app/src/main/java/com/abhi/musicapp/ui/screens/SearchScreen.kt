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
fun SearchScreen(onBack: () -> Unit, fixture: SearchFixture = SearchFixture.None) {
    var typed by remember { mutableStateOf("") }
    val query = if (fixture == SearchFixture.Recommendations) "Hello" else typed
    Column(Modifier.fillMaxSize().padding(horizontal = 26.dp)) {
        Spacer(Modifier.height(46.dp))
        Row(Modifier.fillMaxWidth().height(37.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Search...", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = InkBlack)
            Spacer(Modifier.weight(1f))
            Text("Up to date", fontFamily = GeistMono, fontSize = 14.sp, color = InkBlack,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(GrayLight).padding(horizontal = 10.dp, vertical = 8.dp))
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
            val rows: List<Pair<String, String>> = when (fixture) {
                SearchFixture.None -> if (typed.isBlank()) emptyList() else DemoData.songs.filter { it.first.contains(typed, ignoreCase = true) } // Figma 47-13151: empty list until typing
                else -> listOf("[songs name ]" to "File or folder name", "Love songs" to "10 songs", "Love songs" to "10 songs")
            }
            rows.forEach { (t, sub) ->
                Row(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(10.dp)).background(PureWhite).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(PureWhite)) // photo thumbs not shipped
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t, fontSize = 18.sp, color = InkBlack)
                        Text(sub, fontFamily = GeistMono, fontSize = 12.sp, color = InkBlack)
                    }
                    when (fixture) {
                        SearchFixture.PastRecords -> Icon(Icons.Filled.Close, null, tint = InkBlack, modifier = Modifier.size(24.dp))
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
