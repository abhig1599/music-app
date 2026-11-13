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
            Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) { ClayBlob(levels, tick) }
        }
    }
}

/** Metallic clay-like blob: its outline is pushed outward by the song's frequency bands (low at the top, mirrored). Silent or paused = a calm round form; only the soft highlight drifts. */
@Composable
fun ClayBlob(levels: (() -> FloatArray?)?, tick: State<Long>?) {
    Canvas(Modifier.fillMaxSize()) {
        tick?.value // redraw every frame while live
        val lv = levels?.invoke()
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        val n = 72
        fun lvl(i: Int): Float {
            if (lv == null || lv.isEmpty()) return 0f
            val f = i.toFloat() / n; val pos = (1f - abs(2f * f - 1f)) * (lv.size - 1)
            val a = pos.toInt().coerceIn(0, lv.size - 1); val b = (a + 1).coerceAtMost(lv.size - 1)
            return lv[a] + (lv[b] - lv[a]) * (pos - a)
        }
        val pts = List(n) { i ->
            val ang = 2.0 * PI * i / n - PI / 2
            val rad = r * (0.70f + 0.26f * lvl(i))
            Offset((c.x + rad * cos(ang)).toFloat(), (c.y + rad * sin(ang)).toFloat())
        }
        val path = Path()
        fun mid(a: Offset, b: Offset) = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
        val start = mid(pts[n - 1], pts[0]); path.moveTo(start.x, start.y)
        for (i in 0 until n) { val m = mid(pts[i], pts[(i + 1) % n]); path.quadraticTo(pts[i].x, pts[i].y, m.x, m.y) }
        path.close()
        val a = if (tick != null) (System.nanoTime() / 1e9 * 0.45) else 0.9
        val hl = Offset((c.x + r * 0.32f * cos(a)).toFloat() - r * 0.12f, (c.y + r * 0.32f * sin(a)).toFloat() - r * 0.18f)
        val metal = Brush.radialGradient(
            0f to Color(0xFFFFFFFF), 0.18f to Color(0xFFE9E9EB), 0.42f to Color(0xFF9B9BA0),
            0.68f to Color(0xFF3A3A3F), 1f to Color(0xFF151517), center = hl, radius = r * 1.35f,
        )
        drawPath(path, metal)
        drawPath(path, Color(0x33000000), style = Stroke(width = r * 0.02f))
        // small cool specular glint
        drawCircle(Color(0x66FFFFFF), radius = r * 0.09f, center = Offset(hl.x - r * 0.02f, hl.y - r * 0.02f))
    }
}
