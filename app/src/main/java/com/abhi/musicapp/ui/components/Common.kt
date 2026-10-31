package com.abhi.musicapp.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.theme.Bg
import com.abhi.musicapp.ui.theme.GrayLight
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.Orangs
import com.abhi.musicapp.ui.theme.PureWhite
import com.abhi.musicapp.ui.theme.SurfaceSoft
import kotlin.math.cos
import kotlin.math.sin

/** Large screen title like his "Playlist" home header. */
@Composable
fun ScreenTitle(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = InkBlack)
        Spacer(Modifier.weight(1f))
        if (trailing != null) Text(trailing, fontSize = 13.sp, color = GrayText)
    }
}

/** White rounded tile with a monoline icon, used in the home 2x2 grid. */
@Composable
fun IconTile(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(PureWhite)
            .clickable(onClick = onClick)
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = InkBlack, modifier = Modifier.size(28.dp))
    }
}

/** Filter chip row: "All 20", "Folders 02", ... active chip is Orangs. */
@Composable
fun FilterChip(label: String, count: String?, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) Orangs else Color.Transparent
    val fg = if (active) PureWhite else InkBlack
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text(count, color = if (active) PureWhite else GrayText, fontSize = 11.sp)
        }
    }
}

/** Song row card: artwork thumb, title, subtitle, trailing slot. */
@Composable
fun SongRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceSoft)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(InkBlack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Orangs)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = InkBlack, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = GrayText, fontSize = 12.sp)
        }
        trailing()
    }
}

/**
 * The signature circular tick-dial: a ring of short rays, the played fraction
 * in Orangs, the rest Gray-light. Used for the home scan dial and, larger,
 * as the player progress ring.
 */
@Composable
fun TickDial(
    progress: Float,
    modifier: Modifier = Modifier,
    ticks: Int = 48,
    center: @Composable () -> Unit = {},
) {
    // Capture theme colors outside the non-composable draw lambda.
    val litTick = Orangs
    val dimTick = GrayLight
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outer = size.minDimension / 2f
            val inner = outer * 0.72f
            val lit = (ticks * progress.coerceIn(0f, 1f)).toInt()
            for (i in 0 until ticks) {
                val angle = Math.toRadians((i * 360.0 / ticks) - 90.0)
                val color = if (i < lit) litTick else dimTick
                val start = Offset(
                    (cx + inner * cos(angle)).toFloat(),
                    (cy + inner * sin(angle)).toFloat(),
                )
                val end = Offset(
                    (cx + outer * cos(angle)).toFloat(),
                    (cy + outer * sin(angle)).toFloat(),
                )
                drawLine(color = color, start = start, end = end, strokeWidth = outer * 0.06f)
            }
        }
        Box(modifier = Modifier.fillMaxSize(0.62f), contentAlignment = Alignment.Center) { center() }
    }
}

/** Mini player bar. variant: true = dark bar with small pause; false = orange "Pause" bar. */
@Composable
fun MiniPlayer(
    title: String,
    time: String,
    darkVariant: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (darkVariant) InkBlack else Orangs
    val fg = if (darkVariant) PureWhite else InkBlack
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (darkVariant) {
            Text(title, color = fg, fontSize = 13.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(time, color = Orangs, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Filled.Pause, contentDescription = "Pause", tint = PureWhite)
        } else {
            Text("Pause", color = fg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(title, color = fg, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(time, color = fg, fontSize = 12.sp)
            }
        }
    }
}

/** Simple labeled section header like "Read Doc.". */
@Composable
fun SectionHeader(text: String) {
    Text(
        text,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = InkBlack,
    )
}
