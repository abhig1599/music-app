package com.abhi.musicapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.text.withStyle
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
    val bg = if (active) Orangs else SurfaceSoft
    val fg = if (active) PureWhite else InkBlack
    Row(
        modifier = Modifier
            .height(37.dp)
            .clip(RoundedCornerShape(8.72.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = fg, fontSize = 15.sp, maxLines = 1, softWrap = false, fontFamily = com.abhi.musicapp.ui.theme.GeistMono)
        if (count != null) {
            Spacer(Modifier.width(2.dp))
            Text(count, color = if (active) PureWhite else GrayText, fontSize = 10.sp,
                modifier = Modifier.offset(y = (-6).dp))
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
            .padding(horizontal = 20.dp, vertical = 3.dp) // Figma: rows 351 wide at x20, gap 6
            .clip(RoundedCornerShape(10.dp))
            .background(PureWhite)
            .border(1.dp, Color(0xFFD3D3D1), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GrayLight),
            contentAlignment = Alignment.Center,
        ) {
            // His thumbs are album photos (not shipped); neutral tile stands in.
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Orangs)
        }
        Spacer(Modifier.width(13.dp))
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
    pointer: Boolean = false,
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
            val inner = if (pointer) outer * (108.5f / 150.5f) else outer * 0.72f
            val lit = (ticks * progress.coerceIn(0f, 1f)).toInt()
            for (i in 0 until ticks) {
                val angle = Math.toRadians((i * 360.0 / ticks) - 90.0)
                // pointer = his player dial: thin grey ticks, orange cluster fixed at the top
                // (centre tick full, neighbours fading). Cluster behaviour vs progress is unverified.
                val d = minOf(i, ticks - i)
                val color = if (pointer) {
                    when (d) {
                        0 -> litTick
                        1 -> litTick.copy(alpha = 0.6f)
                        2 -> litTick.copy(alpha = 0.35f)
                        else -> dimTick
                    }
                } else if (i < lit) litTick else dimTick
                val start = Offset(
                    (cx + inner * cos(angle)).toFloat(),
                    (cy + inner * sin(angle)).toFloat(),
                )
                val end = Offset(
                    (cx + outer * cos(angle)).toFloat(),
                    (cy + outer * sin(angle)).toFloat(),
                )
                val s2 = if (pointer && d == 0) Offset(
                    (cx + (inner - outer * 0.08f) * cos(angle)).toFloat(),
                    (cy + (inner - outer * 0.08f) * sin(angle)).toFloat(),
                ) else start
                drawLine(color = color, start = s2, end = end, strokeWidth = if (pointer) outer * (5f / 150.5f) else outer * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
        Box(modifier = Modifier.fillMaxSize(if (pointer) 0.7f else 0.62f), contentAlignment = Alignment.Center) { center() }
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
            .padding(horizontal = 18.dp, vertical = 4.dp) // Figma: x18, 355 wide
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (darkVariant) {
            Column(Modifier.weight(1f)) {
                Text(title, color = fg, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(time, color = GrayText, fontSize = 10.sp)
            }
            val padded = time.padStart(5, '0')
            Text(
                androidx.compose.ui.text.buildAnnotatedString {
                    padded.forEach { ch ->
                        if (ch == ':') withStyle(androidx.compose.ui.text.SpanStyle(color = Orangs)) { append(ch) }
                        else append(ch)
                    }
                },
                color = fg, fontSize = 34.sp,
                fontFamily = com.abhi.musicapp.ui.theme.GeistMono,
            )
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
