package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val info = remember(tick) { com.abhi.musicapp.playback.EqEngine.info() }
    var enabled by remember { mutableStateOf(com.abhi.musicapp.playback.EqEngine.enabled(ctx)) }
    var preset by remember { mutableIntStateOf(com.abhi.musicapp.playback.EqEngine.preset(ctx)) }
    val levels = remember(info) { List(info?.bands ?: 0) { mutableIntStateOf(com.abhi.musicapp.playback.EqEngine.level(ctx, it)) } }
    androidx.compose.runtime.LaunchedEffect(info) { if (info != null) com.abhi.musicapp.playback.EqEngine.apply(ctx) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = InkBlack, modifier = Modifier.clickable(onClick = onBack))
        }
        ScreenTitle("Equaliser")
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clip(RoundedCornerShape(16.dp)).background(SurfaceSoft).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Enabled", fontSize = 16.sp, color = InkBlack, modifier = Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = { enabled = it; com.abhi.musicapp.playback.EqEngine.setEnabled(ctx, it) },
                enabled = info != null, colors = SwitchDefaults.colors(checkedTrackColor = Orangs))
        }
        if (info == null) {
            Text((com.abhi.musicapp.playback.EqEngine.lastError ?: "No equaliser available.") + " Play a song, then reopen this screen.",
                fontSize = 13.sp, color = GrayText, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            Text("Retry", fontSize = 14.sp, color = Orangs, modifier = Modifier.padding(horizontal = 24.dp).clickable { tick++ })
            return@Column
        }
        LazyRow(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            items(info.presets.size) { i ->
                Text(
                    info.presets[i], color = if (preset == i) PureWhite else InkBlack, fontSize = 14.sp,
                    fontWeight = if (preset == i) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 4.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (preset == i) Orangs else SurfaceSoft)
                        .clickable {
                            com.abhi.musicapp.playback.EqEngine.usePreset(ctx, i); preset = i
                            levels.forEachIndexed { b, st -> st.intValue = com.abhi.musicapp.playback.EqEngine.level(ctx, b) }
                            com.abhi.musicapp.playback.EqEngine.apply(ctx)
                        }.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).clip(RoundedCornerShape(16.dp)).background(SurfaceSoft).padding(16.dp)) {
            info.centreHz.forEachIndexed { i, hz ->
                val label = if (hz >= 1000) "%.1fkHz".format(hz / 1000f) else "${hz}Hz"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontSize = 13.sp, color = GrayText, maxLines = 1, softWrap = false, modifier = Modifier.width(60.dp).padding(end = 8.dp))
                    Slider(
                        value = levels[i].intValue.toFloat(),
                        onValueChange = { levels[i].intValue = it.toInt(); preset = -1; com.abhi.musicapp.playback.EqEngine.setBand(ctx, i, it.toInt()) },
                        valueRange = info.minMb.toFloat()..info.maxMb.toFloat(),
                        enabled = enabled, modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Orangs, activeTrackColor = Orangs),
                    )
                    Text("%.1fdB".format(levels[i].intValue / 100f), fontSize = 13.sp, color = GrayText, maxLines = 1, softWrap = false,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.width(56.dp))
                }
            }
        }
        Text("Bands, ranges and presets come from this phone's own equaliser effect. Not yet checked by ear on hardware.",
            fontSize = 12.sp, color = GrayText, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(16.dp))
    }
}
