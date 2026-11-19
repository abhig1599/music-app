package com.abhi.musicapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.ui.screens.DownloadsScreen
import com.abhi.musicapp.ui.screens.EqualiserScreen
import com.abhi.musicapp.ui.screens.HomeScreen
import com.abhi.musicapp.ui.screens.OnboardingScreen
import com.abhi.musicapp.ui.screens.PlayerScreen
import com.abhi.musicapp.ui.screens.SearchScreen
import com.abhi.musicapp.ui.screens.SettingsScreen
import com.abhi.musicapp.ui.screens.RecycleBinScreen
import com.abhi.musicapp.ui.screens.ReadNowScreen
import com.abhi.musicapp.ui.screens.ThemesScreen
import com.abhi.musicapp.ui.theme.ThemeState
import com.abhi.musicapp.ui.theme.MusicAppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders each screen to a real PNG (Robolectric native graphics) so layout
 * can be pixel-compared against the Figma design. Output: build/outputs/roborazzi.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], instrumentedPackages = ["androidx.loader.content"], qualifiers = "w390dp-h844dp")
class ScreenShotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun shot(name: String, heightDp: Int = 844, content: @androidx.compose.runtime.Composable () -> Unit) {
        // 390x844 dp canvas (his Figma frame) via the qualifiers on the class;
        // paint the theme background so dark mode renders truthfully.
        if (heightDp != 844) org.robolectric.RuntimeEnvironment.setQualifiers("w390dp-h${heightDp}dp")
        composeRule.setContent {
            MusicAppTheme {
                Box(
                    modifier = Modifier
                        .size(390.dp, heightDp.dp)
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                ) { content() }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    private fun shotWide(name: String, w: Int = 844, h: Int = 390, content: @androidx.compose.runtime.Composable () -> Unit) {
        org.robolectric.RuntimeEnvironment.setQualifiers("w${w}dp-h${h}dp")
        composeRule.setContent {
            MusicAppTheme {
                Box(Modifier.size(w.dp, h.dp).background(androidx.compose.material3.MaterialTheme.colorScheme.background)) { content() }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    private fun landscapeState() = com.abhi.musicapp.playback.PlayerUiState(title = "Malcolm Todd - Earrings", artist = "", isPlaying = true, positionMs = 61_000L, durationMs = 192_000L, hasTrack = true)

    // TEST RENDERS (software, synthetic levels): landscape 844x390 split, timer face and ferrofluid face.
    @Test fun landscapeTimer() = shotWide("landscape_timer") { PlayerScreen(onBack = {}, fixture = landscapeState(), progressInFixture = true) }
    @Test fun landscapeFerro() = shotWide("landscape_ferrofluid") {
        val lv = FloatArray(48) { i -> (0.9f * (1f - i / 60f) * (0.55f + 0.45f * kotlin.math.abs(kotlin.math.sin(i * 1.7f)))).coerceIn(0f, 1f) }
        PlayerScreen(onBack = {}, fixture = landscapeState(), progressInFixture = true, fixtureLevels = lv, fixtureFlipped = true, fixtureRotation = 180f, fixtureBeat = 0.8f)
    }

    @Test fun landscapeMenu() = shotWide("landscape_menu") { PlayerScreen(onBack = {}, fixture = landscapeState(), progressInFixture = true, initialOverlay = 1) }
    @Test fun landscapeFocus() = shotWide("landscape_focus") { PlayerScreen(onBack = {}, fixture = landscapeState(), progressInFixture = true, initialOverlay = 2) }
    @Test fun landscapeLongTitleCompact() = shotWide("landscape_longtitle_compact", 700, 320) {
        PlayerScreen(onBack = {}, fixture = landscapeState().copy(title = "A Very Long Song Title That Goes On And On - Extended Remastered Edition (Live)"), progressInFixture = true)
    }

    @Test fun landscapeOutput() = shotWide("landscape_output") { PlayerScreen(onBack = {}, fixture = landscapeState(), progressInFixture = true, initialOverlay = 3) }
    @Test fun landscapeMenuCompact280() = shotWide("landscape_menu_280", 600, 280) { PlayerScreen(onBack = {}, fixture = landscapeState(), progressInFixture = true, initialOverlay = 1) }
    @Test fun landscapeMenuLastItemReachable() {
        org.robolectric.RuntimeEnvironment.setQualifiers("w600dp-h280dp")
        composeRule.setContent { MusicAppTheme { Box(Modifier.size(600.dp, 280.dp)) { PlayerScreen(onBack = {}, fixture = landscapeState(), progressInFixture = true, initialOverlay = 1) } } }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_Lyrics", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/landscape_menu_280_scrolled.png")
    }

    // TEST RENDER: ferrofluid motion frames from a synthetic beat signal (120 bpm, 12 fps): 24 frames written as ferro_motion_NN.png, plus a contact sheet via ferrofluid_motion.
    @Test fun ferrofluidMotion() {
        val lvState = androidx.compose.runtime.mutableStateOf(FloatArray(48))
        val bState = androidx.compose.runtime.mutableFloatStateOf(0f)
        val clk = androidx.compose.runtime.mutableLongStateOf(1_000_000_000L)
        composeRule.setContent { MusicAppTheme { Box(Modifier.size(200.dp).background(androidx.compose.ui.graphics.Color(0xFFEAEAE8))) {
            val l = lvState.value; val bb = bState.floatValue
            com.abhi.musicapp.ui.components.ClayBlob({ l }, null, { bb }, { clk.longValue }) } } }
        for (f in 0 until 24) {
            clk.longValue = 1_000_000_000L + f * 83_333_333L
            val t = f / 12f
            val beat = ((t * 2f) % 1f).let { ph -> kotlin.math.exp(-ph * 5f) }
            val amp = 0.25f + 0.65f * beat
            lvState.value = FloatArray(48) { i -> (amp * (1f - i / 60f) * (0.5f + 0.5f * kotlin.math.abs(kotlin.math.sin(i * 1.7f + t * 3f)))).coerceIn(0f, 1f) }
            bState.floatValue = beat
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/ferro_motion_%02d.png".format(f))
        }
    }

    // TEST RENDER: ferrofluid states side by side with synthetic stimulus. Top: silent/paused, quiet, loud, loud+beat peak. Bottom: synthetic beat decay 1.0 -> 0 over 6 steps.
    @Test fun ferrofluidStates() = shot("ferrofluid_states", heightDp = 420) {
        fun lv(a: Float) = FloatArray(48) { i -> (a * (1f - i / 60f) * (0.55f + 0.45f * kotlin.math.abs(kotlin.math.sin(i * 1.7f)))).coerceIn(0f, 1f) }
        val top = listOf(Triple(FloatArray(48), 0f, "silent"), Triple(lv(0.35f), 0f, "quiet"), Triple(lv(0.9f), 0f, "loud"), Triple(lv(0.9f), 1f, "loud+beat"))
        androidx.compose.foundation.layout.Column {
            androidx.compose.foundation.layout.Row {
                top.forEach { (l, b, _) -> Box(Modifier.size(97.dp, 120.dp).padding(2.dp)) { com.abhi.musicapp.ui.components.ClayBlob({ l }, null, { b }) } }
            }
            androidx.compose.foundation.layout.Row {
                listOf(1f, 0.8f, 0.55f, 0.3f, 0.12f, 0f).forEach { b -> Box(Modifier.size(65.dp, 90.dp).padding(2.dp)) { com.abhi.musicapp.ui.components.ClayBlob({ lv(0.9f * (0.5f + 0.5f * b)) }, null, { b }) } }
            }
        }
    }

    @Test fun onboarding() = shot("onboarding") { OnboardingScreen(onDone = {}) }

    @Test fun onboardingPermissions() = shot("onboarding_permissions") { OnboardingScreen(onDone = {}, initialStep = 2) }

    @Test fun onboardingReader() = shot("onboarding_reader") { OnboardingScreen(onDone = {}, initialStep = 3, figmaFixture = true) }
    @Test fun onboardingReaderShipped() = shot("onboarding_reader_shipped") { OnboardingScreen(onDone = {}, initialStep = 3) }

    @Test fun home() = shot("home") {
        HomeScreen({}, {}, {}, {}, {}, {}, fixture = com.abhi.musicapp.playback.PlayerUiState(
            title = "Song name in a line...", isPlaying = true, positionMs = 61_000L, durationMs = 192_000L, hasTrack = true))
    }

    /** SHIPPED Home, fixture = null: real empty/denied library in Robolectric (no audio permission granted). */
    @Test fun homeShippedDenied() = shot("home_shipped_denied") {
        HomeScreen({}, {}, {}, {}, {}, {})
    }

    /** Base Home state (Figma 36-12141): no track loaded, no mini-player. Fixture, DemoData rows. */
    @Test fun homeBase() = shot("home_base_fixture") {
        HomeScreen({}, {}, {}, {}, {}, {}, fixture = com.abhi.musicapp.playback.PlayerUiState())
    }

    @Test fun homeUpdateFixture() = shot("home_update_fixture") {
        HomeScreen({}, {}, {}, {}, {}, {}, updateAvailableFixture = true, fixture = com.abhi.musicapp.playback.PlayerUiState(
            title = "Song name in a line...", isPlaying = true, positionMs = 61_000L, durationMs = 192_000L, hasTrack = true))
    }

    @Test fun homeDownloadRingFixture() = shot("home_download_ring_fixture") {
        HomeScreen({}, {}, {}, {}, {}, {}, downloadRingFixture = true, fixture = com.abhi.musicapp.playback.PlayerUiState(
            title = "Song name in a line...", isPlaying = true, positionMs = 61_000L, durationMs = 192_000L, hasTrack = true))
    }

    @Test fun playerSheetAddToPlaylist() = shot("player_sheet_addtoplaylist") { com.abhi.musicapp.ui.screens.PlayerSheetFixture(com.abhi.musicapp.ui.screens.PlayerSheet.AddToPlaylist) }
    @Test fun playerSheetNewPlaylist() = shot("player_sheet_newplaylist") { com.abhi.musicapp.ui.screens.PlayerSheetFixture(com.abhi.musicapp.ui.screens.PlayerSheet.NewPlaylist) }
    @Test fun playerSheetSpeed() = shot("player_sheet_speed") { com.abhi.musicapp.ui.screens.PlayerSheetFixture(com.abhi.musicapp.ui.screens.PlayerSheet.Speed) }
    @Test fun playerSheetDelete() = shot("player_sheet_delete") { com.abhi.musicapp.ui.screens.PlayerSheetFixture(com.abhi.musicapp.ui.screens.PlayerSheet.Delete) }
    @Test fun playerSheetLyricsEmpty() = shot("player_sheet_lyricsempty") { com.abhi.musicapp.ui.screens.PlayerSheetFixture(com.abhi.musicapp.ui.screens.PlayerSheet.LyricsEmpty) }
    @Test fun playerSheetLyrics() = shot("player_sheet_lyrics") { com.abhi.musicapp.ui.screens.PlayerSheetFixture(com.abhi.musicapp.ui.screens.PlayerSheet.Lyrics) }
    @Test fun playerSheetQueue() = shot("player_sheet_queue") { com.abhi.musicapp.ui.screens.PlayerSheetFixture(com.abhi.musicapp.ui.screens.PlayerSheet.Queue) }

    @Test fun playerSheetMenu() = shot("player_sheet_menu") { com.abhi.musicapp.ui.screens.PlayerSheetFixture(com.abhi.musicapp.ui.screens.PlayerSheet.Menu) }
    @Test fun playerSheetFocusOpen() = shot("player_sheet_focusopen") { com.abhi.musicapp.ui.screens.PlayerSheetFixture(com.abhi.musicapp.ui.screens.PlayerSheet.FocusOpen) }
    @Test fun playerSheetDeviceOpen() = shot("player_sheet_deviceopen") { com.abhi.musicapp.ui.screens.PlayerSheetFixture(com.abhi.musicapp.ui.screens.PlayerSheet.DeviceOpen) }

    @Test fun playerFlippedFixture() = shot("player_flipped_fixture") {
        // TEST RENDER: centre disc on its animation face, synthetic levels (not real audio).
        val lv = FloatArray(48) { i -> (0.9f * (1f - i / 60f) * (0.55f + 0.45f * kotlin.math.abs(kotlin.math.sin(i * 1.7f)))).coerceIn(0f, 1f) }
        PlayerScreen(onBack = {}, fixture = com.abhi.musicapp.playback.PlayerUiState(
            title = "Malcolm Todd - Earrings", artist = "", isPlaying = true,
            positionMs = 61_000L, durationMs = 192_000L, hasTrack = true), progressInFixture = true, fixtureLevels = lv, fixtureFlipped = true, fixtureRotation = 180f)
    }

    @Test fun playerReactiveFixture() = shot("player_reactive_fixture") {
        // TEST RENDER: synthetic bass-heavy levels, not real audio. Shows the ticks grown inward by band level.
        val lv = FloatArray(48) { i -> (0.95f * (1f - i / 60f) * (0.55f + 0.45f * kotlin.math.abs(kotlin.math.sin(i * 1.7f)))).coerceIn(0f, 1f) }
        PlayerScreen(onBack = {}, fixture = com.abhi.musicapp.playback.PlayerUiState(
            title = "Malcolm Todd - Earrings", artist = "", isPlaying = true,
            positionMs = 61_000L, durationMs = 192_000L, hasTrack = true), progressInFixture = true, fixtureLevels = lv)
    }

    @Test fun player() = shot("player") {
        // same sample state as the Figma frame: playing, 1:01 of 03:12
        PlayerScreen(onBack = {}, fixture = com.abhi.musicapp.playback.PlayerUiState(
            title = "Malcolm Todd - Earrings", artist = "", isPlaying = true,
            positionMs = 61_000L, durationMs = 192_000L, hasTrack = true))
    }

    @Test
    fun playerPlaying() = shot("player_playing") {
        PlayerScreen(
            onBack = {},
            fixture = com.abhi.musicapp.playback.PlayerUiState(
                title = "Malcolm Todd - Earrings",
                artist = "",
                isPlaying = true,
                positionMs = 61_000L,
                durationMs = 192_000L,
                hasTrack = true,
            ),
        )
    }


    @Test fun sleepPills() = shot("sleep_pills") {
        androidx.compose.foundation.layout.Box(Modifier.padding(23.dp)) { com.abhi.musicapp.ui.components.SleepPillStack() }
    }

    /** Inset fixture, NOT device validation: 24dp status bar + 48dp nav bar painted as bars over the screen. */
    private fun inset(name: String, content: @androidx.compose.runtime.Composable () -> Unit) = shot(name) {
        Box(Modifier.size(390.dp, 844.dp)) {
            Box(Modifier.padding(top = 24.dp, bottom = 48.dp)) { content() }
            Box(Modifier.size(390.dp, 24.dp).background(androidx.compose.ui.graphics.Color(0x33000000)))
            Box(Modifier.align(androidx.compose.ui.Alignment.BottomStart).size(390.dp, 48.dp).background(androidx.compose.ui.graphics.Color(0x33000000)))
        }
    }

    @Test fun homeInset() = inset("home_inset_fixture") {
        HomeScreen({}, {}, {}, {}, {}, {}, fixture = com.abhi.musicapp.playback.PlayerUiState(
            title = "Song name in a line...", isPlaying = true, positionMs = 61_000L, durationMs = 192_000L, hasTrack = true))
    }

    @Test fun playerInset() = inset("player_inset_fixture") {
        PlayerScreen(onBack = {}, fixture = com.abhi.musicapp.playback.PlayerUiState(
            title = "Malcolm Todd - Earrings", artist = "", isPlaying = true,
            positionMs = 61_000L, durationMs = 192_000L, hasTrack = true))
    }

    @Test fun downloadListFixture() = shot("download_list_fixture") { com.abhi.musicapp.ui.screens.DownloadListFixture() }
    @Test fun search() = shot("search") { SearchScreen(onBack = {}) }
    @Test fun searchPastFixture() = shot("search_past_fixture") { SearchScreen(onBack = {}, fixture = com.abhi.musicapp.ui.screens.SearchFixture.PastRecords) }
    @Test fun searchRecsFixture() = shot("search_recs_fixture") { SearchScreen(onBack = {}, fixture = com.abhi.musicapp.ui.screens.SearchFixture.Recommendations) }

    @Test fun settings() = shot("settings") { SettingsScreen(onBack = {}, onRowClick = {}) }

    // Fixtures: design sample values, render state only. Tall canvas shows the whole 1508dp frame.
    @Test fun settingsFixtureExpanded() = shot("settings_fixture_expanded", 1560) { SettingsScreen(onBack = {}, fixture = true, startExpanded = true) }
    @Test fun settingsFixtureCollapsed() = shot("settings_fixture_collapsed", 1100) { SettingsScreen(onBack = {}, fixture = true, startExpanded = false) }

    // Fixtures: design sample rows/content, render state only.
    @Test fun recycleBinFixture() = shot("recycle_bin_fixture") { RecycleBinScreen(onBack = {}, fixture = List(3) { com.abhi.musicapp.ui.screens.BinRow("[songs name ]", 2) }) }
    @Test fun recycleBinEmpty() = shot("recycle_bin_empty") { RecycleBinScreen(onBack = {}) }
    @Test fun readNowFixture() = shot("read_now_fixture") { ReadNowScreen(onBack = {}, fixtureApiName = "Gemini API", designComparison = true) }
    @Test fun readNowEmpty() = shot("read_now_empty") { ReadNowScreen(onBack = {}) }

    @Test fun themes() = shot("themes") { ThemesScreen(onBack = {}) }

    @Test
    fun themesDark() {
        ThemeState.mode.value = ThemeState.Mode.DARK
        shot("themes_dark") { ThemesScreen(onBack = {}) }
        ThemeState.mode.value = ThemeState.Mode.LIGHT
    }

    @Test fun downloads() = shot("downloads") { DownloadsScreen(onBack = {}) }
    @Test fun downloadsLinkFixture() = shot("downloads_link_fixture") { DownloadsScreen(onBack = {}, fixture = com.abhi.musicapp.ui.screens.DlFixture.LinkAdded) }
    @Test fun downloadsLoadingFixture() = shot("downloads_loading_fixture") { DownloadsScreen(onBack = {}, fixture = com.abhi.musicapp.ui.screens.DlFixture.Loading) }
    @Test fun downloadsSongsFixture() = shot("downloads_songs_fixture") { DownloadsScreen(onBack = {}, fixture = com.abhi.musicapp.ui.screens.DlFixture.Songs) }

    private fun queueItems() = (1..8).map { com.abhi.musicapp.ui.screens.QueueEntry("$it", "Malcolm Todd - Earrings $it", "Artist $it") }
    @Test fun queueSmallScreen() = shot("queue_small_screen", 520) { com.abhi.musicapp.ui.screens.QueueSheetContent(queueItems(), 1, { _, _ -> }, {}, {}) }
    @Test fun queueLargeFont() = shot("queue_large_font", 700) {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(androidx.compose.ui.platform.LocalDensity.current.density, 1.6f)) {
            com.abhi.musicapp.ui.screens.QueueSheetContent(queueItems(), 1, { _, _ -> }, {}, {}) }
    }
    // RENDER-ONLY: production PlayerScreen with injected rail state (muted, speaker preferred) and the clay face showing. Not real routing or audio.
    @Test fun outputRailProduction() = shot("output_rail_production") {
        com.abhi.musicapp.ui.screens.PlayerScreen(onBack = {}, initialOverlay = 3, fixtureFlipped = true, fixtureMuted = true, fixturePrefSpeaker = true,
            fixtureBeat = 0.5f, fixtureLevels = FloatArray(48) { i -> (0.7f * (1f - i / 60f)).coerceIn(0f, 1f) },
            fixture = com.abhi.musicapp.playback.PlayerUiState(title = "Malcolm Todd - Earrings", artist = "", isPlaying = true, positionMs = 61_000L, durationMs = 192_000L, hasTrack = true))
    }
    @Test fun focusRailMutedProduction() = shot("focus_rail_muted_production") {
        com.abhi.musicapp.ui.screens.PlayerScreen(onBack = {}, initialOverlay = 2, fixtureFlipped = true, fixtureMuted = true,
            fixture = com.abhi.musicapp.playback.PlayerUiState(title = "Malcolm Todd - Earrings", artist = "", isPlaying = true, positionMs = 61_000L, durationMs = 192_000L, hasTrack = true))
    }
    @Test fun outputRailProductionLandscape() = shotWide("output_rail_production_landscape") {
        com.abhi.musicapp.ui.screens.PlayerScreen(onBack = {}, initialOverlay = 3, fixtureFlipped = true, fixtureMuted = false, fixturePrefSpeaker = false,
            fixture = com.abhi.musicapp.playback.PlayerUiState(title = "Malcolm Todd - Earrings", artist = "", isPlaying = true, positionMs = 61_000L, durationMs = 192_000L, hasTrack = true))
    }
    // TEST RENDER (synthetic, not real audio): one beat at t=0, then 8 frames 100 ms apart to see one drop leave the pool and merge back. Row of 8 images.
    @Test fun clayDropLife() {
        val clk = androidx.compose.runtime.mutableLongStateOf(1_000_000_000L)
        val bt = androidx.compose.runtime.mutableFloatStateOf(1f)
        val lv = FloatArray(48) { i -> (0.8f * (1f - i / 60f)).coerceIn(0f, 1f) }
        composeRule.setContent { MusicAppTheme { Box(Modifier.size(260.dp).background(androidx.compose.ui.graphics.Color(0xFFEAEAE8))) {
            com.abhi.musicapp.ui.components.ClayBlob({ lv }, null, { bt.floatValue }, { clk.longValue }) } } }
        composeRule.waitForIdle()
        for (f in 0 until 8) {
            clk.longValue = 1_000_000_000L + f * 100_000_000L
            bt.floatValue = if (f == 0) 1f else 0.1f
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/clay_drop_life_%d.png".format(f))
        }
    }
    // TEST RENDER (synthetic beat, injected state, no audio): production PlayerScreen, 8 frames 100 ms apart after one beat, to check drop headroom and motion in the real layout.
    @Test fun clayDropLifeProductionPlayer() {
        val clk = androidx.compose.runtime.mutableLongStateOf(1_000_000_000L)
        val bt = androidx.compose.runtime.mutableFloatStateOf(1f)
        com.abhi.musicapp.ui.components.clayClockOverride = { clk.longValue }
        try {
            composeRule.setContent { MusicAppTheme { com.abhi.musicapp.ui.screens.PlayerScreen(onBack = {}, fixtureFlipped = true,
                fixtureBeat = bt.floatValue, fixtureLevels = FloatArray(48) { i -> (0.8f * (1f - i / 60f)).coerceIn(0f, 1f) },
                fixture = com.abhi.musicapp.playback.PlayerUiState(title = "Malcolm Todd - Earrings", artist = "", isPlaying = true, positionMs = 61_000L, durationMs = 192_000L, hasTrack = true)) } }
            composeRule.waitForIdle()
            for (f in 0 until 8) {
                clk.longValue = 1_000_000_000L + f * 100_000_000L
                bt.floatValue = if (f == 0) 1f else 0.1f
                composeRule.waitForIdle()
                composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/player_drop_life_%d.png".format(f))
            }
        } finally { com.abhi.musicapp.ui.components.clayClockOverride = null }
    }
    // TEST RENDER (synthetic): every band high at once, 4 frames, to check that several drops stay inside the player layout.
    @Test fun clayDropMultiProductionPlayer() {
        val clk = androidx.compose.runtime.mutableLongStateOf(1_000_000_000L)
        val bt = androidx.compose.runtime.mutableFloatStateOf(1f)
        com.abhi.musicapp.ui.components.clayClockOverride = { clk.longValue }
        try {
            composeRule.setContent { MusicAppTheme { com.abhi.musicapp.ui.screens.PlayerScreen(onBack = {}, fixtureFlipped = true,
                fixtureBeat = bt.floatValue, fixtureLevels = FloatArray(48) { i -> if (i % 4 == 0) 1f else 0.85f },
                fixture = com.abhi.musicapp.playback.PlayerUiState(title = "Malcolm Todd - Earrings", artist = "", isPlaying = true, positionMs = 61_000L, durationMs = 192_000L, hasTrack = true)) } }
            composeRule.waitForIdle()
            for (f in 0 until 16) {
                clk.longValue = 1_000_000_000L + f * 70_000_000L
                bt.floatValue = if (f == 0) 1f else 0.1f
                composeRule.waitForIdle()
                composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/player_drop_multi_%d.png".format(f))
            }
        } finally { com.abhi.musicapp.ui.components.clayClockOverride = null }
    }
    // TEST RENDER (synthetic levels, fixed clock): the new smaller irregular centre in five states: silent, quiet, low-heavy, one band, loud. No audio.
    @Test fun clayShapes() {
        val loud = FloatArray(48) { i -> if (i % 3 == 0) 1f else 0.7f }
        val low = FloatArray(48) { i -> (0.8f * (1f - i / 60f)).coerceIn(0f, 1f) }
        val one = FloatArray(48) { i -> if (i in 18..26) 0.95f else 0.1f }
        // (levels, clock seconds): silent, quiet (below the gate), then music at different times so the shapes differ.
        val states = listOf<Pair<FloatArray?, Long>>(null to 5L, FloatArray(48) { 0.12f } to 5L, low to 5L, one to 5L, loud to 5L, loud to 13L, one to 21L, low to 30L)
        shotWide("clay_shapes", 1312, 200) {
            androidx.compose.foundation.layout.Row {
                states.forEach { (lv, sec) -> Box(Modifier.size(164.dp)) { com.abhi.musicapp.ui.components.ClayBlob({ lv }, null, { if (lv == null) 0f else 0.5f }, { sec * 1_000_000_000L }) } }
            }
        }
    }
    @Test fun lyricsEmptyState() = shot("lyrics_empty_state", 600) { Box(Modifier.size(390.dp, 600.dp)) { com.abhi.musicapp.ui.screens.LyricsSheet(-1L, "Malcolm Todd - Earrings") {} } }
    @Test fun settingsTransitions() = shot("settings_transitions", 1500) { SettingsScreen(onBack = {}, onRowClick = {}, startExpanded = true) }
    @Test fun settingsTransitionsScrolled() {
        composeRule.setContent { MusicAppTheme { Box(Modifier.size(390.dp, 600.dp).background(androidx.compose.material3.MaterialTheme.colorScheme.background)) { SettingsScreen(onBack = {}, onRowClick = {}, startExpanded = true) } } }
        composeRule.onNode(androidx.compose.ui.test.hasScrollAction()).performTouchInput { swipeUp(startY = bottom - 10f, endY = top + 10f) }
        composeRule.onNode(androidx.compose.ui.test.hasScrollAction()).performTouchInput { swipeUp(startY = bottom - 10f, endY = top + 10f) }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/settings_transitions_scrolled.png")
    }
    // Home scan ring: grey rays stay put, orange cluster advances (phase in ray steps). One row of 6 frames.
    @Test fun scanRingTravel() = shot("scan_ring_travel", 120) {
        androidx.compose.foundation.layout.Row {
            listOf(0f, 2.5f, 5f, 7.5f, 10f, 12f).forEach { ph -> Box(Modifier.size(64.dp).padding(2.dp)) { com.abhi.musicapp.ui.screens.ScanRays(Modifier.size(60.dp), phaseOverride = ph) } }
        }
    }

    @Test fun equaliser() = shot("equaliser") { EqualiserScreen(onBack = {}) }
}
