package com.abhi.musicapp

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.analysis.TempoCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Cache-key shape: a dead or column-less uri must fall back to the bare media id (one extra analysis, never a wrong BPM). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TempoCacheKeyTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun nullUriFallsBackToMediaId() {
        assertEquals("42", TempoCache.keyFor(ctx, null, "42"))
    }

    @Test fun unreadableUriFallsBackToMediaId() {
        assertEquals("42", TempoCache.keyFor(ctx, Uri.parse("content://dead.provider/none"), "42"))
    }

    @Test fun keyNeverContainsPathOrPersonalData() {
        val k = TempoCache.keyFor(ctx, Uri.parse("content://dead.provider/none"), "42")
        assertTrue(k.matches(Regex("[0-9:._-]+")))
    }
}
