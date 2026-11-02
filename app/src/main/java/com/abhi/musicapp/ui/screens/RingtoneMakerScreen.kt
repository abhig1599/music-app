package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.components.ScreenTitle
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.PureWhite
import kotlin.math.sin

/**
 * Ringtone maker: pick a start/end window of the current track and save it as a
 * ringtone. Waveform + trim UI is real; the audio slicing engine lands in a later
 * milestone (marked PLANNED). Writing the system ringtone requires the system
 * write-settings consent, which we deep-link to instead of declaring silently.
 */
@Composable
fun RingtoneMakerScreen(onBack: () -> Unit, onOpenSystemConsent: () -> Unit) {
    var startFrac by remember { mutableStateOf(0.2f) }
    var endFrac by remember { mutableStateOf(0.5f) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable(onClick = onBack))
        }
        ScreenTitle("Ringtone")

        // Waveform placeholder bars (deterministic, not decoded audio yet).
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val bars = 48
                val barW = size.width / bars
                for (i in 0 until bars) {
                    val t = i.toFloat() / bars
                    val inRange = t >= startFrac && t <= endFrac
                    val h = (0.25f + 0.7f * kotlin.math.abs(sin(i * 1.7f))) * size.height
                    drawLine(
                        color = if (inRange) Color(0xFFFF6603) else Color(0xFFB9B9B9),
                        start = Offset(i * barW + barW / 2, (size.height - h) / 2),
                        end = Offset(i * barW + barW / 2, (size.height + h) / 2),
                        strokeWidth = barW * 0.5f,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        TrimSlider("Start", startFrac) { v -> if (v < endFrac - 0.05f) startFrac = v }
        TrimSlider("End", endFrac) { v -> if (v > startFrac + 0.05f) endFrac = v }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Preview", tint = PureWhite)
            }
            Box(
                modifier = Modifier
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onOpenSystemConsent)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Save + set as ringtone", fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground)
            }
        }
        Text(
            "Audio trimming is PLANNED - preview and save activate when the slicing engine lands. Setting the ringtone asks the system for write-settings consent.",
            fontSize = 12.sp, color = GrayText, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun TrimSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, color = GrayText, modifier = Modifier.padding(end = 12.dp))
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.weight(1f),
        )
    }
}
