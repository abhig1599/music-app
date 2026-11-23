package com.abhi.musicapp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.playback.PlaybackService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.Duration

/** Service wiring/lifecycle, not decoder or audio-output verification. */
@RunWith(AndroidJUnit4::class)
@Config(sdk=[34])
class PlaybackServiceLifecycleTest {
    @Test fun initiallyUnknownQueueAndMidSongOptInAreSafeAndShutdownCancelsReconciliation() {
        val ctx=ApplicationProvider.getApplicationContext<Context>()
        val prefs=ctx.getSharedPreferences("transitions",Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val controller=Robolectric.buildService(PlaybackService::class.java).create()
        val main=PlaybackService.exo!!
        assertFalse(main.isReleased)
        prefs.edit().putBoolean("on",true).putBoolean("automix",true).commit()
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        assertFalse(main.isReleased) // unknown duration/no pair: no analysis or phase start
        prefs.edit().putBoolean("automix",false).commit()
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        controller.destroy()
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        assertTrue(main.isReleased)
        assertNull(PlaybackService.exo); assertNull(PlaybackService.crossfader)
        prefs.edit().clear().commit()
    }
}
