package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.components.ScreenTitle
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.Orangs
import com.abhi.musicapp.ui.theme.PureWhite
import com.abhi.musicapp.ui.theme.SurfaceSoft

/**
 * Downloads with HIS existing tab design: All / Pending / Downloaded.
 * The paste-link import is a visible stub: no download source is chosen yet
 * (see DECISIONS.md), so submitting explains that instead of faking a download.
 */
@Composable
fun DownloadsScreen(onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var link by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back",
                tint = InkBlack, modifier = Modifier.clickable(onClick = onBack))
        }
        ScreenTitle("Downloads")

        // Import by link (stub)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceSoft)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Link, contentDescription = null, tint = GrayText,
                modifier = Modifier.padding(start = 8.dp))
            TextField(
                value = link,
                onValueChange = { link = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Paste a song or playlist link", color = GrayText, fontSize = 13.sp) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Orangs,
                ),
            )
            Text(
                "Get",
                color = PureWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Orangs)
                    .clickable {
                        notice = if (link.isBlank()) {
                            "Paste a link first."
                        } else {
                            "Link downloads are not connected to a source yet - this build only plays files already on your phone."
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        if (notice.isNotEmpty()) {
            Text(notice, color = Orangs, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        }

        // His tabs: All / Pending / Downloaded
        Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            listOf("All", "Pending", "Downloaded").forEachIndexed { i, label ->
                val active = tab == i
                Text(
                    label,
                    color = if (active) PureWhite else InkBlack,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) Orangs else SurfaceSoft)
                        .clickable { tab = i }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        // Empty state (no downloads in v1)
        Column(
            modifier = Modifier.fillMaxWidth().padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("No downloads yet", color = InkBlack, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text("Songs you download will show up here.", color = GrayText, fontSize = 12.sp)
        }
    }
}
