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
        val now = clockNs()
        val sec = (now / 1_000_000L) / 1000f
        // Small irregular mass of clay: a modest core with a few stretched lobes that reach out with the bands and pull back, and a slow
        // wobble so the outline is never a circle. Lobe angles drift slowly. Heights come only from the audio bands and the beat.
        val lobes = LOBES
        val core = r * (0.22f + 0.025f * b)
        fun band(i: Int): Float {
            if (lv == null || lv.isEmpty()) return 0f
            val lo = (i * lv.size) / lobes; val hi = ((i + 1) * lv.size) / lobes
            var m = 0f; var k = lo
            while (k < hi.coerceAtLeast(lo + 1)) { m = maxOf(m, lv[k.coerceIn(0, lv.size - 1)]); k++ }
            return m
        }
        val capL = r * 0.40f
        val ang = DoubleArray(lobes) { i -> 2.0 * PI * i / lobes - PI / 2 + 0.35 * sin(0.23 * sec + i * 1.7) + FERRO_JITTER[i % FERRO_JITTER.size] * 0.25 }
        val len = FloatArray(lobes) { i ->
            val l = band(i)
            // A faint idle length keeps the shape irregular and alive-looking only through slow drift; real reach needs real levels.
            val idle = r * (0.05f + 0.03f * FERRO_JITTER[(i + 3) % FERRO_JITTER.size])
            val live = if (l < 0.02f) 0f else r * 0.62f * l * (1f + 0.6f * b) * (0.7f + 0.3f * FERRO_JITTER[i % FERRO_JITTER.size])
            idle + capL * kotlin.math.tanh(live / capL)
        }
        val clay = Brush.radialGradient(0f to Color(0xFF4A4A4E), 1f to Color(0xFF38383C), center = Offset(c.x - r * 0.06f, c.y - r * 0.08f), radius = r * 0.8f)
        // Chunky clay: the core and each lobe are unions of overlapping circles (thick neck, rounded bulb at the tip), so lobes are fat and
        // blobby with soft joins, not thin points. Lobes bend a little along their length and sway slowly.
        val tipAng = DoubleArray(lobes) { i -> ang[i] + 0.22 * sin(1.1 * sec + i * 2.3) }
        drawCircle(clay, core * (1f + 0.05f * sin(2.0 * sec).toFloat()), c)
        for (k in 0 until 3) { // lumpy core
            val ca = 2.1 * k + 0.4 * sec
            drawCircle(clay, core * 0.72f, Offset((c.x + core * 0.35f * cos(ca)).toFloat(), (c.y + core * 0.35f * sin(ca)).toFloat()))
        }
        for (i in 0 until lobes) {
            val reach = len[i]
            val bulb = core * (0.34f + 0.16f * (reach / capL).coerceIn(0f, 1f)) // longer lobes end in a bigger rounded bulb
            val neck = core * 0.62f
            val steps = (reach / (bulb * 0.45f)).toInt().coerceIn(2, 16)
            for (st in 0..steps) {
                val t = st.toFloat() / steps
                val dist = core * 0.5f + t * (reach + core * 0.2f)
                val a2 = ang[i] + (tipAng[i] - ang[i]) * t
                val rad = neck + (bulb - neck) * (t * t) // stays thick in the middle, rounded at the end
                drawCircle(clay, rad, Offset((c.x + dist * cos(a2)).toFloat(), (c.y + dist * sin(a2)).toFloat()))
            }
        }
        // Drops: on a beat a drop pinches off the longest lobe tip and flies out into the empty space around the mass, then merges back.
        var loud = 0; var loudL = 0f
        for (i in 0 until lobes) if (len[i] > loudL) { loudL = len[i]; loud = i }
        if (loudL > r * 0.16f) {
            val picks = ArrayList<Int>(); picks.add(loud)
            for (k in (0 until lobes).sortedByDescending { len[it] }) {
                if (picks.size >= 3) break
                if (k in picks || len[k] < loudL * 0.6f || len[k] < r * 0.14f) continue
                picks.add(k)
            }
            drops.onBeat(b, now, picks.map { Triple(tipAng[it], len[it] / r, it) })
        }
        drops.draw(this, c, core, r, now, clay)
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
            if (i == 0) live.add(Drop(angle, nowNs, 0.12f + 0.14f * st, 0.06f + 0.035f * st, strength))
            else if (beat > 0.55f) live.add(Drop(angle, nowNs + i * 55_000_000L, 0.09f + 0.11f * st, 0.04f + 0.03f * st, strength))
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

private const val LOBES = 5
private val FERRO_JITTER = floatArrayOf(0.2f, 0.9f, 0.5f, 1f, 0.1f, 0.7f, 0.4f, 0.85f, 0.3f, 0.6f, 0.95f, 0.15f, 0.55f)
