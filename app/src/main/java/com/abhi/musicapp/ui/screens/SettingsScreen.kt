package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.components.ScreenTitle
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.Orangs
import com.abhi.musicapp.ui.theme.SurfaceSoft

/**
 * Settings with the 7-category structure mapped from the reference analysis:
 * Library / Playback / Sound / Appearance / Storage & background / Permissions / About.
 * Rows are stubs until each feature lands; "Planned" marks designed-but-not-built rows.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onRowClick: (String) -> Unit = {}) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back",
                tint = InkBlack, modifier = Modifier.clickable(onClick = onBack))
        }
        ScreenTitle("Settings")

        SettingsCategory("Library", listOf(
            "Scan device" to "Rescan folders for new audio",
            "Hidden folders" to "Manage folders hidden from the library",
            "Recycle bin" to "Removed songs",
        ), onRowClick = onRowClick)
        SettingsCategory("Playback", listOf(
            "Gapless playback" to "Planned",
            "Crossfade" to "Planned",
            "Play together with other apps" to "Planned",
            "Sleep timer" to "Off",
        ), onRowClick = onRowClick)
        SettingsCategory("Sound", listOf(
            "Equaliser" to "On",
            "Bass boost" to "Planned",
        ), onRowClick = onRowClick)
        SettingsCategory("Appearance", listOf(
            "Theme" to "Light",
            "Accent color" to "Orangs",
            "Language" to "English",
        ), onRowClick = onRowClick)
        SettingsCategory("Storage & background", listOf(
            "Download location" to "Planned",
            "Download on Wi-Fi only" to "Planned",
            "Google Drive backup" to "Planned - needs sign-in",
        ), onRowClick = onRowClick)
        SettingsCategory("Permissions", listOf(
            "Music and audio" to "Status",
            "Notifications" to "Status",
            "Floating player" to "Planned - system access",
            "Set ringtone" to "Planned - system access",
        ), onRowClick = onRowClick)
        SettingsCategory("About", listOf(
            "Version" to "0.1.0",
            "Terms & privacy" to "Documents not published yet",
        ), onRowClick = onRowClick)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsCategory(
    title: String,
    rows: List<Pair<String, String>>,
    onRowClick: (String) -> Unit = {},
) {
    Text(title, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Orangs)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceSoft),
    ) {
        rows.forEachIndexed { i, (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRowClick(label) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, fontSize = 14.sp, color = InkBlack, modifier = Modifier.weight(1f))
                Text(
                    value,
                    fontSize = 12.sp,
                    color = GrayText,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 150.dp),
                )
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GrayText)
            }
            if (i < rows.lastIndex) Spacer(
                Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 16.dp)
                    .background(com.abhi.musicapp.ui.theme.GrayLight)
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}
