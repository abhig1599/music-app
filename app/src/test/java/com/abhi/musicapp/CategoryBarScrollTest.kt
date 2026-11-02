package com.abhi.musicapp

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.ui.screens.HomeScreen
import com.abhi.musicapp.ui.theme.MusicAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Robolectric gesture test, not a device test: the All/Folders/Playlist/Artists bar must scroll sideways. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], instrumentedPackages = ["androidx.loader.content"], qualifiers = "w390dp-h844dp")
class CategoryBarScrollTest {
    @get:Rule val rule = createComposeRule()

    private fun offset(): Float =
        rule.onNodeWithTag("categoryBar").fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].value()

    @Test fun categoryBarSwipesToRevealArtists() {
        rule.setContent { MusicAppTheme { HomeScreen({}, {}, {}, {}, {}, {}) } }
        rule.waitForIdle()
        assertEquals(0f, offset(), 0f)
        rule.onNodeWithTag("categoryBar").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertTrue("bar did not scroll", offset() > 0f)
        rule.onNodeWithText("Artists").assertExists()
    }
}
