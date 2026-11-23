package com.abhi.musicapp.analysis

import android.content.Context
import android.net.Uri
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory map of detected content ends, keyed by file identity (uri + size + last-modified + duration).
 * uri+duration alone is NOT identity: a replaced file of the same length would reuse a stale content end, so
 * provider metadata versions the entry. An identity with missing metadata is unusable and abstains. Not
 * persisted - a stale detection must never outlive the process that validated it.
 */
object TailCache {
    data class FileIdentity(val uri: String, val sizeBytes: Long, val lastModifiedMs: Long, val durationMs: Long)

    private val entries = ConcurrentHashMap<FileIdentity, Long>()
    private const val MAX_ENTRIES = 64

    /** Provider-supplied version evidence; null (abstain) when size or mtime is unavailable. */
    fun fileIdentity(c: Context, uri: Uri, durationMs: Long): FileIdentity? {
        if (durationMs <= 0L) return null
        val size: Long; val mtimeSec: Long
        try {
            c.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.SIZE, android.provider.MediaStore.MediaColumns.DATE_MODIFIED),
                null, null, null,
            )?.use { cur ->
                if (!cur.moveToFirst()) return null
                size = if (cur.isNull(0)) -1L else cur.getLong(0)
                mtimeSec = if (cur.isNull(1)) -1L else cur.getLong(1)
            } ?: return null
        } catch (e: Exception) { return null } // column unsupported on this provider/API: abstain
        if (size <= 0L || mtimeSec <= 0L) return null
        return FileIdentity(uri.toString(), size, mtimeSec * 1000L, durationMs)
    }

    fun contentEndMs(id: FileIdentity): Long? = entries[id]

    fun put(id: FileIdentity, contentEndMs: Long) {
        if (contentEndMs !in 1..id.durationMs) return
        if (entries.size >= MAX_ENTRIES) entries.clear()
        entries[id] = contentEndMs
    }

    fun clear() = entries.clear()
}
