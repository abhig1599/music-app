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
 * WIRED ONLY IN DEBUG BUILDS (PlaybackService.buildPlayer inserts it into the processor chain in debuggable builds (FLAG_DEBUGGABLE); the release
 * chain never includes it): a pass-through tap that can dump the PCM each
 * player decodes to filesDir/xfade_pcm/deck<N>.pcm, armed only when the "pcm_capture" transitions pref is true (the emulator
 * transition smoke arms it via run-as; nothing is ever written unless armed). It exists so the transition test can show both decks
 * decoding audio simultaneously during a ramp and reconstruct what the app sent toward the mixer.
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
