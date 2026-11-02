package com.abhi.musicapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.ui.components.TickDial
import com.abhi.musicapp.ui.theme.MusicAppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Player dial at 0/25/50/100 percent (test render of the dial only, not a phone screenshot). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w800dp-h240dp-xhdpi")
class DialProgressRenderTest {
    @get:Rule val rule = createComposeRule()

    @Test fun dialAtQuarterSteps() {
        rule.setContent {
            MusicAppTheme {
                Row(Modifier.background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
                    listOf(0f, 0.25f, 0.5f, 1f).forEach { p ->
                        Box(Modifier.size(200.dp)) { TickDial(progress = p, modifier = Modifier.size(190.dp), ticks = 48, pointer = true, showProgress = true) {} }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/outputs/roborazzi/player_dial_0_25_50_100.png")
    }
}
