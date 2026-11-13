package com.abhi.musicapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.GrayLight
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Rotation angle 0..360 -> true when the front (timer) face is the one facing the viewer. */
internal fun frontFacing(rotationDeg: Float): Boolean { val r = ((rotationDeg % 360f) + 360f) % 360f; return r < 90f || r > 270f }

/** A swipe/flick of at least [thresholdPx] in any direction flips the disc. */
internal fun isFlipSwipe(dx: Float, dy: Float, thresholdPx: Float): Boolean = sqrt(dx * dx + dy * dy) >= thresholdPx

/**
 * Two-faced centre disc (his "coin toss"): front = timer (the Figma centre), back = metallic clay-like blob that
 * reacts to the song's frequency bands. Tap = play/pause, swipe or flick = flip. Rotation is a 3D turn about the vertical axis.
 * [fixtureRotation] pins the angle for test renders. Not a copy of the Pinterest reference (its motion was never seen).
 */
@Composable
fun CentreDisc(
    flipped: Boolean,
    onFlip: () -> Unit,
    onTap: () -> Unit,
    time: String,
    total: String,
    mono: androidx.compose.ui.text.font.FontFamily,
    levels: (() -> FloatArray?)?,
    tick: State<Long>?,
    beat: (() -> Float)? = null,
    reducedMotion: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    fixtureRotation: Float? = null,
) {
    val animated by animateFloatAsState(
        if (flipped) 180f else 0f,
        if (reducedMotion) snap<Float>() else tween<Float>(560, easing = FastOutSlowInEasing), label = "coin",
    )
    val rot = fixtureRotation ?: animated
    val front = frontFacing(rot)
    val density = LocalDensity.current
    val thresholdPx = with(density) { 36.dp.toPx() }
    Box(
        modifier
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }
            .pointerInput(Unit) {
                var dx = 0f; var dy = 0f
                detectDragGestures(
                    onDragStart = { dx = 0f; dy = 0f },
                    onDrag = { change, d -> change.consume(); dx += d.x; dy += d.y },
                    onDragEnd = { if (isFlipSwipe(dx, dy, thresholdPx)) onFlip() },
                )
            }
            .semantics {
                onClick(if (isPlaying) "Pause" else "Play") { onTap(); true }
                customActions = listOf(CustomAccessibilityAction(if (flipped) "Show timer" else "Show animation") { onFlip(); true })
            }
            .graphicsLayer { rotationY = rot; cameraDistance = 14f * density.density },
        contentAlignment = Alignment.Center,
    ) {
        if (front) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(time, fontSize = 35.68.sp, fontFamily = mono, color = InkBlack)
                    Text("M", fontSize = 20.sp, color = InkBlack)
                }
                Box(Modifier.size(width = 104.dp, height = 1.dp).background(GrayLight))
                Text(total, fontSize = 16.51.sp, fontFamily = mono, color = InkBlack)
            }
        } else {
            // Back face is mirrored by the 180 deg turn; turn it back so the blob reads normally.
            Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) { ClayBlob(levels, tick, beat) }
        }
    }
}

/**
 * Ferrofluid-style centre (his words: a black liquid in a dish that comes alive and forms spikes with the beat).
 * A glossy black pool whose edge grows sharp spikes: one per frequency band (low at the top, mirrored), taller with
 * louder bands, and the whole pool swells on a detected beat ([beat], 0..1). Silence or pause = a calm round drop, no motion:
 * nothing here runs on a clock, it only follows the audio levels. My reading of his description, not a copy of any video.
 */
@Composable
fun ClayBlob(levels: (() -> FloatArray?)?, tick: State<Long>?, beat: (() -> Float)? = null) {
    Canvas(Modifier.fillMaxSize()) {
        tick?.value // redraw every frame while live
        val lv = levels?.invoke()
        val b = (beat?.invoke() ?: 0f).coerceIn(0f, 1f)
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        val n = FERRO_SPIKES
        val base = r * (0.40f + 0.05f * b)
        // Spike height per band (low frequencies at the top, mirrored); fixed per-spike variation makes heights uneven like a real cluster, not a time loop.
        fun lvl(i: Int): Float {
            if (lv == null || lv.isEmpty()) return 0f
            val f = i.toFloat() / n; val pos = (1f - abs(2f * f - 1f)) * (lv.size - 1)
            return lv[pos.toInt().coerceIn(0, lv.size - 1)]
        }
        val path = Path()
        val w = 2.0 * PI / n
        val tips = ArrayList<Offset>()
        val bases = ArrayList<Offset>()
        for (k in 0 until n) {
            val l = lvl(k)
            val jit = 0.7f + 0.3f * FERRO_JITTER[k % FERRO_JITTER.size]
            val len = if (l < 0.03f) 0f else r * (0.55f * l * (1f + 0.45f * b) * jit).coerceAtMost(0.50f)
            val ang = w * k - PI / 2
            fun pt(a: Double, rad: Float) = Offset((c.x + rad * cos(a)).toFloat(), (c.y + rad * sin(a)).toFloat())
            val bl = pt(ang - w * 0.62, base)
            val br = pt(ang + w * 0.62, base)
            if (k == 0) path.moveTo(bl.x, bl.y) else path.lineTo(bl.x, bl.y)
            if (len > 0f) {
                // concave sides: a liquid cone that stays wide at the pool and narrows to a rounded tip
                val tip = pt(ang, base + len)
                path.quadTo(pt(ang - w * 0.10, base + len * 0.35f).x, pt(ang - w * 0.10, base + len * 0.35f).y, tip.x, tip.y)
                path.quadTo(pt(ang + w * 0.10, base + len * 0.35f).x, pt(ang + w * 0.10, base + len * 0.35f).y, br.x, br.y)
                tips.add(tip); bases.add(pt(ang, base * 0.9f))
            } else path.lineTo(br.x, br.y)
        }
        path.close()
        val ink = Brush.radialGradient(0f to Color(0xFF2E2E33), 0.5f to Color(0xFF0E0E10), 1f to Color(0xFF000000), center = Offset(c.x - r * 0.10f, c.y - r * 0.14f), radius = r * 0.9f)
        drawPath(path, ink)
        drawPath(path, Color(0xFF000000), style = Stroke(width = r * 0.03f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        // wet sheen along each spike, and a highlight on the pool
        for (i in tips.indices) drawLine(Color(0x44FFFFFF), bases[i], Offset(bases[i].x + (tips[i].x - bases[i].x) * 0.9f, bases[i].y + (tips[i].y - bases[i].y) * 0.9f), strokeWidth = r * 0.012f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawCircle(Color(0x55FFFFFF), radius = base * 0.16f, center = Offset(c.x - base * 0.34f, c.y - base * 0.38f))
        drawArc(Color(0x33FFFFFF), startAngle = 200f, sweepAngle = 70f, useCenter = false,
            topLeft = Offset(c.x - base * 0.78f, c.y - base * 0.78f), size = androidx.compose.ui.geometry.Size(base * 1.56f, base * 1.56f), style = Stroke(width = base * 0.05f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

private const val FERRO_SPIKES = 40
private val FERRO_JITTER = floatArrayOf(0.2f, 0.9f, 0.5f, 1f, 0.1f, 0.7f, 0.4f, 0.85f, 0.3f, 0.6f, 0.95f, 0.15f, 0.55f)
