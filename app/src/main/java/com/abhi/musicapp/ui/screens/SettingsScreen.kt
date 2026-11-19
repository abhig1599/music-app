package com.abhi.musicapp.ui.screens

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.abhi.musicapp.ui.theme.GrayLight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.components.ScreenTitle
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.Orangs
import com.abhi.musicapp.ui.theme.SurfaceSoft

/**
 * Settings with the 7-category structure mapped from the reference analysis:
 * Library / Playback / Sound / Appearance / Storage & background / Permissions / About.
 * Rows are stubs until each feature lands; "Planned" marks designed-but-not-built rows.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onRowClick: (String) -> Unit = {}, fixture: Boolean = false, startExpanded: Boolean = false, previewFolders: List<Pair<String, String>>? = null) {
    // fixture=true shows the design's sample values (07H 27M, permission on). Real app: no usage tracker or grant state yet, so placeholders/off.
    // Figma "Settings" (node 47-14503, 390x1508): content column x22 y41, 351 wide, gap 24:
    // back button 44x44 white circle, title row 37, then cards (white, r16, pad L/R 14 T 16 B 10, gap 6).
    // Card heights read: play time 82, permission 166, updates 221, collapsed visibility 58,
    // expanded visibility 441, Read Now 58, Recycle Bin 58. Inner text sizes/fonts are ESTIMATES.
    val mono = com.abhi.musicapp.ui.theme.GeistMono
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 22.dp, end = 17.dp)) {
        Spacer(Modifier.height(41.dp))
        Box(
            Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(com.abhi.musicapp.ui.theme.PureWhite)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = InkBlack, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.height(24.dp))
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = InkBlack, modifier = Modifier.height(37.dp))
        Spacer(Modifier.height(24.dp))

        FigmaCard(Modifier.heightIn(min = 82.dp)) {
            // Play-time stat: needs real listening statistics; no tracker exists yet, so this is a render placeholder.
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.heightIn(min = 56.dp)) {
                Text(if (fixture) "07H 27M" else "--H --M", fontFamily = mono, fontSize = 28.sp, color = InkBlack)
                Text("Play Time", fontFamily = mono, fontSize = 11.sp, color = InkBlack)
            }
        }
        Spacer(Modifier.height(6.dp))
        AccordionCard("Permission", startExpanded) {
            Spacer(Modifier.height(13.dp))
            val ctx = androidx.compose.ui.platform.LocalContext.current
            var tick by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
            androidx.compose.runtime.DisposableEffect(ctx) {
                val owner = ctx as? androidx.lifecycle.LifecycleOwner
                val obs = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) tick++ }
                owner?.lifecycle?.addObserver(obs)
                onDispose { owner?.lifecycle?.removeObserver(obs) }
            }
            // Shipped: switch shows the REAL OS grant state (read-only); tapping opens system settings. Fixture: Figma sample values.
            val audioOn = if (fixture) true else tick.let { com.abhi.musicapp.library.LibraryRepository.hasAudioPermission(ctx) }
            val notifOn = if (fixture) false else tick.let { com.abhi.musicapp.library.LibraryRepository.hasNotificationPermission(ctx) }
            val open: (() -> Unit)? = if (fixture) null else ({ com.abhi.musicapp.library.LibraryRepository.openAppSettings(ctx) })
            ToggleRow("Music and audio", if (fixture) "Needed to read your songs" else "Needed to read your songs. Tap to open system settings", audioOn, open)
            Spacer(Modifier.height(13.dp))
            ToggleRow("Notifications", if (fixture) "Playback controls in the shade" else "Notification permission. Tap to open system settings", notifOn, open)
        }
        Spacer(Modifier.height(6.dp))
        AccordionCard("Check for updates", startExpanded) {
            Spacer(Modifier.height(13.dp))
            Text("Current Version", fontFamily = mono, fontSize = 13.sp, color = InkBlack)
            val sctx = androidx.compose.ui.platform.LocalContext.current
            val versionName = runCatching { sctx.packageManager.getPackageInfo(sctx.packageName, 0).versionName }.getOrNull() ?: "0.1.1"
            Text(versionName, fontFamily = mono, fontSize = 11.sp, color = GrayText)
            Spacer(Modifier.height(13.dp))
            WideButton("Update check not available yet", dark = false, enabled = false)
            Spacer(Modifier.height(13.dp))
            if (fixture) WideButton("Update available (design state only)", dark = true, enabled = true)
        }
        Spacer(Modifier.height(6.dp))
        AccordionCard("Visibility", startExpanded) {
            run {
                Spacer(Modifier.height(13.dp))
                // Design sample paths only in fixtures. Shipped state lists real MediaStore folders (read-only: no hide/show logic exists yet).
                val vctx = androidx.compose.ui.platform.LocalContext.current
                var realFolders by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<Pair<String, String>>>(previewFolders ?: emptyList()) }
                if (!fixture && previewFolders == null) androidx.compose.runtime.LaunchedEffect(Unit) {
                    val all = com.abhi.musicapp.library.LibraryRepository.queryAllSongs(vctx)
                    realFolders = all.filter { it.folder.isNotEmpty() }.map { it.folder }.distinct().sorted().map { it.substringAfterLast('/') to it }
                }
                val sample = listOf("Recording" to ".../DCIM/Audio/Recording", "Download" to ".../DCIM/Audio/Downloads",
                    "Tunes" to ".../DCIM/Audio/Tunes", "Call Recording" to ".../DCIM/Audio/Recording",
                    "Y-music" to ".../DCIM/Audio/Y-music")
                if (!fixture && realFolders.isEmpty()) Text(
                    if (!com.abhi.musicapp.library.LibraryRepository.hasAudioPermission(vctx)) "Music permission is off, so folders can't be listed. Use the Permission card above."
                    else "No music folders found on this device.", fontSize = 12.sp, color = GrayText)
                val visibleFolders = if (fixture) sample else realFolders
                val vstore = androidx.compose.runtime.remember { com.abhi.musicapp.library.VisibilityStore(vctx) }
                var hiddenSet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(vstore.hidden()) }
                visibleFolders.forEach { (n, path) ->
                    val shown = fixture || path !in hiddenSet
                    Row(
                        Modifier.padding(vertical = 4.dp).fillMaxWidth().testTag("visibility:$path")
                            .semantics { stateDescription = if (shown) "Shown" else "Hidden" }
                            .then(if (fixture) Modifier else Modifier.clickable { vstore.setHidden(path, shown); hiddenSet = vstore.hidden(); if (shown) com.abhi.musicapp.playback.PlayerManager.pruneUpcomingHidden(hiddenSet) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val boxShape = RoundedCornerShape(5.dp)
                        Box(Modifier.size(20.dp).clip(boxShape)
                            .background(if (shown && !fixture) InkBlack else androidx.compose.ui.graphics.Color.Transparent)
                            .border(1.5.dp, if (shown && !fixture) InkBlack else GrayText, boxShape),
                            contentAlignment = Alignment.Center) {
                            if (shown && !fixture) Icon(Icons.Filled.Check, null, tint = com.abhi.musicapp.ui.theme.PureWhite, modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(n, fontFamily = mono, fontSize = 12.sp, color = InkBlack)
                            Text(path, fontFamily = mono, fontSize = 11.sp, color = GrayText)
                        }
                    }
                }
                if (!fixture && visibleFolders.isNotEmpty()) Text("Checked folders are shown. Unchecking hides a folder's songs in this app only. No files are changed or deleted.", fontSize = 11.sp, color = GrayText, fontFamily = mono)
                Spacer(Modifier.height(13.dp))
                // Figma shows two Save buttons (first disabled-looking, second enabled-looking). Visual only, no save logic.
                // Design fixture only: the shipped state has no hide/show logic, so it draws no Save buttons.
                if (fixture) {
                    Spacer(Modifier.height(13.dp))
                    WideButton("Save", dark = false, enabled = false)
                    Spacer(Modifier.height(13.dp))
                    WideButton("Save", dark = false, enabled = true)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        AccordionCard("Song Transitions", startExpanded) {
            Spacer(Modifier.height(13.dp))
            val tctx = androidx.compose.ui.platform.LocalContext.current
            var xOn by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(!fixture && com.abhi.musicapp.playback.Crossfader.enabled(tctx)) }
            var xSecs by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(if (fixture) 4f else com.abhi.musicapp.playback.Crossfader.seconds(tctx).toFloat()) }
            ToggleRow("Crossfade", if (xOn) "The next song starts before this one ends" else "Off. Songs play one after the other", xOn) {
                xOn = !xOn
                if (!fixture) com.abhi.musicapp.playback.Crossfader.setEnabled(tctx, xOn)
            }
            Spacer(Modifier.height(13.dp))
            Text("Duration: ${xSecs.toInt()} seconds", fontFamily = mono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (xOn) InkBlack else GrayText)
            androidx.compose.material3.Slider(
                value = xSecs, onValueChange = { xSecs = it.toInt().toFloat(); if (!fixture) com.abhi.musicapp.playback.Crossfader.setSeconds(tctx, xSecs.toInt()) },
                valueRange = 1f..12f, steps = 10, enabled = xOn,
                colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = com.abhi.musicapp.ui.theme.Orangs, activeTrackColor = com.abhi.musicapp.ui.theme.Orangs),
            )
            Spacer(Modifier.height(6.dp))
            Text("AutoMix is not available. Crossfade is a plain overlap for a set time. It has not been heard on a real phone yet.", fontFamily = mono, fontSize = 11.sp, color = GrayText)
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(6.dp))
        NavCard("Read Now") { onRowClick("Read Now") }
        Spacer(Modifier.height(6.dp))
        NavCard("Recycle Bin") { onRowClick("Recycle bin") }
        Spacer(Modifier.height(6.dp))
        AboutCard(startExpanded)

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * FAQ-style section: tap the header row to expand downward / collapse. Each section opens independently
 * (no single-open rule chosen). Spring height change is skipped when system animations are off.
 */
