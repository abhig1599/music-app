package com.abhi.musicapp.library

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat

/**
 * Reads the on-device audio library via MediaStore.
 * Permission model verified:
 * https://developer.android.com/training/data-storage/shared/media#storage-permissions
 */
object LibraryRepository {

    fun hasAudioPermission(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Opens this app's system settings page. The app cannot change OS permissions itself. */
    fun openAppSettings(context: Context) {
        val i = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", context.packageName, null))
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }

    /**
     * Rescan: asks Android's media scanner to index audio files found in the standard public folders
     * (Music, Download, Podcasts, Audiobooks, Documents; depth 4), then calls [onDone] on the main thread
     * so the caller can re-query MediaStore. Under scoped storage the app can only list what the OS lets it
     * see; anything it cannot list is not scanned here (the MediaStore re-query still picks up whatever the
     * OS has already indexed). Returns the number of files handed to the scanner.
     */
    fun rescan(context: Context, onDone: () -> Unit): Int {
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        if (!hasAudioPermission(context)) { main.post(onDone); return 0 }
        val exts = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "wma", "amr", "mid")
        val found = mutableListOf<String>()
        fun walk(f: java.io.File, depth: Int) {
            val kids = try { f.listFiles() } catch (_: Exception) { null } ?: return
            for (k in kids) {
                if (k.isDirectory) { if (depth < 4 && !k.name.startsWith(".")) walk(k, depth + 1) }
                else if (k.extension.lowercase() in exts) found.add(k.absolutePath)
            }
        }
        listOf(android.os.Environment.DIRECTORY_MUSIC, android.os.Environment.DIRECTORY_DOWNLOADS,
            android.os.Environment.DIRECTORY_PODCASTS, android.os.Environment.DIRECTORY_AUDIOBOOKS,
            android.os.Environment.DIRECTORY_DOCUMENTS).forEach {
            try { walk(android.os.Environment.getExternalStoragePublicDirectory(it), 0) } catch (_: Exception) {}
        }
        if (found.isEmpty()) { main.post(onDone); return 0 }
        var left = found.size
        android.media.MediaScannerConnection.scanFile(context.applicationContext, found.toTypedArray(), null) { _, _ ->
            main.post { left--; if (left <= 0) onDone() }
        }
        return found.size
    }

    /** Truthful result text for a scan: counts come from the before/after MediaStore song ids, never from files handed to the scanner. */
    fun scanSummary(hadPermission: Boolean, before: Set<Long>, after: Set<Long>): String {
        if (!hadPermission) return "Music permission is off, can't scan"
        val added = (after - before).size
        val removed = (before - after).size
        fun songs(n: Int) = if (n == 1) "1 song" else "$n songs"
        return when {
            added > 0 && removed > 0 -> "${songs(added)} added, ${removed} no longer found"
            added > 0 -> "${songs(added)} added"
            removed > 0 -> "${songs(removed)} no longer found"
            else -> "No new songs found"
        }
    }

    fun querySongs(context: Context): List<Song> {
        if (!hasAudioPermission(context)) return emptyList()
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val hasRel = Build.VERSION.SDK_INT >= 29
        val projection = (listOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
        ) + (if (hasRel) listOf(MediaStore.Audio.Media.RELATIVE_PATH) else emptyList())).toTypedArray()
        val songs = mutableListOf<Song>()
        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} ASC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val relCol = if (hasRel) cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH) else -1
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                songs += Song(
                    id = id,
                    title = cursor.getString(titleCol) ?: "Unknown",
                    artist = cursor.getString(artistCol) ?: "Unknown artist",
                    durationMs = cursor.getLong(durCol),
                    uri = ContentUris.withAppendedId(collection, id),
                    folder = if (relCol >= 0) (cursor.getString(relCol) ?: "").trimEnd('/') else "",
                )
            }
        }
        return songs
    }

    /** Groups songs by MediaStore folder (RELATIVE_PATH, API 29+; empty on older devices). Name = last path segment. */
    fun folders(songs: List<Song>): List<Pair<String, List<Song>>> =
        songs.filter { it.folder.isNotEmpty() }
            .groupBy { it.folder }
            .map { (path, list) -> path.substringAfterLast('/') to list }
            .sortedBy { it.first.lowercase() }
}
