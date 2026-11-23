package com.abhi.musicapp

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.analysis.AutoMix
import com.abhi.musicapp.playback.Crossfader
import com.abhi.musicapp.playback.configureIncoming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Engine gate: AutoMix implies Crossfade (and the 0.1.7 AutoMix-on/Crossfade-off saved state must now mix, not sit dead). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TransitionGateTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Before fun reset() {
        ctx.getSharedPreferences("transitions", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun crossfadeOnAloneAllows() {
        Crossfader.setEnabled(ctx, true); AutoMix.setSwitchedOn(ctx, false)
        assertTrue(Crossfader.transitionsAllowed(ctx))
    }

    @Test fun automixOnWithCrossfadeOffStillAllows() { // the 0.1.7 dead-switch state
        Crossfader.setEnabled(ctx, false); AutoMix.setSwitchedOn(ctx, true)
        assertTrue(Crossfader.transitionsAllowed(ctx))
    }

    @Test fun bothOffBlocks() {
        Crossfader.setEnabled(ctx, false); AutoMix.setSwitchedOn(ctx, false)
        assertFalse(Crossfader.transitionsAllowed(ctx))
    }

    @Test fun incomingDeckStartsOnTheNextSongWithSameQueue() {
        val main = ExoPlayer.Builder(ctx).build()
        val inc = ExoPlayer.Builder(ctx).build()
        val items = (0 until 5).map { MediaItem.Builder().setUri("file:///s$it.mp3").setMediaId("id$it").build() }
        main.setMediaItems(items, 1, 0L)
        configureIncoming(inc, main, main.nextMediaItemIndex)
        assertEquals(5, inc.mediaItemCount)
        assertEquals(2, inc.currentMediaItemIndex)
        assertEquals("id2", inc.currentMediaItem?.mediaId)
        (0 until 5).forEach { assertEquals("id$it", inc.getMediaItemAt(it).mediaId) }
        main.release(); inc.release()
    }
}