@Composable
private fun AccordionCard(title: String, startExpanded: Boolean, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    var open by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(startExpanded) }
    val reduce = com.abhi.musicapp.ui.reducedMotion()
    val rot by androidx.compose.animation.core.animateFloatAsState(
        if (open) 180f else 0f,
        if (reduce) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween(220), label = "chevron")
    FigmaCard(if (reduce) Modifier else Modifier.animateContentSize(androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .clickable(role = androidx.compose.ui.semantics.Role.Button) { open = !open }
                .semantics { stateDescription = if (open) "Expanded" else "Collapsed" }
                .testTag("accordion:$title"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardTitle(title, Modifier.weight(1f))
            Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = InkBlack, modifier = Modifier.graphicsLayer { rotationZ = rot })
        }
        if (open) content()
    }
}

@Composable
private fun FigmaCard(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(com.abhi.musicapp.ui.theme.PureWhite)
            .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 10.dp),
        content = content,
    )
}

@Composable
private fun CardTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = InkBlack, modifier = modifier)

@Composable
private fun ToggleRow(title: String, detail: String, on: Boolean, onClick: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier) {
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = com.abhi.musicapp.ui.theme.GeistMono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkBlack)
            Text(detail, fontFamily = com.abhi.musicapp.ui.theme.GeistMono, fontSize = 11.sp, color = GrayText)
        }
        // Display-only switch (reflects OS state when onClick != null; it does not change permissions).
        Box(
            Modifier.size(34.dp, 20.dp).clip(RoundedCornerShape(10.dp))
                .background(if (on) InkBlack else com.abhi.musicapp.ui.theme.SurfaceSoft),
            contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
        ) { Box(Modifier.padding(2.dp).size(16.dp).clip(androidx.compose.foundation.shape.CircleShape).background(com.abhi.musicapp.ui.theme.PureWhite)) }
    }
}

