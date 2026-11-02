package com.abhi.musicapp

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class LauncherIconRenderTest {
    /** Draws the launcher icon exactly as the system would (default mask) for a visual check. */
    @Test fun launcherIconRender() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val d = androidx.core.content.ContextCompat.getDrawable(app, R.mipmap.ic_launcher)!!
        val bmp = android.graphics.Bitmap.createBitmap(432, 432, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        c.drawColor(android.graphics.Color.WHITE)
        d.setBounds(0, 0, 432, 432)
        d.draw(c)
        bmp.captureRoboImage("build/outputs/roborazzi/launcher_icon.png")
        val mono = (d as android.graphics.drawable.AdaptiveIconDrawable).monochrome
        org.junit.Assert.assertNotNull(mono)
    }
}
