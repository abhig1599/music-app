package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.theme.GeistMono
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.PureWhite
import com.abhi.musicapp.ui.theme.SurfaceSoft

// Shared page header from Figma (Recycle bin 47-14803, Read Now 59-2982, both 390x844, fill Bg):
// Frame 81 at x22 y41, 351 wide, vertical gap 24: back circle 44, title row 351 x 37 (gap 24), content.
@Composable
internal fun FigmaPage(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}, titleSize: androidx.compose.ui.unit.TextUnit = 28.sp, chevronBack: Boolean = false, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 22.dp, end = 17.dp, top = 41.dp)) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(if (chevronBack) androidx.compose.ui.graphics.Color(0xFFF0F0F0) else PureWhite).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            if (chevronBack) {
            androidx.compose.foundation.Canvas(Modifier.size(width = 6.dp, height = 12.dp)) { // Figma Download Detail: chevron vector 6x12, 2px stroke, Gray (hex inferred)
                val st = 2.dp.toPx()
                val col = androidx.compose.ui.graphics.Color(0xFF6B6B6B)
                drawLine(col, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(0f, size.height / 2), st, androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(col, androidx.compose.ui.geometry.Offset(0f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width, size.height), st, androidx.compose.ui.graphics.StrokeCap.Round)
            }
            } else Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = InkBlack, modifier = Modifier.size(20.dp)) // arrow not verified against Figma on this frame
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().height(37.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = titleSize, fontWeight = FontWeight.Bold, color = InkBlack, modifier = Modifier.weight(1f))
            trailing()
        }
        Spacer(Modifier.height(24.dp))
        content()
        Spacer(Modifier.height(24.dp))
    }
}

data class BinRow(val name: String, val daysLeft: Int)

/**
 * Recycle Bin. List rows: 351x72, white, r10, pad 10, left group 292x50 gap 13 (50x50 thumb + text), undo icon 24x24; list gap 6.
 * Shipped meaning (settled 2026-11-23, main): "removed from library" - an app-only hide via BinStore. Nothing is auto-purged,
 * so Empty Bin stays disabled and the design's "N days left" countdown (which would imply auto-deletion) is replaced by the
 * removal date. Restore shows the song again. The ONLY permanent path is the explicit per-row "Delete forever" (user's
 * three-dot Delete request, 2026-11-24): it asks in-app, then Android shows its own system consent dialog (API 30+);
 * the app can never delete a file silently.
 * [fixture] rows remain design samples for render tests only.
 * [fixtureEntries] is render-only: it supplies the SHIPPED row path (real BinStore entries + resolved songs)
 * so the production bin renders in tests without MediaStore; rows show the removal date, never a countdown.
 */
