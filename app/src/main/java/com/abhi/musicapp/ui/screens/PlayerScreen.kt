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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
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
    val circle = com.abhi.musicapp.ui.theme.SurfaceSoft

    val mono = FontFamily.Monospace // Figma: Geist Mono (not bundled until licence check)
    // Absolute layout from exact Design-panel reads (390x844 frame, see DECISIONS.md 02:40).
    Box(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.offset(26.dp, 46.dp).size(344.dp, 37.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Playlist", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = InkBlack,
                modifier = Modifier.weight(1f).clickable(onClick = onBack))
            Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = InkBlack)
        }
        TickDial(progress = progress, modifier = Modifier.offset(48.dp, 135.dp).size(301.dp), ticks = 48, pointer = true) {}
        Column(
            modifier = Modifier.offset(147.dp, 252.dp).size(104.dp, 68.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(formatMs(state.positionMs), fontSize = 35.68.sp, fontFamily = mono, color = InkBlack)
                Text("M", fontSize = 20.sp, color = InkBlack)
            }
            Box(Modifier.size(width = 104.dp, height = 1.dp).background(GrayLight))
            Text(formatTotal(state.durationMs), fontSize = 15.51.sp, fontFamily = mono, color = InkBlack)
        }
        Column(
            modifier = Modifier.offset(23.5.dp, 489.dp).width(344.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("[Playlist/Source]", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = InkBlack)
            Text(
                if (state.hasTrack) state.title else "Nothing playing",
                fontSize = 32.sp, fontWeight = FontWeight.Medium, color = InkBlack,
                maxLines = 1,
            )
        }
        Row(
            modifier = Modifier.offset(89.dp, 659.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(63.dp).clip(CircleShape).background(PureWhite)
                    .clickable { PlayerManager.previous() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = InkBlack) }
            Box(
                modifier = Modifier.size(63.dp).clip(CircleShape)
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
            Box(
                modifier = Modifier.size(63.dp).clip(CircleShape).background(PureWhite)
                    .clickable { PlayerManager.next() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = InkBlack) }
        }
        Box(Modifier.offset(19.dp, 726.dp).size(72.dp).clip(CircleShape).background(circle), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Headset, contentDescription = "Audio output", tint = InkBlack)
        }
        Row(
            modifier = Modifier.offset(154.5.dp, 757.dp).size(79.dp, 24.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = InkBlack)
            Text("Queue", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = InkBlack)
        }
        Box(Modifier.offset(298.dp, 726.dp).size(72.dp).clip(CircleShape).background(circle), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.SelfImprovement, contentDescription = "Focus mode (icon pending his decision)", tint = InkBlack)
        }
    }
}
