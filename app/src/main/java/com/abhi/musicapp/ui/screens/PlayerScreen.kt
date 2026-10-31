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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.playback.PlayerManager
import com.abhi.musicapp.playback.PlayerUiState
import com.abhi.musicapp.ui.components.TickDial
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.Orangs
import com.abhi.musicapp.ui.theme.PureWhite

internal fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

/**
 * Player, matching his Figma "Sleep concept / player" frame (captured
 * 2026-10-31): tick-dial with the ELAPSED time inside the centre and total
 * below it, [Playlist/Source] label above the title, prev / play / next,
 * then headphones / Queue / focus row. Driven by the real Media3 state;
 * [fixture] renders a deterministic playing track for screenshot tests.
 * Focus glyph is provisional - the focus-mode icon is his pending decision.
 */
@Composable
fun PlayerScreen(onBack: () -> Unit, fixture: PlayerUiState? = null) {
    val live by PlayerManager.state.collectAsState()
    val state = fixture ?: live
    val progress = if (state.durationMs > 0)
        state.positionMs.toFloat() / state.durationMs.toFloat() else 0f

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Playlist", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = InkBlack,
                modifier = Modifier.weight(1f).clickable(onClick = onBack))
            Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = InkBlack)
        }

        Spacer(Modifier.height(24.dp))

        TickDial(progress = progress, modifier = Modifier.size(280.dp), ticks = 64) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        formatMs(state.positionMs),
                        fontSize = 40.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = InkBlack,
                    )
                    Text("M", fontSize = 12.sp, color = GrayText)
                }
                Text(
                    formatMs(state.durationMs),
                    fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = InkBlack,
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "[Playlist/Source]",
            fontSize = 12.sp, color = GrayText,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (state.hasTrack) state.title else "Nothing playing",
            fontSize = 22.sp, fontWeight = FontWeight.Bold, color = InkBlack,
        )


        Spacer(Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous",
                tint = InkBlack,
                modifier = Modifier.size(36.dp).clickable { PlayerManager.previous() })
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Orangs)
                    .clickable { PlayerManager.togglePlayPause() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = PureWhite, modifier = Modifier.size(36.dp),
                )
            }
            Icon(Icons.Filled.SkipNext, contentDescription = "Next",
                tint = InkBlack,
                modifier = Modifier.size(36.dp).clickable { PlayerManager.next() })
        }

        Spacer(Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Headset, contentDescription = "Audio output", tint = GrayText)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = GrayText)
                Text("Queue", fontSize = 13.sp, color = GrayText,
                    modifier = Modifier.padding(start = 4.dp))
            }
            Icon(Icons.Filled.SelfImprovement, contentDescription = "Focus mode (icon pending his decision)", tint = GrayText)
        }
        Spacer(Modifier.height(24.dp))
    }
}
