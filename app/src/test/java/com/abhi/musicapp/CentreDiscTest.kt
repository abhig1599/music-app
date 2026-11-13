package com.abhi.musicapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.ui.components.CentreDisc
import com.abhi.musicapp.ui.components.frontFacing
import com.abhi.musicapp.ui.components.isFlipSwipe
import com.abhi.musicapp.ui.theme.MusicAppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1200dp-h220dp-xhdpi")
class CentreDiscTest {
    @get:Rule val rule = createComposeRule()

    @Test fun facingAndSwipeLogic() {
        assertTrue(frontFacing(0f)); assertTrue(frontFacing(89f)); assertFalse(frontFacing(90f)); assertFalse(frontFacing(180f))
        assertFalse(frontFacing(269f)); assertTrue(frontFacing(271f)); assertTrue(frontFacing(360f)); assertTrue(frontFacing(-30f))
        assertTrue(isFlipSwipe(40f, 0f, 36f)); assertTrue(isFlipSwipe(-30f, 30f, 36f)); assertFalse(isFlipSwipe(10f, 10f, 36f))
    }

    /** TEST RENDER: the disc at several points of the coin turn (0..180 deg), synthetic levels (not real audio). */
    @Test fun flipFilmstrip() {
        val lv = FloatArray(48) { i -> (0.9f * (1f - i / 60f) * (0.55f + 0.45f * kotlin.math.abs(kotlin.math.sin(i * 1.7f)))).coerceIn(0f, 1f) }
        rule.setContent {
            MusicAppTheme {
                Row(Modifier.background(androidx.compose.material3.MaterialTheme.colorScheme.background).padding(8.dp)) {
                    listOf(0f, 35f, 70f, 90f, 110f, 145f, 180f).forEach { a ->
                        Box(Modifier.padding(4.dp).size(164.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CentreDisc(flipped = false, onFlip = {}, onTap = {}, time = "1:01", total = "03:12",
                                mono = com.abhi.musicapp.ui.theme.GeistMono, levels = { lv }, tick = null, reducedMotion = true,
                                isPlaying = true, modifier = Modifier.size(164.dp), fixtureRotation = a)
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/outputs/roborazzi/centre_disc_flip_filmstrip.png")
    }
}
