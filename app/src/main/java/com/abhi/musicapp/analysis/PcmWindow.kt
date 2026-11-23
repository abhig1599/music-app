package com.abhi.musicapp.analysis

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Pure decoder-output conversion. Byte range comes from MediaCodec.BufferInfo, not buffer position/limit. */
object PcmWindow {
    const val PCM16 = 2
    const val FLOAT32 = 4
    fun mono(buffer: ByteBuffer, offset: Int, size: Int, channels: Int, encoding: Int): FloatArray {
        require(channels in 1..32)
        val bytes = when (encoding) { PCM16 -> 2; FLOAT32 -> 4; else -> throw IllegalArgumentException("Unsupported PCM encoding") }
        require(offset >= 0 && size >= 0 && offset.toLong() + size <= buffer.capacity())
        val b = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        b.clear(); b.position(offset); b.limit(offset + size)
        val frames = size / (channels * bytes)
        return FloatArray(frames) {
            var sum = 0.0
            repeat(channels) {
                val v = if (encoding == PCM16) b.short / 32768f else b.float
                sum += if (v.isFinite()) v.coerceIn(-1f, 1f) else 0f
            }
            (sum / channels).toFloat()
        }
    }

    /**
     * Raw interleaved samples across all channels, for per-channel analysis. Values are NOT coerced:
     * non-finite or out-of-range samples survive so detectors can abstain on clipped/truncated decodes.
     */
    fun interleaved(buffer: ByteBuffer, offset: Int, size: Int, channels: Int, encoding: Int): FloatArray {
        require(channels in 1..32)
        val bytes = when (encoding) { PCM16 -> 2; FLOAT32 -> 4; else -> throw IllegalArgumentException("Unsupported PCM encoding") }
        require(offset >= 0 && size >= 0 && offset.toLong() + size <= buffer.capacity())
        val b = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        b.clear(); b.position(offset); b.limit(offset + size)
        return FloatArray(size / bytes) { if (encoding == PCM16) b.short / 32768f else b.float }
    }
}
