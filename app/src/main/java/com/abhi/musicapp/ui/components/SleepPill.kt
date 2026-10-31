package com.abhi.musicapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.theme.GeistMono
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.Orangs
import com.abhi.musicapp.ui.theme.PureWhite

private val Stroke = Color(0xFFD3D3D1)   // read from Figma
private val Chip = Color(0xFFE3E3E1)     // Bg 2, read from style definition

@Composable
private fun TimeChip(value: String, unit: String) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(Chip).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(value, fontFamily = GeistMono, fontSize = 15.sp, color = InkBlack)
        Spacer(Modifier.width(3.dp))
        Text(unit, fontFamily = GeistMono, fontSize = 11.sp, color = Color(0xFF6B6B6B))
    }
}

/** Grey ticks; chip sizes, tick pitch and orange cluster are ESTIMATES (not Design-panel reads). */
@Composable
private fun Ticks(modifier: Modifier, orangeAt: Int?) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(24) { i ->
            val o = orangeAt != null && (i == orangeAt || i == orangeAt + 1)
            Box(Modifier.width(1.dp).height(if (o) 24.dp else 20.dp).background(if (o) Orangs else Color(0xFFD3D3D1)))
        }
    }
}

/** Strip: 344 x 46, r10, white, 1px D3D3D1 stroke (read). Variants follow Frame 45. */
@Composable
fun SleepStrip(left: String, right: String?, timeUp: Boolean = false, orangeCluster: Boolean = false, topChip: String? = null) {
    Box(Modifier.width(344.dp)) {
        Row(
            Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(10.dp)).background(PureWhite)
                .border(1.dp, Stroke, RoundedCornerShape(10.dp)).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeChip(left, "M")
            Ticks(Modifier.weight(1f).padding(horizontal = 8.dp), if (orangeCluster) 11 else null)
            if (timeUp) {
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Orangs).padding(horizontal = 8.dp, vertical = 7.dp)) {
                    Text("Time-up", fontFamily = GeistMono, fontSize = 15.sp, color = PureWhite)
                }
            } else if (right != null) TimeChip(right, "M")
        }
        if (topChip != null) {
            Row(
                Modifier.align(Alignment.TopCenter).offset(y = (-8).dp).clip(RoundedCornerShape(8.dp)).background(Orangs)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) { Text("$topChip M", fontFamily = GeistMono, fontSize = 11.sp, color = PureWhite) }
        }
    }
}

/** "App exit in 0:10 S x" pill: 239.5 x 46, r10 (read). */
@Composable
fun AppExitPill() {
    Row(
        Modifier.width(240.dp).height(46.dp).clip(RoundedCornerShape(10.dp)).background(PureWhite)
            .border(1.dp, Stroke, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("App exit in", fontFamily = GeistMono, fontSize = 15.sp, color = InkBlack)
        Spacer(Modifier.width(10.dp))
        TimeChip("0:10", "S")
        Spacer(Modifier.weight(1f))
        Icon(Icons.Filled.Close, null, tint = InkBlack)
    }
}

/** Frame 45 stack: vertical, gap 24 (read). Render fixture only; no timer behaviour. */
@Composable
fun SleepPillStack() {
    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        SleepStrip("1:01", "10:01", orangeCluster = true, topChip = "1:01")
        SleepStrip("1:01", "10:01", orangeCluster = true)
        SleepStrip("1:01", null, timeUp = true)
        AppExitPill()
    }
}
