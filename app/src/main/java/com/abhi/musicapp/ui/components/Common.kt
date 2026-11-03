package com.abhi.musicapp.ui.components

import com.abhi.musicapp.ui.pressScale
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
        if (trailing != null) Text(trailing, fontSize = 14.sp, color = GrayText)
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
        Text(label, color = fg, fontSize = 16.sp, maxLines = 1, softWrap = false, fontFamily = com.abhi.musicapp.ui.theme.GeistMono)
        if (count != null) {
            Spacer(Modifier.width(2.dp))
            Text(count, color = if (active) PureWhite else GrayText, fontSize = 11.sp,
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
            .background(com.abhi.musicapp.ui.theme.FigmaWhite)
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
            Text(title, color = InkBlack, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = GrayText, fontSize = 13.sp)
        }
        trailing()
    }
}

/**
 * The signature circular tick-dial: a ring of short rays, the played fraction
 * in Orangs, the rest Gray-light. Used for the home scan dial and, larger,
 * as the player progress ring.
 */
/**
 * Figma Player dial: 48 strokes at irregular angles (degrees clockwise from the top), measured from the
 * design at 145% zoom (centre fitted, +-0.4 deg). Anchors sit on multiples of 15; the stroke after each
 * anchor sits 5.6..7.6 deg later, so gaps alternate between about 5.6 and 9.4 deg (mean 7.5).
 */
internal val FIGMA_DIAL_ANGLES: DoubleArray = run {
    val mid = doubleArrayOf(6.0, 22.6, 36.4, 52.4, 66.8, 80.6, 97.2, 111.2, 127.6, 141.4, 157.2, 172.0,
        185.8, 202.6, 216.2, 232.4, 246.8, 260.6, 277.4, 291.2, 307.6, 321.6, 337.4, 352.2)
    DoubleArray(48) { if (it % 2 == 0) (it / 2) * 15.0 else mid[it / 2] }
}

@Composable
fun TickDial(
    progress: Float,
    modifier: Modifier = Modifier,
    ticks: Int = 48,
    pointer: Boolean = false,
    /** Player dial: ticks up to the played fraction are solid orange, the next two fade to light orange, the rest grey. false keeps the fixed Figma cluster. */
    showProgress: Boolean = false,
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
            val lit = if (pointer && ticks == FIGMA_DIAL_ANGLES.size) FIGMA_DIAL_ANGLES.count { it < progress.coerceIn(0f, 1f) * 360.0 - 1e-6 } else (ticks * progress.coerceIn(0f, 1f)).toInt()
            for (i in 0 until ticks) {
                val deg = if (pointer && ticks == FIGMA_DIAL_ANGLES.size) FIGMA_DIAL_ANGLES[i] else i * 360.0 / ticks
                val angle = Math.toRadians(deg - 90.0)
                // pointer = his player dial: thin grey ticks, orange cluster fixed at the top
                // (centre tick full, neighbours fading). Cluster behaviour vs progress is unverified.
                val d = minOf(i, ticks - i)
                val ahead = i - lit
                val color = if (pointer && showProgress) {
                    when { progress <= 0f -> dimTick; i < lit -> litTick; ahead == 0 -> litTick.copy(alpha = 0.6f); ahead == 1 -> litTick.copy(alpha = 0.35f); else -> dimTick }
                } else if (pointer) {
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
                // Figma: head tick inner r~92 (vs 108.5), its two neighbours ~102.
                val grow = when {
                    !pointer -> 0f
                    showProgress && progress > 0f -> when { i == lit - 1 -> 0.11f; i == lit - 2 || ahead == 0 -> 0.043f; else -> 0f }
                    showProgress -> 0f
                    d == 0 -> 0.11f
                    d == 1 -> 0.043f
                    else -> 0f
                }
                val s2 = if (grow > 0f) Offset(
                    (cx + (inner - outer * grow) * cos(angle)).toFloat(),
                    (cy + (inner - outer * grow) * sin(angle)).toFloat(),
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
            .pressScale(0.97f)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (darkVariant) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(title, color = fg, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(time.padStart(5, '0'), color = GrayText, fontSize = 11.sp)
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
                Text(title, color = fg, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(time, color = fg, fontSize = 13.sp)
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
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = InkBlack,
    )
}
