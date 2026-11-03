package com.abhi.musicapp

import com.abhi.musicapp.playback.SleepState
import com.abhi.musicapp.playback.exitSecondsLeft
import com.abhi.musicapp.playback.sleepFraction
import com.abhi.musicapp.playback.sleepStep
import com.abhi.musicapp.ui.screens.clockText
import com.abhi.musicapp.ui.screens.customSleepMs
import com.abhi.musicapp.ui.components.FIGMA_DIAL_ANGLES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerLogicTest {
    private val c = SleepState.Counting(600_000, 700_000)

    @Test fun keepsCountingBeforeEnd() {
        val r = sleepStep(c, 699_999, false, false, true)
        assertEquals(c, r.state); assertTrue(!r.pause && !r.exitApp)
    }
    @Test fun endPausesAndStartsTenSecondExit() {
        val r = sleepStep(c, 700_000, false, false, true)
        assertEquals(SleepState.Exiting(600_000, 710_000), r.state); assertTrue(r.pause); assertTrue(!r.exitApp)
    }
    @Test fun lastSongToEndKeepsPlayingUntilSongEnds() {
        val r = sleepStep(c, 700_000, true, false, true)
        assertEquals(SleepState.FinishingSong(600_000), r.state); assertTrue(!r.pause)
        assertEquals(r.state, sleepStep(r.state, 800_000, true, false, true).state)
        val e = sleepStep(r.state, 800_000, true, true, true)
        assertEquals(SleepState.Exiting(600_000, 810_000), e.state); assertTrue(e.pause)
    }
    @Test fun lastSongToEndWhilePausedStopsAtOnce() {
        assertTrue(sleepStep(c, 700_000, true, false, false).state is SleepState.Exiting)
    }
    @Test fun exitFiresOnceAtDeadline() {
        val x = SleepState.Exiting(1, 10_000)
        assertTrue(!sleepStep(x, 9_999, false, false, false).exitApp)
        val r = sleepStep(x, 10_000, false, false, false)
        assertTrue(r.exitApp && r.pause); assertEquals(SleepState.Off, r.state)
        assertTrue(!sleepStep(SleepState.Off, 20_000, false, false, false).exitApp)
    }
    @Test fun secondsAndFraction() {
        assertEquals(10, exitSecondsLeft(10_000, 0)); assertEquals(1, exitSecondsLeft(10_000, 9_001)); assertEquals(0, exitSecondsLeft(10_000, 12_000))
        assertEquals(0.5f, sleepFraction(600_000, 700_000, 400_000), 1e-4f)
        assertEquals(1f, sleepFraction(600_000, 700_000, 900_000), 0f)
    }
    @Test fun customTimeValidation() {
        assertEquals(90 * 60_000L, customSleepMs("1", "30")); assertEquals(5 * 60_000L, customSleepMs("", "5"))
        assertNull(customSleepMs("", "")); assertNull(customSleepMs("0", "0")); assertNull(customSleepMs("0", "60")); assertNull(customSleepMs("25", "0"))
        assertEquals(24 * 3_600_000L, customSleepMs("24", "0"))
    }
    @Test fun clock() { assertEquals("10:01", clockText(601_000)); assertEquals("0:00", clockText(-5)); assertEquals("90:00", clockText(5_400_000)) }
    @Test fun figmaDialAngles() {
        assertEquals(48, FIGMA_DIAL_ANGLES.size)
        assertTrue(FIGMA_DIAL_ANGLES.toList().zipWithNext().all { (a, b) -> b > a })
        assertTrue(FIGMA_DIAL_ANGLES.last() < 360.0)
        val gaps = (FIGMA_DIAL_ANGLES.toList() + 360.0).zipWithNext { a, b -> b - a }
        assertEquals(360.0, gaps.sum(), 1e-9); assertTrue(gaps.max() - gaps.min() > 3.0) // irregular, not 7.5 each
    }
}
