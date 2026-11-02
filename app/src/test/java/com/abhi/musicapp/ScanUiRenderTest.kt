package com.abhi.musicapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.ui.FitDesignWidth
import com.abhi.musicapp.ui.screens.HomeScreen
import com.abhi.musicapp.ui.screens.ScanRays
import com.abhi.musicapp.ui.theme.MusicAppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Robolectric renders of the scan interaction (test renders, not phone screenshots). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], instrumentedPackages = ["androidx.loader.content"], qualifiers = "w360dp-h800dp-xhdpi")
class ScanUiRenderTest {
    @get:Rule val rule = createComposeRule()

    /** Real Home, no music permission in the test app: tapping the dial folder must show the permission notice. */
    @Test fun tapScanShowsNotice() {
        rule.setContent { FitDesignWidth { MusicAppTheme { Box(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) { HomeScreen({}, {}, {}, {}, {}, {}) } } } }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Scan device").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("scanNotice").assertExists()
        rule.onRoot().captureRoboImage("build/outputs/roborazzi/scan_notice_home.png")
    }

    /** Rays mid-rotation (frames 0 ms and 350 ms) next to the idle dial. */
    @Test fun spinningRays() {
        rule.mainClock.autoAdvance = false
        rule.setContent { MusicAppTheme { Box(Modifier.size(300.dp).background(androidx.compose.material3.MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) { ScanRays(Modifier.size(147.dp), scanning = true) } } }
        rule.mainClock.advanceTimeBy(350)
        rule.onRoot().captureRoboImage("build/outputs/roborazzi/scan_rays_spinning_350ms.png")
    }
}
