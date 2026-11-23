package com.abhi.musicapp.library

import android.content.Context

/**
 * Recycle Bin = "removed from library": songs the user asked this app to stop showing. App-private
 * preference keyed by MediaStore id with the removal time. This NEVER deletes, moves or edits the audio
 * file: the song stays on the phone, visible in every other app, and Restore shows it here again.
 * There is no automatic purge and no permanent-deletion path in the app.
 */
class BinStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("recycle_bin", Context.MODE_PRIVATE)

    data class Entry(val mediaId: Long, val removedAtMs: Long)

    fun entries(): List<Entry> = (prefs.getStringSet(KEY, emptySet()) ?: emptySet()).mapNotNull { s ->
        val p = s.split('|')
        val id = p.getOrNull(0)?.toLongOrNull()
        if (id == null) null else Entry(id, p.getOrNull(1)?.toLongOrNull() ?: 0L)
    }.sortedByDescending { it.removedAtMs }

    fun removedIds(): Set<Long> = entries().map { it.mediaId }.toSet()

    fun add(mediaId: Long, nowMs: Long = System.currentTimeMillis()) {
        if (mediaId < 0) return
        val cur = (prefs.getStringSet(KEY, emptySet()) ?: emptySet()).toMutableSet()
        cur.removeAll { it.startsWith("$mediaId|") }
        cur.add("$mediaId|$nowMs")
        prefs.edit().putStringSet(KEY, cur).apply()
    }

    fun restore(mediaId: Long) {
        val cur = (prefs.getStringSet(KEY, emptySet()) ?: emptySet()).toMutableSet()
        if (cur.removeAll { it.startsWith("$mediaId|") }) prefs.edit().putStringSet(KEY, cur).apply()
    }

    companion object {
        private const val KEY = "removed"
        /** Pure filter, unit-tested: removed ids drop out; everything else keeps its order. */
        fun filterRemoved(songs: List<Song>, removed: Set<Long>): List<Song> =
            if (removed.isEmpty()) songs else songs.filter { it.id !in removed }
    }
}
