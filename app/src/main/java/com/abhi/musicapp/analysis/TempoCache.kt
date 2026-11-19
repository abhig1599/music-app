package com.abhi.musicapp.analysis

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri

/**
 * Decodes up to [MAX_SECONDS] of a local file to mono floats and runs [TempoAnalyzer]. Results (including "unknown") are cached per
 * file key in SharedPreferences "tempo". Nothing calls this from playback yet; it is a prototype that needs real songs to judge.
 * Local only, no network. Not hardware-verified: decoding was never run in CI.
 */
object TempoCache {
    const val MAX_SECONDS = 60
    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("tempo", Context.MODE_PRIVATE)

    /** Cached value: null = not analysed yet; bpm < 0 = analysed, unknown. */
    fun cached(c: Context, key: String): Float? = if (prefs(c).contains(key)) prefs(c).getFloat(key, -1f) else null

    /** Blocking (call off the main thread). Returns the BPM or null when unknown. */
    fun analyse(c: Context, uri: Uri, key: String, cancelled: () -> Boolean = { false }): Float? {
        cached(c, key)?.let { return if (it > 0f) it else null }
        val decoded = try { decode(c, uri, cancelled) } catch (e: Exception) { null }
        if (cancelled()) return null // never cache a cancelled run as "unknown"
        val r = decoded?.let { TempoAnalyzer.analyze(it.first, it.second) }
        prefs(c).edit().putFloat(key, r?.bpm ?: -1f).apply()
        return r?.bpm
    }

    /** Background tempo prefetch for the next queued song. Off by default and has no setting in the UI; nothing audible uses the result. */
    fun prefetchEnabled(c: Context) = prefs(c).getBoolean("prefetch_on", false)

    private fun decode(c: Context, uri: Uri, cancelled: () -> Boolean): Pair<FloatArray, Int>? {
        val ex = MediaExtractor()
        try {
            ex.setDataSource(c, uri, null)
            val idx = (0 until ex.trackCount).firstOrNull { ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true } ?: return null
            ex.selectTrack(idx)
            val fmt = ex.getTrackFormat(idx)
            val sr = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var ch = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
            val codec = MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME)!!)
            val out = FloatArray(sr * MAX_SECONDS); var n = 0
            try {
            codec.configure(fmt, null, null, 0); codec.start()
            val info = MediaCodec.BufferInfo(); var inDone = false; var outDone = false
            while (!outDone && n < out.size && !cancelled()) {
                if (!inDone) {
                    val i = codec.dequeueInputBuffer(10_000)
                    if (i >= 0) {
                        val sz = ex.readSampleData(codec.getInputBuffer(i)!!, 0)
                        if (sz < 0) { codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inDone = true }
                        else { codec.queueInputBuffer(i, 0, sz, ex.sampleTime, 0); ex.advance() }
                    }
                }
                val o = codec.dequeueOutputBuffer(info, 10_000)
                if (o >= 0) {
                    val sb = codec.getOutputBuffer(o)!!.order(java.nio.ByteOrder.nativeOrder()).asShortBuffer()
                    val frames = sb.remaining() / ch
                    for (f in 0 until frames) {
                        var s = 0f
                        for (k in 0 until ch) s += sb.get(f * ch + k) / 32768f
                        if (n < out.size) out[n++] = s / ch
                    }
                    codec.releaseOutputBuffer(o, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outDone = true
                } else if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    ch = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                }
            }
            } finally { try { codec.stop() } catch (e: Exception) {}; codec.release() } // always freed, also on error or cancel
            return out.copyOf(n) to sr
        } finally { ex.release() }
    }
}
