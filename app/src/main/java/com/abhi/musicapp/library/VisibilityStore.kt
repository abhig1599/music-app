package com.abhi.musicapp.library

import android.content.Context

/**
 * Which music folders the user has hidden inside this app (Settings > Visibility). App-private preference,
 * keyed by the full MediaStore folder path (RELATIVE_PATH), so two folders with the same last name stay distinct.
 * Hiding only filters what the app shows. It never deletes, moves or edits a file, and unhiding restores the songs.
 */
class VisibilityStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("visibility", Context.MODE_PRIVATE)

    fun hidden(): Set<String> = prefs.getStringSet(KEY, emptySet())?.toSet() ?: emptySet()

    fun setHidden(folder: String, hide: Boolean) {
        if (folder.isEmpty()) return
        val cur = hidden().toMutableSet()
        if (hide) cur.add(folder) else cur.remove(folder)
        prefs.edit().putStringSet(KEY, cur).apply()
    }

    companion object {
        private const val KEY = "hidden_folders"
        /** Songs with no folder info (older Android) can't be hidden and always show. */
        fun filterVisible(songs: List<Song>, hidden: Set<String>): List<Song> =
            if (hidden.isEmpty()) songs else songs.filter { it.folder.isEmpty() || it.folder !in hidden }
    }
}
