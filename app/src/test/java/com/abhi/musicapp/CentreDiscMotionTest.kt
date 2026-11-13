package com.abhi.musicapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.ui.components.CentreDisc
import com.abhi.musicapp.ui.screens.CENTRE_DISC_DP
import com.abhi.musicapp.ui.screens.DIAL_DP
import com.abhi.musicapp.ui.screens.RING_DRAG_MIN_FRACTION
import com.abhi.musicapp.ui.theme.MusicAppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Drives the real CentreDisc with a real swipe gesture and steps the Compose animation clock 40 ms at a time,
 * capturing each frame (test clock, Robolectric render: not a phone, real audio or real frame pacing).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w220dp-h220dp-xhdpi")
class CentreDiscMotionTest {
    @get:Rule val rule = createComposeRule()

    @Test fun discSitsInsideRingDragZone() {
        assertTrue(CENTRE_DISC_DP / 2f / DIAL_DP < RING_DRAG_MIN_FRACTION)
        // dead band between disc edge and where a seek-drag may start, in dp
        val dead = RING_DRAG_MIN_FRACTION * DIAL_DP - CENTRE_DISC_DP / 2f
        assertEquals(20.3f, dead, 0.2f)
    }

    private fun run(prefix: String, reduced: Boolean, lv: FloatArray?, frames: Int) {
        rule.mainClock.autoAdvance = false
        var flippedNow = false
        rule.setContent {
            MusicAppTheme {
                var flipped by remember { mutableStateOf(false) }
                flippedNow = flipped
                Box(Modifier.background(androidx.compose.material3.MaterialTheme.colorScheme.background).padding(8.dp)) {
                    CentreDisc(flipped = flipped, onFlip = { flipped = !flipped }, onTap = {}, time = "1:01", total = "03:12",
                        mono = com.abhi.musicapp.ui.theme.GeistMono, levels = lv?.let { l -> { l } }, tick = null, reducedMotion = reduced,
                        isPlaying = lv != null, modifier = Modifier.size(164.dp).testTag("disc"))
                }
            }
        }
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithTag("disc").captureRoboImage("build/outputs/roborazzi/${prefix}_00.png")
        rule.onNodeWithTag("disc").performTouchInput { swipeLeft() }
        for (i in 1..frames) {
            rule.mainClock.advanceTimeBy(40)
            rule.onNodeWithTag("disc").captureRoboImage("build/outputs/roborazzi/${prefix}_%02d.png".format(i))
        }
    }

    private val lv = FloatArray(48) { i -> (0.9f * (1f - i / 60f) * (0.55f + 0.45f * kotlin.math.abs(kotlin.math.sin(i * 1.7f)))).coerceIn(0f, 1f) }

    /** Playing, normal motion: swipe, then 16 frames x 40 ms. */
    @Test fun swipeFlipMotion() = run("centre_flip_motion", reduced = false, lv = lv, frames = 16)

    /** Reduced motion: the flip must be instant. */
    @Test fun swipeFlipReducedMotion() = run("centre_flip_reduced", reduced = true, lv = lv, frames = 2)

    /** Paused/silent (no levels): back face is a calm round form. */
    @Test fun pausedBackFace() = run("centre_flip_paused", reduced = true, lv = null, frames = 1)
}
