package com.abhi.musicapp.library

import android.content.Context
import android.net.Uri
import java.io.ByteArrayInputStream

/**
 * Reads lyrics that are stored INSIDE an MP3 file (ID3v2 "USLT" frame). Nothing is downloaded and no lyrics service is used.
 * Other formats (m4a, flac, ogg) and files without that frame return null, and the app shows an empty state instead.
 */
object EmbeddedLyrics {
    fun read(ctx: Context, uri: Uri): String? = try {
        ctx.contentResolver.openInputStream(uri)?.use { ins ->
            val head = ByteArray(10)
            if (ins.read(head) < 10 || head[0] != 'I'.code.toByte() || head[1] != 'D'.code.toByte() || head[2] != '3'.code.toByte()) return null
            val ver = head[3].toInt()
            if (ver != 3 && ver != 4) return null
            val size = syncsafe(head, 6)
            if (size <= 0 || size > 4_000_000) return null
            val tag = ByteArray(size)
            var got = 0
            while (got < size) { val n = ins.read(tag, got, size - got); if (n < 0) break; got += n }
            parse(tag, got, ver)
        }
    } catch (e: Exception) { null }

    private fun syncsafe(b: ByteArray, o: Int) = (b[o].toInt() and 0x7f shl 21) or (b[o + 1].toInt() and 0x7f shl 14) or (b[o + 2].toInt() and 0x7f shl 7) or (b[o + 3].toInt() and 0x7f)
    private fun plain(b: ByteArray, o: Int) = (b[o].toInt() and 0xff shl 24) or (b[o + 1].toInt() and 0xff shl 16) or (b[o + 2].toInt() and 0xff shl 8) or (b[o + 3].toInt() and 0xff)

    internal fun parse(tag: ByteArray, len: Int, ver: Int): String? {
        var p = 0
        while (p + 10 <= len) {
            val id = String(tag, p, 4, Charsets.ISO_8859_1)
            if (id[0].code == 0) return null
            val fs = if (ver == 4) syncsafe(tag, p + 4) else plain(tag, p + 4)
            if (fs <= 0 || p + 10 + fs > len) return null
            if (id == "USLT") return decodeUslt(tag, p + 10, fs)
            p += 10 + fs
        }
        return null
    }

    private fun decodeUslt(b: ByteArray, off: Int, len: Int): String? {
        if (len < 5) return null
        val enc = b[off].toInt()
        var i = off + 4 // encoding byte + 3-byte language
        val end = off + len
        val cs = when (enc) { 1 -> Charsets.UTF_16; 2 -> Charsets.UTF_16BE; 3 -> Charsets.UTF_8; else -> Charsets.ISO_8859_1 }
        val wide = enc == 1 || enc == 2
        // skip the content descriptor (terminated by 0x00 or 0x0000)
        while (i < end) { if (wide) { if (i + 1 < end && b[i].toInt() == 0 && b[i + 1].toInt() == 0) { i += 2; break }; i += 2 } else { if (b[i].toInt() == 0) { i += 1; break }; i += 1 } }
        if (i >= end) return null
        val text = String(b, i, end - i, cs).trim { it <= ' ' || it == '\uFEFF' }
        return text.ifBlank { null }
    }
}