@Composable
fun RecycleBinScreen(onBack: () -> Unit, fixture: List<BinRow> = emptyList(),
    fixtureEntries: List<Pair<com.abhi.musicapp.library.BinStore.Entry, com.abhi.musicapp.library.Song?>>? = null) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var tick by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val isFixture = fixture.isNotEmpty()
    var real by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<Pair<com.abhi.musicapp.library.BinStore.Entry, com.abhi.musicapp.library.Song?>>>(fixtureEntries ?: emptyList()) }
    // Per-row permanent deletion: in-app confirmation, then Android's own consent dialog (API 30+). No silent deletes.
    var pendingDelete by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.abhi.musicapp.library.BinStore.Entry?>(null) }
    var deleteNote by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val deleteLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        pendingDelete?.let { entry ->
            if (handleDeleteResult(ctx, res.resultCode, entry.mediaId)) tick++
            else if (res.resultCode == android.app.Activity.RESULT_OK) deleteNote = "Android confirmed, but the file is still listed, so the song stays in the bin."
            // cancelled or denied in the system dialog: bin item stays, nothing to say
        }
        pendingDelete = null
    }
    androidx.compose.runtime.LaunchedEffect(tick) {
        if (fixtureEntries != null) {
            real = fixtureEntries
        } else if (!isFixture) {
            val byId = com.abhi.musicapp.library.LibraryRepository.queryAllSongs(ctx).associateBy { it.id }
            real = com.abhi.musicapp.library.BinStore(ctx).entries().map { it to byId[it.mediaId] }
        }
    }
    Box(Modifier.fillMaxSize()) {
    FigmaPage("Recycle Bin", onBack, titleSize = 30.sp, chevronBack = true, trailing = {
        // "Empty Bin" pill from the design: stays disabled - permanently deleting files is not a shipped action.
        Box(Modifier.alpha(0.5f).clip(RoundedCornerShape(8.dp)).background(SurfaceSoft).padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text("Empty Bin", fontFamily = GeistMono, fontSize = 14.sp, color = InkBlack)
        }
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isFixture) fixture.forEach { row ->
                Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(RoundedCornerShape(10.dp)).background(com.abhi.musicapp.ui.theme.FigmaWhite).border(1.dp, androidx.compose.ui.graphics.Color(0xFFD9D9D9), RoundedCornerShape(10.dp)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceSoft))
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(row.name, fontSize = 18.sp, color = InkBlack)
                        Text("${row.daysLeft} days left", fontFamily = GeistMono, fontSize = 15.sp, color = InkBlack)
                    }
                    Icon(Icons.Filled.Undo, "Restore", tint = InkBlack, modifier = Modifier.size(24.dp))
                }
            } else {
                real.forEach { (entry, song) ->
                    val date = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(entry.removedAtMs))
                    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(RoundedCornerShape(10.dp)).background(com.abhi.musicapp.ui.theme.FigmaWhite).border(1.dp, androidx.compose.ui.graphics.Color(0xFFD9D9D9), RoundedCornerShape(10.dp)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceSoft))
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song?.title ?: "Song no longer on this phone", fontSize = 18.sp, color = InkBlack, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text("Removed $date - file untouched", fontFamily = GeistMono, fontSize = 13.sp, color = GrayText)
                        }
                        Icon(Icons.Filled.Delete, "Delete forever", tint = GrayText, modifier = Modifier.size(22.dp).clickable { pendingDelete = entry })
                        Spacer(Modifier.width(14.dp))
                        Icon(Icons.Filled.Undo, "Restore", tint = InkBlack, modifier = Modifier.size(24.dp).clickable {
                            com.abhi.musicapp.library.BinStore(ctx).restore(entry.mediaId); tick++
                        })
                    }
                }
                if (real.isEmpty()) Text("Nothing in the bin", fontFamily = GeistMono, fontSize = 13.sp, color = GrayText, modifier = Modifier.padding(top = 8.dp))
                else Text("Removed songs only stop showing in this app. Their files stay on your phone unless you choose Delete forever on a row; nothing is ever deleted automatically.", fontFamily = GeistMono, fontSize = 11.sp, color = GrayText, modifier = Modifier.padding(top = 8.dp))
                deleteNote?.let { note -> Text(note, fontFamily = GeistMono, fontSize = 11.sp, color = Orangs, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
    }

    // In-app confirmation for the only permanent-deletion path. On pre-Android-11 phones permanent deletion is unavailable.
    pendingDelete?.let { entry ->
        val canDelete = android.os.Build.VERSION.SDK_INT >= 30
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0x99000000)).clickable { pendingDelete = null })
        Box(Modifier.align(androidx.compose.ui.Alignment.Center).padding(horizontal = 40.dp).clip(RoundedCornerShape(12.dp)).background(com.abhi.musicapp.ui.theme.FigmaWhite).padding(20.dp)) {
            Column {
                Text("Delete forever?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = InkBlack)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (canDelete) "The audio file will be permanently deleted from this phone, not just hidden from this app. Android asks you to confirm once more. This cannot be undone."
                    else "Permanently deleting files needs Android 11 or newer. On this phone the song can only be hidden from this app.",
                    fontSize = 13.sp, color = InkBlack)
                Spacer(Modifier.height(14.dp))
                Row {
                    if (canDelete) {
                        Text("Delete forever", fontSize = 14.sp, color = Orangs, modifier = Modifier.clickable {
                            try {
                                deleteLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(requestFileDeletion(ctx, entry.mediaId).intentSender).build())
                            } catch (e: Exception) {
                                deleteNote = "Couldn't ask Android to delete this file. The song stays in the bin."
                                pendingDelete = null
                            }
                        })
                        Spacer(Modifier.width(24.dp))
                    }
                    Text("Cancel", fontSize = 14.sp, color = InkBlack, modifier = Modifier.clickable { pendingDelete = null })
                }
            }
        }
    }
    }
}

/** Android's own consent dialog for permanently deleting one audio file (API 30+). The app cannot delete silently.
 * Per developer.android.com/reference/android/provider/MediaStore#createDeleteRequest: the uri must be hosted by
 * MediaStore.AUTHORITY and reference one item by _ID (ContentUris.withAppendedId does exactly that), and the delete
 * has completely finished by the time the activity result arrives - RESULT_OK means granted AND done. */
