package com.abhi.musicapp

import com.abhi.musicapp.analysis.PhaseSchedule
import com.abhi.musicapp.analysis.PhaseTransitionPlanner
import com.abhi.musicapp.analysis.TailCache
import com.abhi.musicapp.playback.Crossfader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Identity, boundary and crossfade-only arm behavior for the content-end trim. */
class TailWiringTest {
    private fun id(uri: String, size: Long = 3_000_000, mtime: Long = 1_700_000_000_000, dur: Long = 240_000) =
        TailCache.FileIdentity(uri, size, mtime, dur)

    @After fun resetCache() = TailCache.clear()

    @Test fun cacheHitRequiresFullIdentity() {
        TailCache.put(id("u"), 237_000L)
        assertEquals(237_000L, TailCache.contentEndMs(id("u")))
    }

    @Test fun replacedSameLengthFileDoesNotReuseStaleContentEnd() {
        // The flagged gap: same uri AND same duration, but a different file (rewritten -> new mtime/size).
        TailCache.put(id("file:///a.mp3"), 237_000L)
        assertNull(TailCache.contentEndMs(id("file:///a.mp3", mtime = 1_700_000_500_000)))
        assertNull(TailCache.contentEndMs(id("file:///a.mp3", size = 3_000_001)))
        assertNull(TailCache.contentEndMs(id("file:///a.mp3", dur = 241_000)))
        assertNull(TailCache.contentEndMs(id("file:///other.mp3")))
    }

    @Test fun cacheRejectsOutOfRangeContentEnd() {
        TailCache.put(id("u"), 0L)
        TailCache.put(id("u"), 240_001L)
        assertNull(TailCache.contentEndMs(id("u")))
    }

    @Test fun effectiveDurationBounds() {
        assertEquals(240_000L, Crossfader.effectiveDurationMs(240_000L, null))
        assertEquals(240_000L, Crossfader.effectiveDurationMs(240_000L, 0L))
        assertEquals(240_000L, Crossfader.effectiveDurationMs(240_000L, -5L))
        assertEquals(240_000L, Crossfader.effectiveDurationMs(240_000L, 240_001L)) // stale longer-file detection
        assertEquals(237_500L, Crossfader.effectiveDurationMs(240_000L, 237_500L))
    }

    @Test fun crossfadeOnlyArmsAgainstDetectedContentEndNotFileEnd() {
        // 240s file, detected content end 236s, user fade 6s, speed 1x. At 231s the file end leaves 9s of
        // media - beyond fade + 1.5s, so NO arm yet; the detected end leaves 5s, so it arms, ending 5s ahead.
        assertNull(Crossfader.decideArm(240_000L, 231_000L, 1f, null, 6_000L))
        val d = Crossfader.decideArm(236_000L, 231_000L, 1f, null, 6_000L)!!
        assertEquals(6_000L, d.fadeMs)
        assertEquals(5_000L, d.deadlineAfterMs)
    }

    @Test fun crossfadeOnlyFadeIsCappedAtThirdOfEffectiveDuration() {
        // The shortened boundary feeds ordinary fade timing: the cap uses the detected end, not the file end.
        val d = Crossfader.decideArm(12_000L, 7_000L, 1f, null, 6_000L)!!
        assertEquals(4_000L, d.fadeMs)
        assertEquals(5_000L, d.deadlineAfterMs)
    }

    @Test fun crossfadeOnlyNeverArmsOutsideTheFadeWindow() {
        assertNull(Crossfader.decideArm(236_000L, 100_000L, 1f, null, 6_000L)) // early: too much left
        assertNull(Crossfader.decideArm(236_000L, 237_000L, 1f, null, 6_000L)) // past the effective end
        assertNull(Crossfader.decideArm(236_000L, 235_999L, 1f, null, 6_000L)) // under 300ms left: too late
    }

    @Test fun shortenedBoundaryInvalidatesPhasePlanEvidence() {
        // A plan computed against the file end (fade ends exactly at 240s) must NOT survive a shortened
        // content end: prepare validates fade == duration - start, so 237.5s invalidates it and the pair
        // falls back to the plain crossfade instead of mistiming the blend.
        val plan = PhaseTransitionPlanner.Plan(true, 236_000L, 4_000L, 1f, 5f, "test")
        assertNotNull(PhaseSchedule.prepare(plan, 200_000L, 240_000L, 200_000L, 1f, 6_000L))
        assertNull(PhaseSchedule.prepare(plan, 200_000L, 237_500L, 200_000L, 1f, 6_000L))
    }
}
