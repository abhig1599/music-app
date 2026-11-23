package com.abhi.musicapp.playback

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

const val LEVEL_BANDS = 48
internal const val FFT_SIZE = 1024

/**
 * Frequency analysis of the audio ExoPlayer is actually decoding (a Media3 TeeAudioProcessor sees the
 * PCM; no microphone, no RECORD_AUDIO, nothing leaves the app). [bands] holds 0..1 levels, low
 * frequencies in index 0 up to ~14 kHz. Written from the audio thread, read by the UI.
 */
object AudioLevels {
    @Volatile var bands: FloatArray = FloatArray(LEVEL_BANDS)
        private set
    /** 0..1 pulse that jumps on a bass onset (a kick/beat-like jump in low-frequency energy) and decays. An approximation, not tempo tracking. */
    @Volatile var beat: Float = 0f
        private set
    @Volatile private var enabled = false

    /** The Player screen turns analysis on while it is visible and playing; off, the audio thread does no maths. */
    fun setEnabled(on: Boolean) { enabled = on; if (!on) { bands = FloatArray(LEVEL_BANDS); beat = 0f } }

    private val analyzer = BandAnalyzer() // one shared analyzer; during an overlap BOTH decks feed it, so its methods are synchronized

    @kotlin.OptIn(UnstableApi::class)
    fun newProcessor(): TeeAudioProcessor = TeeAudioProcessor(object : TeeAudioProcessor.AudioBufferSink {
        override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
            analyzer.configure(sampleRateHz, channelCount, encoding)
            bands = FloatArray(LEVEL_BANDS)
        }
        override fun handleBuffer(buffer: ByteBuffer) {
            if (!enabled) return
            analyzer.push(buffer)?.let { bands = it; beat = analyzer.beat }
        }
    })
}

/** Pure maths, unit-testable: turns interleaved PCM into [LEVEL_BANDS] levels in 0..1. */
class BandAnalyzer {
    private var rate = 44100
    private var channels = 2
    private var encoding = C.ENCODING_PCM_16BIT
    private val window = FloatArray(FFT_SIZE)
    private var filled = 0
    private val re = DoubleArray(FFT_SIZE)
    private val im = DoubleArray(FFT_SIZE)
    private val hann = DoubleArray(FFT_SIZE) { 0.5 - 0.5 * cos(2 * PI * it / (FFT_SIZE - 1)) }
    private var edges = binEdges(rate)
    /** Beat pulse 0..1 after the latest window: set by a bass jump over its running average, decays about 100 ms per step of ~12 ms hop. */
    var beat = 0f
        private set
    private var bassAvg = 0.0
    private var peak = 1e-3 // slow automatic gain so quiet and loud songs both fill the ring

    @Synchronized
    fun configure(sampleRate: Int, channelCount: Int, enc: Int) {
        rate = if (sampleRate > 0) sampleRate else 44100
        channels = max(1, channelCount); encoding = enc; filled = 0; beat = 0f; bassAvg = 0.0; edges = binEdges(rate); peak = 1e-3
    }

    /** Returns fresh levels once a full window has been collected, else null. Unsupported encodings give null.
     *  Synchronized: two live decks call this from two audio threads during a crossfade; without the lock the
     *  window fill can race past the buffer and throw on the audio thread (a playback-stopping sink error). */
    @Synchronized
    fun push(src: ByteBuffer): FloatArray? {
        val b = src.duplicate().order(ByteOrder.nativeOrder())
        val bytes = when (encoding) { C.ENCODING_PCM_16BIT -> 2; C.ENCODING_PCM_FLOAT -> 4; else -> return null }
        var out: FloatArray? = null
        while (b.remaining() >= bytes * channels) {
            var s = 0f
            for (c in 0 until channels) s += if (bytes == 2) b.short / 32768f else b.float
            window[filled++] = s / channels
            if (filled == FFT_SIZE) { out = analyze(window, rate); filled = FFT_SIZE / 2; System.arraycopy(window, FFT_SIZE / 2, window, 0, FFT_SIZE / 2) }
        }
        return out
    }

    @Synchronized // shares beat/bassAvg/peak state with push (and is called from it)
    fun analyze(samples: FloatArray, sampleRate: Int): FloatArray {
        if (sampleRate != rate) { rate = sampleRate; edges = binEdges(rate) }
        for (i in 0 until FFT_SIZE) { re[i] = samples[i] * hann[i]; im[i] = 0.0 }
        fft(re, im)
        val mags = DoubleArray(FFT_SIZE / 2) { sqrt(re[it] * re[it] + im[it] * im[it]) / FFT_SIZE }
        val raw = DoubleArray(LEVEL_BANDS)
        for (k in 0 until LEVEL_BANDS) {
            var m = 0.0
            for (i in edges[k] until max(edges[k] + 1, edges[k + 1])) m = max(m, mags[min(i, mags.size - 1)])
            // Pink-ish tilt: music has far more energy in the bass; lift the highs so the whole ring moves.
            raw[k] = m * (1.0 + 6.0 * k / LEVEL_BANDS)
        }
        val bass = (0 until 8).sumOf { raw[it] } / 8.0
        beat *= 0.9f
        if (bass > 2e-4 && bass > bassAvg * 1.45) beat = max(beat, min(1.0, 0.45 + (bass / (bassAvg + 1e-9) - 1.45) * 0.4).toFloat())
        bassAvg = bassAvg * 0.92 + bass * 0.08
        val frameMax = raw.max()
        peak = max(frameMax, peak * 0.995)
        return FloatArray(LEVEL_BANDS) { k ->
            // Silence stays at zero (no fake motion): below the noise floor nothing is shown.
            if (frameMax < 1e-4) 0f else min(1.0, sqrt(raw[k] / peak)).toFloat()
        }
    }

    companion object {
        /** FFT bin edges for LEVEL_BANDS log-spaced bands from 40 Hz to min(14 kHz, Nyquist). */
        fun binEdges(rate: Int): IntArray {
            val lo = 40.0; val hi = min(14000.0, rate / 2.0 - 1)
            val binHz = rate.toDouble() / FFT_SIZE
            return IntArray(LEVEL_BANDS + 1) { k -> (lo * Math.pow(hi / lo, k.toDouble() / LEVEL_BANDS) / binHz).toInt().coerceIn(1, FFT_SIZE / 2 - 1) }
        }

        fun fft(re: DoubleArray, im: DoubleArray) {
            val n = re.size
            var j = 0
            for (i in 1 until n) {
                var bit = n shr 1
                while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
                j = j xor bit
                if (i < j) { var t = re[i]; re[i] = re[j]; re[j] = t; t = im[i]; im[i] = im[j]; im[j] = t }
            }
            var len = 2
            while (len <= n) {
                val ang = -2 * PI / len
                val wr = cos(ang); val wi = sin(ang)
                var i = 0
                while (i < n) {
                    var cr = 1.0; var ci = 0.0
                    for (k in 0 until len / 2) {
                        val a = i + k; val bb = a + len / 2
                        val tr = re[bb] * cr - im[bb] * ci; val ti = re[bb] * ci + im[bb] * cr
                        re[bb] = re[a] - tr; im[bb] = im[a] - ti; re[a] += tr; im[a] += ti
                        val nr = cr * wr - ci * wi; ci = cr * wi + ci * wr; cr = nr
                    }
                    i += len
                }
                len = len shl 1
            }
        }
    }
}
