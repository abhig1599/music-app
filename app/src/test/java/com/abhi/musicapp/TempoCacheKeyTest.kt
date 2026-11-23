package com.abhi.musicapp

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.analysis.TempoCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Cache-key policy: a key carries the file's size + modified time when they can be read. When they cannot
 * (dead/null uri), the key is NOT persistable - results stay in memory for the session and never hit disk,
 * so a replaced file can never inherit a stale tempo from a bare-id disk entry.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TempoCacheKeyTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun legacyBpmOnlyEntryIsNotTreatedAsConfidentAnalysis() {
        val key = TempoCache.Key("legacy-regression", true)
        ctx.getSharedPreferences("tempo", Context.MODE_PRIVATE).edit().putFloat(key.value, 120f).commit()
        assertEquals(null, TempoCache.cachedResult(ctx, key))
    }

    @Test fun tailWindowDiscardsPrerollAtFramePrecision() {
        assertEquals(0, TempoCache.skipFramesBeforeStart(2_000_000L, 2_000_000L, 48000, 1024))
        assertEquals(480, TempoCache.skipFramesBeforeStart(1_990_000L, 2_000_000L, 48000, 1024))
        assertEquals(1024, TempoCache.skipFramesBeforeStart(1_000_000L, 2_000_000L, 48000, 1024))
        assertEquals(1, TempoCache.skipFramesBeforeStart(1_999_999L, 2_000_000L, 48000, 1024))
    }

    @Test fun nullUriIsNotPersistable() {
        val k = TempoCache.keyFor(ctx, null, "42")
        assertEquals("42", k.value); assertFalse(k.persistable)
    }

    @Test fun unreadableUriIsNotPersistable() {
        val k = TempoCache.keyFor(ctx, Uri.parse("content://dead.provider/none"), "42")
        assertEquals("42", k.value); assertFalse(k.persistable)
    }

    @Test fun keyValueContainsNoPathOrPersonalData() {
        assertTrue(TempoCache.keyFor(ctx, null, "42").value.matches(Regex("[0-9:._-]+")))
    }

    @Test fun nonPersistableAnalysisNeverWritesDiskPrefs() {
        val uri = Uri.parse("content://dead.provider/none")
        val key = TempoCache.keyFor(ctx, uri, "no-such-song-7")
        TempoCache.analyse(ctx, uri, key) // dead uri decodes to nothing; must not persist
        val disk = ctx.getSharedPreferences("tempo", Context.MODE_PRIVATE)
        assertFalse(disk.contains(key.value))
        assertEquals(-1f, TempoCache.cached(ctx, key)!!, 0f) // but the session remembers "unknown"
    }
}
