package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.theme.GeistMono
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.PureWhite
import com.abhi.musicapp.ui.theme.SurfaceSoft

// Shared page header from Figma (Recycle bin 47-14803, Read Now 59-2982, both 390x844, fill Bg):
// Frame 81 at x22 y41, 351 wide, vertical gap 24: back circle 44, title row 351 x 37 (gap 24), content.
@Composable
internal fun FigmaPage(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}, titleSize: androidx.compose.ui.unit.TextUnit = 28.sp, chevronBack: Boolean = false, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 22.dp, end = 17.dp, top = 41.dp)) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(if (chevronBack) androidx.compose.ui.graphics.Color(0xFFF0F0F0) else PureWhite).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            if (chevronBack) {
            androidx.compose.foundation.Canvas(Modifier.size(width = 6.dp, height = 12.dp)) { // Figma Download Detail: chevron vector 6x12, 2px stroke, Gray (hex inferred)
                val st = 2.dp.toPx()
                val col = androidx.compose.ui.graphics.Color(0xFF6B6B6B)
                drawLine(col, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(0f, size.height / 2), st, androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(col, androidx.compose.ui.geometry.Offset(0f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width, size.height), st, androidx.compose.ui.graphics.StrokeCap.Round)
            }
            } else Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = InkBlack, modifier = Modifier.size(20.dp)) // arrow not verified against Figma on this frame
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().height(37.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = titleSize, fontWeight = FontWeight.Bold, color = InkBlack, modifier = Modifier.weight(1f))
            trailing()
        }
        Spacer(Modifier.height(24.dp))
        content()
        Spacer(Modifier.height(24.dp))
    }
}

data class BinRow(val name: String, val daysLeft: Int)

/**
 * Recycle Bin. List rows: 351x72, white, r10, pad 10, left group 292x50 gap 13 (50x50 thumb + text), undo icon 24x24; list gap 6.
 * Delete semantics are still an open decision (DECISIONS.md), so the real app has no rows; [fixture] rows are design samples.
 */
@Composable
fun RecycleBinScreen(onBack: () -> Unit, fixture: List<BinRow> = emptyList()) {
    FigmaPage("Recycle Bin", onBack, titleSize = 30.sp, chevronBack = true, trailing = {
        // "Empty Bin" pill: size/fill are estimates. Disabled: nothing can be binned yet.
        Box(Modifier.alpha(if (fixture.isEmpty()) 0.5f else 1f).clip(RoundedCornerShape(8.dp)).background(SurfaceSoft).padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text("Empty Bin", fontFamily = GeistMono, fontSize = 13.sp, color = InkBlack)
        }
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            fixture.forEach { row ->
                Row(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(10.dp)).background(com.abhi.musicapp.ui.theme.FigmaWhite).border(1.dp, androidx.compose.ui.graphics.Color(0xFFD9D9D9), RoundedCornerShape(10.dp)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    // Thumb is a photo in the design; photo assets are not shipped, neutral tile instead.
                    Box(Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceSoft))
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(row.name, fontSize = 18.sp, color = InkBlack) // Figma read: Helvetica Neue Regular 18
                        Text("${row.daysLeft} days left", fontFamily = GeistMono, fontSize = 14.sp, /* DERIVED, not panel-verified */ color = InkBlack)
                    }
                    Icon(Icons.Filled.Undo, "Restore", tint = InkBlack, modifier = Modifier.size(24.dp))
                }
            }
            if (fixture.isEmpty()) Text("Nothing in the bin", fontFamily = GeistMono, fontSize = 12.sp, color = GrayText, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/**
 * Read Now (Reader). Cards: white, r16, 351 wide; API card 351x168 (pad 14/16/14/10, gap 13; title row 20, API row 321x46, button 321x48), empty card 351x267.52, gap 24 (Design panel). Button radius/fonts estimated.
 * The Reader is optional/undefined: Add API is disabled, no key is accepted or stored, no AI call exists.
 */
@Composable
fun ReadNowScreen(onBack: () -> Unit, fixtureApiName: String? = null, designComparison: Boolean = false) {
    val mono = GeistMono
    FigmaPage("Read Now", onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            if (fixtureApiName != null) {
                Column(Modifier.fillMaxWidth().height(168.dp).clip(RoundedCornerShape(16.dp)).background(com.abhi.musicapp.ui.theme.FigmaWhite)
                    .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 10.dp)) {
                    Text("API Collection", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = InkBlack, modifier = Modifier.height(20.dp))
                    Spacer(Modifier.height(13.dp))
                    Row(Modifier.height(46.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(fixtureApiName, fontFamily = mono, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = InkBlack) // Geist Mono Medium (size not panel-verified)
                            Text("...hello", fontSize = 14.sp, color = GrayText) // Helvetica Neue Regular 14 Gray (read)
                        }
                        Icon(Icons.Filled.Delete, "Delete", tint = InkBlack, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(13.dp))
                    ReadButton("Add API", dark = false, enabled = false)
                }
            }
            Column(Modifier.fillMaxWidth().height(267.52.dp).clip(RoundedCornerShape(16.dp)).background(com.abhi.musicapp.ui.theme.FigmaWhite).padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 10.dp)) {
                Text("API Collection", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = InkBlack)
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No API added yet", fontSize = 14.sp, color = InkBlack) // Helvetica Neue Regular 14 (read)
                }
                ReadButton(if (designComparison) "Add API" else "Add API (unavailable)", dark = true, enabled = false, designComparison = designComparison)
            }
            // The design carries this explanatory note under the cards; reproduced as drawn.
            if (designComparison) {
                Text("Now I want to add a line that you can only add one API at a time, and if there are some issue with the Read Now feature, you can simply need to delete the API and add a new one to get it running.",
                fontFamily = mono, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GrayText) // Geist Mono Medium 12 Gray (read)
            } else {
                Text("Read Now is not available yet. No API is stored and nothing is sent anywhere.", fontFamily = mono, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GrayText)
            }
        }
    }
}

@Composable
private fun ReadButton(label: String, dark: Boolean, enabled: Boolean = true, designComparison: Boolean = false) {
    Box(Modifier.alpha(if (enabled || (dark && designComparison)) 1f else 0.5f).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp))
        .background(if (dark) InkBlack else com.abhi.musicapp.ui.theme.GrayLight), contentAlignment = Alignment.Center) {
        Text(label, fontFamily = GeistMono, fontSize = 11.sp, color = if (dark) com.abhi.musicapp.ui.theme.FigmaWhite else GrayText)
    }
}
