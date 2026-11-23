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
    /** How a rescan ended. [folderRoots] is how many of the listed public folders could be opened at all. */
    enum class ScanStatus { COMPLETE, NO_PERMISSION, FOLDERS_UNREADABLE, SCANNER_FAILED, TIMED_OUT }
    data class ScanResult(val status: ScanStatus, val filesHandedOver: Int = 0, val folderRoots: Int = 0)

    fun rescan(context: Context, onDone: (ScanResult) -> Unit) {
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        if (!hasAudioPermission(context)) { main.post { onDone(ScanResult(ScanStatus.NO_PERMISSION)) }; return }
        Thread {
            val exts = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "wma", "amr", "mid")
            val found = mutableListOf<String>()
            var rootsOpened = 0
            fun walk(f: java.io.File, depth: Int, root: Boolean) {
                val kids = try { f.listFiles() } catch (_: Exception) { null }
                if (kids == null) return
                if (root) rootsOpened++
                for (k in kids) {
                    if (k.isDirectory) { if (depth < 4 && !k.name.startsWith(".")) walk(k, depth + 1, false) }
                    else if (k.extension.lowercase() in exts) found.add(k.absolutePath)
                }
            }
            val roots = listOf(android.os.Environment.DIRECTORY_MUSIC, android.os.Environment.DIRECTORY_DOWNLOADS,
                android.os.Environment.DIRECTORY_PODCASTS, android.os.Environment.DIRECTORY_AUDIOBOOKS,
                android.os.Environment.DIRECTORY_DOCUMENTS)
            roots.forEach { try { walk(android.os.Environment.getExternalStoragePublicDirectory(it), 0, true) } catch (_: Exception) {} }
            if (rootsOpened == 0) { main.post { onDone(ScanResult(ScanStatus.FOLDERS_UNREADABLE, 0, 0)) }; return@Thread }
            if (found.isEmpty()) { main.post { onDone(ScanResult(ScanStatus.COMPLETE, 0, rootsOpened)) }; return@Thread }
            var left = found.size
            var failed = 0
            try {
                android.media.MediaScannerConnection.scanFile(context.applicationContext, found.toTypedArray(), null) { _, uri ->
                    main.post {
                        if (uri == null) failed++
                        left--
                        if (left <= 0) onDone(ScanResult(if (failed == found.size) ScanStatus.SCANNER_FAILED else ScanStatus.COMPLETE, found.size, rootsOpened))
                    }
                }
            } catch (_: Exception) { main.post { onDone(ScanResult(ScanStatus.SCANNER_FAILED, found.size, rootsOpened)) } }
        }.start()
    }

    /** Truthful result text for a scan: counts come from the before/after MediaStore song ids, never from files handed to the scanner. */
    fun scanSummary(status: ScanStatus, before: Set<Long>, after: Set<Long>): String {
        when (status) {
            ScanStatus.NO_PERMISSION -> return "Music permission is off, can't scan"
            ScanStatus.FOLDERS_UNREADABLE -> return "Couldn't open the music folders. Showing what Android already indexed"
            ScanStatus.SCANNER_FAILED -> return "Scan failed. Showing what Android already indexed"
            ScanStatus.TIMED_OUT -> return "Scan didn't finish. Showing what Android has indexed so far"
            ScanStatus.COMPLETE -> {}
        }
        val added = (after - before).size
        val removed = (before - after).size
        fun songs(n: Int) = if (n == 1) "1 song" else "$n songs"
        return when {
            added > 0 && removed > 0 -> "${songs(added)} added, $removed no longer found"
            added > 0 -> "${songs(added)} added"
            removed > 0 -> "${songs(removed)} no longer found"
            else -> "No new songs found"
        }
    }

    /** Songs the app shows: everything MediaStore lists minus folders hidden in Settings > Visibility. Every list, search and queue uses this. */
    fun querySongs(context: Context): List<Song> = BinStore.filterRemoved(VisibilityStore.filterVisible(queryAllSongs(context), VisibilityStore(context).hidden()), BinStore(context).removedIds())

    /** Unfiltered library, only for Settings > Visibility (so a hidden folder stays listed and can be shown again). */
    fun queryAllSongs(context: Context): List<Song> {
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
