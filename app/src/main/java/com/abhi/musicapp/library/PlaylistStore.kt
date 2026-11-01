package com.abhi.musicapp.library

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Local playlist persistence (app-private JSON file, no network, no file deletion).
 * Stores only song ids (MediaStore _ID) and playlist names. A song removed from the device simply won't resolve.
 */
data class Playlist(val name: String, val songIds: List<Long>)

class PlaylistStore(private val file: File) {
    constructor(context: Context) : this(File(context.filesDir, "playlists.json"))

    fun load(): List<Playlist> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val ids = o.getJSONArray("songIds")
                Playlist(o.getString("name"), (0 until ids.length()).map { ids.getLong(it) })
            }
        }.getOrDefault(emptyList())
    }

    private fun save(list: List<Playlist>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(JSONObject().put("name", p.name).put("songIds", JSONArray(p.songIds)))
        }
        file.writeText(arr.toString())
    }

    /** Returns false if the name is blank or already used (case-insensitive). */
    fun create(name: String): Boolean {
        val n = name.trim()
        val cur = load()
        if (n.isEmpty() || cur.any { it.name.equals(n, ignoreCase = true) }) return false
        save(cur + Playlist(n, emptyList()))
        return true
    }

    fun addSong(name: String, songId: Long): Boolean {
        val cur = load()
        val p = cur.firstOrNull { it.name == name } ?: return false
        if (songId in p.songIds) return true
        save(cur.map { if (it.name == name) it.copy(songIds = it.songIds + songId) else it })
        return true
    }

    fun removeSong(name: String, songId: Long) {
        save(load().map { if (it.name == name) it.copy(songIds = it.songIds - songId) else it })
    }

    /** Removes the playlist entry only. Never touches audio files. */
    fun delete(name: String) {
        save(load().filterNot { it.name == name })
    }
}
