package com.abhi.musicapp.playback

import android.app.Activity
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertSame
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/** Guards the controller context choice only. It does not exercise Media3 binding. */
@RunWith(RobolectricTestRunner::class)
class PlayerManagerContextTest {
    @Test
    fun controllerIsBoundWithApplicationContextNotActivity() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val ctx = PlayerManager.controllerContext(activity)
        assertNotSame(activity, ctx)
        assertSame(ApplicationProvider.getApplicationContext<android.content.Context>().applicationContext, ctx.applicationContext)
    }
}
