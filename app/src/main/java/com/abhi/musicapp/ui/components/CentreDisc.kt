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
import kotlin.math.exp
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
/** Test-only: pins the blob's clock so renders can step time. Null in the app. */
internal var clayClockOverride: (() -> Long)? = null

@Composable
fun ClayBlob(levels: (() -> FloatArray?)?, tick: State<Long>?, beat: (() -> Float)? = null, clockNs: () -> Long = { clayClockOverride?.invoke() ?: System.nanoTime() }) {
    val drops = remember { ClayDrops() }
    Canvas(Modifier.fillMaxSize()) {
        tick?.value // redraw every frame while live
        val lv = levels?.invoke()
        val b = (beat?.invoke() ?: 0f).coerceIn(0f, 1f)
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        val n = FERRO_SPIKES
        val base = r * (0.46f + 0.07f * b)
        // Spike height per band (low frequencies at the top, mirrored); fixed per-spike variation makes heights uneven like a real cluster, not a time loop.
        fun lvl(i: Int): Float {
            if (lv == null || lv.isEmpty()) return 0f
            val f = i.toFloat() / n; val pos = (1f - abs(2f * f - 1f)) * (lv.size - 1)
            return lv[pos.toInt().coerceIn(0, lv.size - 1)]
        }
        // Liquid outline: radius(theta) = pool + sum of rounded gaussian bumps (one per band). Bumps merge at the base like
        // ferrofluid; tall ones are narrower, short ones broad. Heights come only from the audio bands and beat.
        val path = Path()
        val steps = 240
        val w = 2.0 * PI / n
        val hs = FloatArray(n) { k ->
            val l = lvl(k)
            if (l < 0.015f) 0f else r * (0.46f * l * (1f + 0.7f * b) * (0.7f + 0.3f * FERRO_JITTER[k % FERRO_JITTER.size])).coerceAtMost(0.34f)
        }
        val tips = ArrayList<Offset>(); val bases = ArrayList<Offset>()
        for (j in 0 until steps) {
            val th = 2.0 * PI * j / steps - PI / 2
            var add = 0f
            for (k in 0 until n) {
                if (hs[k] <= 0f) continue
                val ak = w * k - PI / 2
                var d = th - ak
                while (d > PI) d -= 2 * PI
                while (d < -PI) d += 2 * PI
                val sigma = w * (0.95 - 0.2 * (hs[k] / (r * 0.5f)).coerceIn(0f, 1f))
                val x = d / sigma
                if (kotlin.math.abs(x) < 3.0) add += hs[k] * exp(-x * x).toFloat()
            }
            val capH = r * 0.34f
            val rad = base + capH * kotlin.math.tanh(add / capH)
            val pt = Offset((c.x + rad * cos(th)).toFloat(), (c.y + rad * sin(th)).toFloat())
            if (j == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        path.close()
        for (k in 0 until n) if (hs[k] > r * 0.08f) {
            val ak = w * k - PI / 2
            tips.add(Offset((c.x + (base + hs[k]) * cos(ak)).toFloat(), (c.y + (base + hs[k]) * sin(ak)).toFloat()))
            bases.add(Offset((c.x + base * 0.9f * cos(ak)).toFloat(), (c.y + base * 0.9f * sin(ak)).toFloat()))
        }
        // Natural clay: one matte tone, only a faint top-light falloff, no gloss, sheen or hard outline.
        val clay = Brush.radialGradient(0f to Color(0xFF4A4A4E), 1f to Color(0xFF38383C), center = Offset(c.x - r * 0.10f, c.y - r * 0.14f), radius = r * 1.1f)
        drawPath(path, clay)
        // Droplets: on a beat a small drop pinches off the loudest spike, flies out and falls back into the pool (same clay tone, so it merges).
        val now = clockNs()
        var loud = 0; var loudH = 0f
        for (k in 0 until n) if (hs[k] > loudH) { loudH = hs[k]; loud = k }
        if (loudH > r * 0.03f) {
            // Candidates: the loudest spike plus up to two other tall spikes that are not neighbours of one already chosen. Quiet beats give one drop.
            val picks = ArrayList<Int>(); picks.add(loud)
            for (k in (0 until n).sortedByDescending { hs[it] }) {
                if (picks.size >= 3) break
                if (k in picks || hs[k] < loudH * 0.6f || hs[k] < r * 0.06f) continue
                if (picks.any { pk -> val dd = kotlin.math.abs(pk - k); minOf(dd, n - dd) < 2 }) continue
                picks.add(k)
            }
            drops.onBeat(b, now, picks.map { Triple(w * it - PI / 2, hs[it] / r, it) })
        }
        drops.draw(this, c, base, r, now, clay)
    }
}

/** Short-lived drops. Each lives about 0.8 s: out to a peak distance along its angle, then back into the pool, shrinking as it merges. */
class ClayDrops {
    private class Drop(val angle: Double, val bornNs: Long, val reach: Float, val size: Float, val spikeH: Float)
    private val live = ArrayList<Drop>()
    private var armed = true
    private var lastSpawnNs = Long.MIN_VALUE
    fun onBeat(beat: Float, nowNs: Long, cands: List<Triple<Double, Float, Int>>) {
        if (beat < 0.25f) { armed = true; return }
        if (!armed || beat < 0.4f || cands.isEmpty()) return
        if (lastSpawnNs != Long.MIN_VALUE && nowNs - lastSpawnNs < 120_000_000L) return
        armed = false; lastSpawnNs = nowNs
        cands.forEachIndexed { i, (angle, strength, _) ->
            val st = strength.coerceIn(0f, 1f)
            if (i == 0) live.add(Drop(angle, nowNs, 0.22f + 0.22f * st, 0.09f + 0.04f * st, strength))
            else if (beat > 0.55f) live.add(Drop(angle, nowNs + i * 45_000_000L, 0.14f + 0.18f * st, 0.05f + 0.03f * st, strength))
        }
        while (live.size > 6) live.removeAt(0)
    }
    fun draw(scope: androidx.compose.ui.graphics.drawscope.DrawScope, c: Offset, base: Float, r: Float, nowNs: Long, brush: Brush) {
        val it = live.iterator()
        while (it.hasNext()) {
            val d = it.next()
            val life = ((nowNs - d.bornNs) / 800_000_000.0).toFloat()
            if (life >= 1f) { it.remove(); continue }
            if (life < 0f) continue
            // Distance: leaves the pool edge, peaks mid-life, returns inside it (base * 0.8) so it visibly mixes back.
            val out = kotlin.math.sin(PI * life).toFloat()
            val tipR = base + r * d.spikeH // starts at the top of the spike it pinches off from, so it is not hidden inside the pool
            val dist = tipR * (0.97f - 0.17f * life) + r * d.reach * out
            val rad = r * d.size * (1f - 0.55f * life)
            val tip = Offset((c.x + dist * cos(d.angle)).toFloat(), (c.y + dist * sin(d.angle)).toFloat())
            val edge = Offset((c.x + base * 0.9f * cos(d.angle)).toFloat(), (c.y + base * 0.9f * sin(d.angle)).toFloat())
            // Neck: a tapered stalk from the pool edge to the drop. It is wide at the pool, thins as the drop rises and snaps when it separates
            // (out > 0.8), then the drop falls back and a short stalk grows again as it merges (life > 0.7).
            val sep = (1f - out) * (1f - out)
            val neckW = if (out > 0.92f) 0f else rad * (0.12f + 1.2f * sep)
            if (neckW > 0.6f && (kotlin.math.abs(dist - base) > 0.5f)) {
                val nx = (-sin(d.angle)).toFloat(); val ny = cos(d.angle).toFloat()
                val np = Path()
                np.moveTo(edge.x + nx * rad * 1.4f, edge.y + ny * rad * 1.4f)
                np.lineTo(tip.x + nx * neckW * 0.5f, tip.y + ny * neckW * 0.5f)
                np.lineTo(tip.x - nx * neckW * 0.5f, tip.y - ny * neckW * 0.5f)
                np.lineTo(edge.x - nx * rad * 1.4f, edge.y - ny * rad * 1.4f)
                np.close()
                scope.drawPath(np, brush)
            }
            scope.drawCircle(brush, rad, tip)
        }
    }
}

private const val FERRO_SPIKES = 14
private val FERRO_JITTER = floatArrayOf(0.2f, 0.9f, 0.5f, 1f, 0.1f, 0.7f, 0.4f, 0.85f, 0.3f, 0.6f, 0.95f, 0.15f, 0.55f)
