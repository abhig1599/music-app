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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.playback.PlayerManager
import com.abhi.musicapp.playback.PlayerUiState
import com.abhi.musicapp.ui.components.TickDial
import com.abhi.musicapp.ui.theme.GrayLight
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.Orangs
import com.abhi.musicapp.ui.theme.PureWhite

internal fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

private fun formatTotal(ms: Long): String {
    val totalSec = if (ms <= 0) 0 else ms / 1000
    return "%02d:%02d".format(totalSec / 60, totalSec % 60)
}

/**
 * Player, measured from his Figma "Sleep concept- app exit in" frame (390x844,
 * captured at 68% zoom, positions divided by 0.678): header centre y~132,
 * dial 296dp centre y~327 with thin ticks and the orange cluster at the top,
 * source label y~537, title y~579, controls y~712 (light 58dp circles around
 * prev/next, 60dp play), bottom row y~785 (72dp circles). Not yet verified
 * pixel-exact. Focus glyph provisional (his pending decision).
 */
@Composable
fun PlayerScreen(onBack: () -> Unit, fixture: PlayerUiState? = null) {
    val live by PlayerManager.state.collectAsState()
    val state = fixture ?: live
    val progress = if (state.durationMs > 0)
        state.positionMs.toFloat() / state.durationMs.toFloat() else 0f
    val circle = GrayLight.copy(alpha = 0.55f)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(115.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Playlist", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = InkBlack,
                modifier = Modifier.weight(1f).clickable(onClick = onBack))
            Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = InkBlack)
        }
        Spacer(Modifier.height(30.dp))

        TickDial(progress = progress, modifier = Modifier.size(296.dp), ticks = 60, pointer = true) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        formatMs(state.positionMs),
                        fontSize = 34.sp, fontFamily = FontFamily.Monospace, color = InkBlack,
                    )
                    Text("M", fontSize = 12.sp, color = InkBlack)
                }
                Box(Modifier.size(width = 56.dp, height = 1.dp).background(GrayLight))
                Text(
                    formatTotal(state.durationMs),
                    fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = InkBlack,
                )
            }
        }

        Spacer(Modifier.height(54.dp))
        Text("[Playlist/Source]", fontSize = 12.sp, color = InkBlack)
        Spacer(Modifier.height(18.dp))
        Text(
            if (state.hasTrack) state.title else "Nothing playing",
            fontSize = 24.sp, fontWeight = FontWeight.Bold, color = InkBlack,
        )
        Spacer(Modifier.height(88.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(58.dp).clip(CircleShape).background(circle)
                    .clickable { PlayerManager.previous() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = InkBlack) }
            Spacer(Modifier.size(width = 6.dp, height = 1.dp))
            Box(
                modifier = Modifier.size(60.dp).clip(CircleShape)
                    .background(if (state.isPlaying) Orangs else InkBlack)
                    .clickable { PlayerManager.togglePlayPause() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = PureWhite, modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.size(width = 6.dp, height = 1.dp))
            Box(
                modifier = Modifier.size(58.dp).clip(CircleShape).background(circle)
                    .clickable { PlayerManager.next() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = InkBlack) }
        }

        Spacer(Modifier.height(7.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(circle), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Headset, contentDescription = "Audio output", tint = InkBlack)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = InkBlack)
                Text("Queue", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = InkBlack,
                    modifier = Modifier.padding(start = 4.dp))
            }
            Box(Modifier.size(72.dp).clip(CircleShape).background(circle), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.SelfImprovement, contentDescription = "Focus mode (icon pending his decision)", tint = InkBlack)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
