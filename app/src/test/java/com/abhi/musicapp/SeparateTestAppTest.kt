package com.abhi.musicapp

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The debug build must install beside the existing cand-0.1.0 app: different package ID and a different label. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SeparateTestAppTest {
    @Test fun debugBuildHasSeparateIdAndLabel() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        assertEquals("com.abhi.musicapp.test", app.packageName)
        assertEquals("Music App Test", app.applicationInfo.loadLabel(app.packageManager).toString())
        assertEquals("0.1.6", app.packageManager.getPackageInfo(app.packageName, 0).versionName)
    }
}