@Composable
private fun WideButton(label: String, dark: Boolean, enabled: Boolean = true) {
    Box(
        Modifier.alpha(if (enabled) 1f else 0.6f).fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp))
            .background(if (dark) InkBlack else com.abhi.musicapp.ui.theme.SurfaceSoft),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontFamily = com.abhi.musicapp.ui.theme.GeistMono, fontSize = 12.sp, color = if (dark) com.abhi.musicapp.ui.theme.PureWhite else InkBlack) }
}

@Composable
private fun NavCard(label: String, onClick: () -> Unit) {
    FigmaCard(Modifier.height(58.dp).clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxHeight()) {
            CardTitle(label, Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, null, tint = InkBlack)
        }
    }
}

@Composable
private fun SettingsCategory(
    title: String,
    rows: List<Pair<String, String>>,
    onRowClick: (String) -> Unit = {},
) {
    Text(title, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Orangs)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceSoft),
    ) {
        rows.forEachIndexed { i, (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRowClick(label) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, fontSize = 15.sp, color = InkBlack, modifier = Modifier.weight(1f))
                Text(
                    value,
                    fontSize = 13.sp,
                    color = GrayText,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 150.dp),
                )
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GrayText)
            }
            if (i < rows.lastIndex) Spacer(
                Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 16.dp)
                    .background(com.abhi.musicapp.ui.theme.GrayLight)
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

/** Temporary target for every About / policy link until real pages exist: his portfolio (his instruction, 2026-11-02). */
private const val ABOUT_LINK = "https://abhicreative.world"

@Composable
private fun AboutCard(startExpanded: Boolean) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val open = {
        runCatching {
            ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(ABOUT_LINK)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Unit
    }
    AccordionCard("About", startExpanded) {
        // Labels say what they are: no real privacy policy or terms page exists yet, all rows open the portfolio.
        // Privacy policy and Terms of service do not exist yet: shown as not published and not tappable (no stand-in legal text).
        listOf("Privacy policy: not published yet", "Terms of service: not published yet").forEach { label ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontFamily = com.abhi.musicapp.ui.theme.GeistMono, fontSize = 12.sp, color = com.abhi.musicapp.ui.theme.GrayText, modifier = Modifier.weight(1f))
            }
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = open), verticalAlignment = Alignment.CenterVertically) {
            Text("About the maker (portfolio)", fontFamily = com.abhi.musicapp.ui.theme.GeistMono, fontSize = 12.sp, color = InkBlack, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, null, tint = InkBlack)
        }
    }
}
