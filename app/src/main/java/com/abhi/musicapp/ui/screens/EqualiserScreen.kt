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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.abhi.musicapp.ui.theme.PureWhite
import com.abhi.musicapp.ui.theme.SurfaceSoft

/**
 * Equaliser. UI + local state only for now: band count/presets are OUR spec
 * (device audio-effects support varies); audio engine hookup is a later
 * milestone. See DECISIONS.md.
 */
@Composable
fun EqualiserScreen(onBack: () -> Unit) {
    var enabled by remember { mutableStateOf(true) }
    var preset by remember { mutableIntStateOf(0) }
    val bands = remember { List(5) { mutableIntStateOf(0) } }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back",
                tint = InkBlack, modifier = Modifier.clickable(onClick = onBack))
        }
        ScreenTitle("Equaliser")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceSoft)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Enabled", fontSize = 15.sp, color = InkBlack, modifier = Modifier.weight(1f))
            Switch(
                checked = enabled,
                onCheckedChange = { enabled = it },
                colors = SwitchDefaults.colors(checkedTrackColor = Orangs),
            )
        }

        val presets = listOf("Normal", "Bass", "Rock", "Pop", "Jazz", "Flat")
        LazyRow(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            items(presets.size) { i ->
                Text(
                    presets[i],
                    color = if (preset == i) PureWhite else InkBlack,
                    fontSize = 13.sp,
                    fontWeight = if (preset == i) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (preset == i) Orangs else SurfaceSoft)
                        .clickable { preset = i }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceSoft)
                .padding(16.dp),
        ) {
            val labels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
            labels.forEachIndexed { i, label ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontSize = 12.sp, color = GrayText,
                        modifier = Modifier.padding(end = 8.dp))
                    Slider(
                        value = bands[i].intValue.toFloat(),
                        onValueChange = { bands[i].intValue = it.toInt() },
                        valueRange = -15f..15f,
                        enabled = enabled,
                        colors = SliderDefaults.colors(
                            thumbColor = Orangs,
                            activeTrackColor = Orangs,
                        ),
                    )
                    Text("${bands[i].intValue}dB", fontSize = 12.sp, color = GrayText)
                }
            }
        }

        Text(
            "Band support depends on the device; unavailable outputs show a notice here.",
            fontSize = 11.sp, color = GrayText, modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(16.dp))
    }
}
