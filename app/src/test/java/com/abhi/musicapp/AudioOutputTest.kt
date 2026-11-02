package com.abhi.musicapp

import android.media.AudioDeviceInfo
import com.abhi.musicapp.playback.OutputKind
import com.abhi.musicapp.playback.classifyOutput
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioOutputTest {
    @Test fun classifies() {
        assertEquals(OutputKind.SPEAKER, classifyOutput(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))
        assertEquals(OutputKind.WIRED_HEADPHONES, classifyOutput(AudioDeviceInfo.TYPE_WIRED_HEADSET))
        assertEquals(OutputKind.BLUETOOTH, classifyOutput(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
        assertEquals(OutputKind.UNKNOWN, classifyOutput(AudioDeviceInfo.TYPE_TELEPHONY))
    }
}
