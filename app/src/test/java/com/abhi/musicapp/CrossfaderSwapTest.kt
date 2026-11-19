package com.abhi.musicapp

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.playback.Crossfader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Lifecycle and ORDER checks for the player role swap, using two real ExoPlayer objects that never play audio. This checks my swap logic
 * (who becomes active, who is released, what the session is told). It does not check Android audio, notification or effect behaviour.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CrossfaderSwapTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private val events = ArrayList<String>()
    private fun make(): Triple<Crossfader, ExoPlayer, ExoPlayer> {
        val main = ExoPlayer.Builder(ctx).build(); val inc = ExoPlayer.Builder(ctx).build()
        val cf = Crossfader(ctx, main, { ExoPlayer.Builder(ctx).build() }, { p -> events.add("swap:" + (if (p === inc) "incoming" else "other") + ":oldReleased=" + main.isReleased) })
        return Triple(cf, main, inc)
    }

    @Test fun swapMakesIncomingActiveAndReleasesOldAfterTellingTheSession() {
        val (cf, main, inc) = make()
        cf.injectIncomingForTest(inc, 1)
        cf.swapForTest()
        assertSame(inc, cf.activePlayer)
        assertEquals(listOf("swap:incoming:oldReleased=false"), events) // session told while the old player is still alive
        assertTrue(main.isReleased)
        assertFalse(inc.isReleased)
        assertEquals(1f, inc.volume, 0.001f)
        inc.release()
    }

    @Test fun cancelReleasesIncomingAndKeepsMain() {
        val (cf, main, inc) = make()
        main.volume = 0.3f
        cf.injectIncomingForTest(inc, 1)
        cf.cancelForTest()
        assertTrue(inc.isReleased)
        assertFalse(main.isReleased)
        assertSame(main, cf.activePlayer)
        assertEquals(1f, main.volume, 0.001f)
        assertTrue(events.isEmpty())
        main.release()
    }

    @Test fun muteCarriesIntoTheNewActivePlayer() {
        val (cf, _, inc) = make()
        cf.setMuted(true)
        cf.injectIncomingForTest(inc, 1)
        cf.swapForTest()
        assertEquals(0f, inc.volume, 0.001f)
        inc.release()
    }

    @Test fun cancelAfterSwapDoesNotTouchTheReleasedPlayer() {
        val (cf, main, inc) = make()
        cf.injectIncomingForTest(inc, 1)
        cf.swapForTest()
        cf.cancelForTest()
        assertTrue(main.isReleased)
        assertFalse(inc.isReleased)
        inc.release()
    }

    @Test fun swapWithNothingIncomingIsANoOp() {
        val (cf, main, inc) = make()
        cf.swapForTest()
        assertTrue(events.isEmpty())
        assertSame(main, cf.activePlayer)
        main.release(); inc.release()
    }
}