internal fun requestFileDeletion(ctx: android.content.Context, mediaId: Long): android.app.PendingIntent =
    android.provider.MediaStore.createDeleteRequest(
        ctx.contentResolver,
        listOf(android.content.ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId)))

/** True while MediaStore still lists this audio id (stale ids from already-missing files query as absent). */
internal fun mediaStoreHasAudio(ctx: android.content.Context, mediaId: Long): Boolean =
    ctx.contentResolver.query(
        android.content.ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId),
        arrayOf(android.provider.MediaStore.Audio.Media._ID), null, null, null)?.use { it.moveToFirst() } == true

/** Handles the system consent result: the bin entry drops only when the user approved AND the provider really no
 * longer lists the item. Cancellation, denial, or a still-present provider entry all preserve the bin item. */
internal fun handleDeleteResult(ctx: android.content.Context, resultCode: Int, mediaId: Long,
    stillExists: (android.content.Context, Long) -> Boolean = ::mediaStoreHasAudio): Boolean {
    if (resultCode != android.app.Activity.RESULT_OK) return false
    if (stillExists(ctx, mediaId)) return false
    com.abhi.musicapp.library.BinStore(ctx).restore(mediaId)
    return true
}

/**
 * Read Now (Reader). Cards: white, r16, 351 wide; API card 351x168 (pad 14/16/14/10, gap 13; title row 20, API row 321x46, button 321x48), empty card 351x267.52, gap 24 (Design panel). Button radius/fonts estimated.
 * The Reader is optional/undefined: Add API is disabled, no key is accepted or stored, no AI call exists.
 */
@Composable
fun ReadNowScreen(onBack: () -> Unit, fixtureApiName: String? = null, designComparison: Boolean = false) {
    val mono = GeistMono
    FigmaPage("Read Now", onBack, titleSize = 30.sp, chevronBack = true) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            if (fixtureApiName != null) {
                Column(Modifier.fillMaxWidth().height(168.dp).clip(RoundedCornerShape(16.dp)).background(com.abhi.musicapp.ui.theme.FigmaWhite)
                    .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 10.dp)) {
                    Text("API Collection", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = InkBlack, modifier = Modifier.height(20.dp))
                    Spacer(Modifier.height(13.dp))
                    Row(Modifier.height(46.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(fixtureApiName, fontFamily = mono, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = InkBlack) // Geist Mono Medium 16 (read)
                            Text("...hello", fontSize = 15.sp, color = GrayText) // Helvetica Neue Regular 14 Gray (read)
                        }
                        Icon(Icons.Outlined.Delete, "Delete", tint = GrayText, modifier = Modifier.size(20.dp)) // Figma: outlined vector 18x20, stroke Gray 1.5 round; Material outlined glyph is an approximation
                    }
                    Spacer(Modifier.height(13.dp))
                    ReadButton("Add API", dark = false, enabled = false)
                }
            }
            Column(Modifier.fillMaxWidth().height(267.52.dp).clip(RoundedCornerShape(16.dp)).background(com.abhi.musicapp.ui.theme.FigmaWhite).padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 10.dp)) {
                Text("API Collection", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = InkBlack)
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No API added yet", fontSize = 15.sp, color = InkBlack) // Helvetica Neue Regular 14 (read)
                }
                ReadButton(if (designComparison) "Add API" else "Add API (unavailable)", dark = true, enabled = false, designComparison = designComparison)
            }
            // The design carries this explanatory note under the cards; reproduced as drawn.
            if (designComparison) {
                Text("Now I want to add a line that you can only add one API at a time, and if there are some issue with the Read Now feature, you can simply need to delete the API and add a new one to get it running.",
                fontFamily = mono, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = GrayText) // Geist Mono Medium 12 Gray (read)
            } else {
                Text("Read Now is not available yet. No API key is stored for this feature.", fontFamily = mono, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = GrayText)
            }
        }
    }
}

@Composable
private fun ReadButton(label: String, dark: Boolean, enabled: Boolean = true, designComparison: Boolean = false) {
    Box(Modifier.alpha(if (enabled || (dark && designComparison)) 1f else 0.5f).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp))
        .background(if (dark) InkBlack else com.abhi.musicapp.ui.theme.GrayLight), contentAlignment = Alignment.Center) {
        Text(label, fontFamily = GeistMono, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = if (dark) com.abhi.musicapp.ui.theme.FigmaWhite else GrayText)
    }
}
