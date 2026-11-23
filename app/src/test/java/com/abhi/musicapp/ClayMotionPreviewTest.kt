package com.abhi.musicapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.ui.components.ClayBlob
import com.abhi.musicapp.ui.components.ClayTuning
import com.abhi.musicapp.ui.components.DropletBlob
import com.abhi.musicapp.ui.components.clayClockOverride
import com.abhi.musicapp.ui.theme.MusicAppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * Motion preview of the real ClayBlob (same code the app draws) fed a SYNTHETIC song-like level signal (kick at 2 Hz, mids, hats, a quiet gap),
 * with the Player's own level easing reproduced. Test clock, Robolectric render at 60 virtual fps: NOT a phone, NOT real audio, NOT real frame pacing.
 * Three tunings side by side: CURRENT (shipped), EARLY (reconstruction of the 11-19 early values), SMOOTH (candidate).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w200dp-h200dp-xhdpi")
class ClayMotionPreviewTest {
    @get:Rule val rule = createComposeRule()

    private fun hash(a: Int): Float { var x = a * 374761393 + 668265263; x = (x xor (x ushr 13)) * 1274126177; return ((x xor (x ushr 16)) and 0xFFFF) / 65535f }

    private fun signal(t: Float, step: Int, out: FloatArray): Float {
        val kick = exp(-(t % 0.5f) / 0.12f)
        val gap = if (t in 1.5f..2.5f) 0.15f else 1f
        for (i in out.indices) {
            val base = when {
                i < 10 -> 0.15f + 0.85f * kick
                i < 28 -> 0.4f + 0.25f * sin(2 * PI * t * 0.7 + i).toFloat() + 0.2f * kick
                else -> 0.1f + 0.3f * exp(-(t % 0.25f) / 0.04f)
            }
            out[i] = ((base + 0.16f * (hash(step * 64 + i) - 0.5f)) * gap).coerceIn(0f, 1f)
        }
        return kick * gap
    }

    private fun run(name: String, tuning: ClayTuning, droplet: Boolean = false) {
        rule.mainClock.autoAdvance = false
        val shown = FloatArray(48); val target = FloatArray(48); val beatBox = FloatArray(1)
        val tick = mutableLongStateOf(0L)
        var stepIdx = 0
        clayClockOverride = { stepIdx * 16_666_667L }
        try {
            rule.setContent {
                MusicAppTheme {
                    Box(Modifier.background(androidx.compose.material3.MaterialTheme.colorScheme.background).padding(8.dp)) {
                        Box(Modifier.size(164.dp).testTag("blob")) { if (droplet) DropletBlob(levels = { tick.longValue; shown }, tick = tick, beat = { beatBox[0] }) else ClayBlob(levels = { tick.longValue; shown }, tick = tick, beat = { beatBox[0] }, tuning = tuning) }
                    }
                }
            }
            var maxEnergy = 0f
            for (s in 0 until 240) {
                stepIdx = s
                val tb = signal(s / 60f, s, target)
                // same easing as PlayerScreen: rise fast (0.6), fall 0.88 per frame
                for (i in shown.indices) { val v = target[i]; shown[i] = if (v > shown[i]) shown[i] + (v - shown[i]) * 0.6f else shown[i] * 0.88f }
                beatBox[0] = if (tb > beatBox[0]) tb else beatBox[0] * 0.85f
                tick.longValue++
                rule.mainClock.advanceTimeBy(17)
                maxEnergy = maxOf(maxEnergy, shown.max())
                if (s % 2 == 0) rule.onNodeWithTag("blob").captureRoboImage("build/outputs/roborazzi/clay_motion_${name}_%03d.png".format(s / 2))
            }
            assertTrue(maxEnergy > 0.3f)
        } finally { clayClockOverride = null }
    }

    @Test fun a_current() = run("A_current", ClayTuning.CURRENT)
    @Test fun b_early() = run("B_early", ClayTuning.EARLY)
    @Test fun d_droplet() = run("D_droplet", ClayTuning.CURRENT, droplet = true)
    @Test fun c_smooth() = run("C_smooth", ClayTuning.SMOOTH)
}
