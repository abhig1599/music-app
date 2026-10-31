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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.components.TickDial
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.Orangs
import com.abhi.musicapp.ui.theme.PureWhite
import com.abhi.musicapp.ui.theme.SurfaceSoft

/**
 * Player: his circular tick-dial as the progress ring around the artwork.
 * Static UI for the v1 milestone; Media3 wiring lands with the playback layer.
 */
@Composable
fun PlayerScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back",
                tint = InkBlack, modifier = Modifier.clickable(onClick = onBack))
        }

        Spacer(Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TickDial(progress = 0.35f, modifier = Modifier.size(280.dp), ticks = 64) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(InkBlack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null,
                        tint = Orangs, modifier = Modifier.size(64.dp))
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Song name in a line", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = InkBlack)
            Spacer(Modifier.height(4.dp))
            Text("Artist name", fontSize = 14.sp, color = GrayText)
            Spacer(Modifier.height(8.dp))
            Text("01:01 / 03:24", fontSize = 13.sp, color = Orangs, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = GrayText)
            Icon(Icons.Filled.Timer, contentDescription = "Sleep timer", tint = GrayText)
            Icon(Icons.Filled.Repeat, contentDescription = "Repeat", tint = GrayText)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous",
                tint = InkBlack, modifier = Modifier.size(40.dp))
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Orangs),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Pause, contentDescription = "Pause",
                    tint = PureWhite, modifier = Modifier.size(40.dp))
            }
            Icon(Icons.Filled.SkipNext, contentDescription = "Next",
                tint = InkBlack, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}
