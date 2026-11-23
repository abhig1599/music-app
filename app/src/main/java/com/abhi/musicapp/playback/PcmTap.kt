package com.abhi.musicapp.playback

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

/**
 * BUILT ONLY WITH -PpcmTap=true (BuildConfig.PCM_TAP gates the processor chain in PlaybackService.buildPlayer; every candidate APK
 * is built without the property, so distributed builds have no tap in the chain at all, no pref to arm and nothing a backup could
 * restore): a pass-through tap that can dump the PCM each
 * player decodes to filesDir/xfade_pcm/deck<N>.pcm, armed only when the "pcm_capture" transitions pref is true. That pref is set
 * solely by the emulator transition smoke (via run-as) for its controlled synthetic-tone fixtures, and the smoke disarms it and
 * wipes the tap directory when it finishes. It is never armed on a real library: this must not capture user music. It exists so
 * the transition test can show both decks decoding audio simultaneously during a ramp and reconstruct what the app sent toward
 * the mixer.
 */
@UnstableApi
object PcmTap {
    private val next = AtomicInteger(0)

    fun newProcessor(context: Context): TeeAudioProcessor {
        val app = context.applicationContext
        val deck = next.getAndIncrement()
        return TeeAudioProcessor(object : TeeAudioProcessor.AudioBufferSink {
            private var out: FileOutputStream? = null
            private var armed = false

            override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
                armed = app.getSharedPreferences("transitions", Context.MODE_PRIVATE).getBoolean("pcm_capture", false)
                try { out?.close() } catch (_: Exception) {}
                out = null
                if (!armed || encoding != C.ENCODING_PCM_16BIT) { armed = false; return }
                val dir = File(app.filesDir, "xfade_pcm"); dir.mkdirs()
                File(dir, "deck$deck.txt").writeText("sampleRate=$sampleRateHz channels=$channelCount encoding=$encoding\n")
                out = FileOutputStream(File(dir, "deck$deck.pcm"), false)
            }

            override fun handleBuffer(buffer: ByteBuffer) {
                val o = out ?: return
                if (!armed) return
                val b = ByteArray(buffer.remaining())
                buffer.get(b)
                o.write(b)
            }
        })
    }
}
