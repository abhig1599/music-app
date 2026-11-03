package com.abhi.musicapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.ui.components.AppExitPill
import com.abhi.musicapp.ui.components.SleepStrip
import com.abhi.musicapp.ui.screens.SleepTimerSheet
import com.abhi.musicapp.ui.screens.SpeedSheet
import com.abhi.musicapp.ui.theme.MusicAppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Test renders of the sleep timer sheet, speed sheet and top strips (not phone screenshots). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class SleepMenuRenderTest {
    @get:Rule val rule = createComposeRule()

    private fun shot(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        rule.setContent { MusicAppTheme { Box(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) { content() } } }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test fun sleepSheet() = shot("sleep_sheet") { SleepTimerSheet {} }
    @Test fun speedSheet() = shot("speed_sheet") { SpeedSheet {} }
    @Test fun strips() = shot("sleep_strips") {
        Column(Modifier.padding(23.dp, 46.dp)) {
            SleepStrip("1:01", "10:01", orangeCluster = true, orangeIndex = 11)
            Box(Modifier.size(12.dp))
            SleepStrip("10:00", null, timeUp = true)
            Box(Modifier.size(12.dp))
            AppExitPill(7) {}
        }
    }
}
