package com.abhi.musicapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.tanh

/**
 * "Water drop on a round speaker" centre (his words, 2026-11-23): when the sound comes on, the water spreads outward in ALL directions
 * and then flows back to the centre. Reference for the behaviour: a water droplet on a subwoofer takes a star-like standing-wave shape
 * whose mode follows the pitch (https://fyfluiddynamics.com/2014/05/vibrating-a-liquid-droplet-produces-some-awesome/).
 * Radius(theta) = resting drop + a bass "breath" in every direction + three low standing-wave modes (3, 5, 8 lobes) driven by low, mid and high bands.
 * Every amplitude is a damped spring, so it swells, overshoots a little and settles back to the round drop. Same colour and a smaller
 * maximum reach than the current clay blob, so the size stays. Preview only until he picks it; not wired into the Player.
 */
class DropletState {
    val x = FloatArray(N)
    private val v = FloatArray(N)
    private var last = Long.MIN_VALUE
    /** Second-order spring toward [target]; ~1.5 Hz response, damping 0.7, so a hit swells and eases back without a hard edge. */
    fun step(target: FloatArray, nowNs: Long): FloatArray {
        if (last == Long.MIN_VALUE) { last = nowNs; return x }
        var dt = ((nowNs - last) / 1e9f).coerceIn(0f, 0.25f)
        if (nowNs > last) last = nowNs
        while (dt > 0f) {
            val h = minOf(dt, 0.004f); dt -= h
            for (i in 0 until N) { val a = W * W * (target[i] - x[i]) - 2f * Z * W * v[i]; v[i] += a * h; x[i] += v[i] * h }
        }
        return x
    }
    companion object { const val N = 4; const val W = 11f; const val Z = 0.72f }
}

internal fun dropletTargets(lv: FloatArray?, b: Float): FloatArray {
    fun avg(lo: Int, hi: Int): Float {
        if (lv == null || lv.isEmpty()) return 0f
        var s = 0f; var n = 0
        for (k in lo until hi) { s += lv[k.coerceIn(0, lv.size - 1)]; n++ }
        return if (n == 0) 0f else s / n
    }
    fun knee(l: Float) = l * l / (l + 0.12f) // soft knee, no hard gate
    return floatArrayOf(
        0.095f * tanh(1.8f * knee(avg(0, 9))) + 0.02f * b,
        0.05f * tanh(2f * knee(avg(8, 20))),
        0.04f * tanh(2f * knee(avg(20, 33))),
        0.03f * tanh(2f * knee(avg(33, 48))),
    )
}

@Composable
fun DropletBlob(levels: (() -> FloatArray?)?, tick: State<Long>?, beat: (() -> Float)? = null, clockNs: () -> Long = { clayClockOverride?.invoke() ?: System.nanoTime() }) {
    val st = remember { DropletState() }
    Canvas(Modifier.fillMaxSize()) {
        tick?.value
        val lv = levels?.invoke()
        val b = (beat?.invoke() ?: 0f).coerceIn(0f, 1f)
        val now = clockNs()
        val sec = (now / 1_000_000L) / 1000f
        val x = st.step(dropletTargets(lv, b), now)
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        val modes = intArrayOf(3, 5, 8)
        val path = Path()
        val steps = 200
        for (j in 0 until steps) {
            val th = 2.0 * PI * j / steps
            var rad = 0.21f + x[0]
            for (k in 0 until 3) rad += x[k + 1] * cos(modes[k] * th + (if (k % 2 == 0) 1 else -1) * (0.5 + 0.25 * k) * sec).toFloat()
            rad = rad.coerceIn(0.16f, 0.5f) * r
            val p = Offset((c.x + rad * cos(th)).toFloat(), (c.y + rad * kotlin.math.sin(th)).toFloat())
            if (j == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        val clay = Brush.radialGradient(0f to Color(0xFF4A4A4E), 1f to Color(0xFF38383C), center = Offset(c.x - r * 0.06f, c.y - r * 0.08f), radius = r * 0.8f)
        drawPath(path, clay)
    }
}
