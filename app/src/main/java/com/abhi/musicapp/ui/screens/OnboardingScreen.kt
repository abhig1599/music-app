package com.abhi.musicapp.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.theme.Bg
import com.abhi.musicapp.ui.theme.GrayLight
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.Orangs
import com.abhi.musicapp.ui.theme.PureWhite
import com.abhi.musicapp.ui.theme.SurfaceSoft

private val Mono = FontFamily.Monospace

/**
 * Onboarding rebuilt from his Figma frames Step 1 / Step 2 / Step3 (390x844):
 * a rounded bottom sheet on the grey Bg, bold title, monospaced body, full-width
 * orange button. Sheet top: 339dp (steps 1-2), 252dp (step 3), measured from
 * 68%-zoom captures (/0.678) - estimated, not Figma inspect values.
 *
 * Content deviations, all flagged in DECISIONS.md:
 *  - his list under "Choose your language..." shows placeholder sort labels; only
 *    real languages (English, Hindi) are kept.
 *  - his "Permission1/2 details in 1-2 lines" are placeholders; real permission
 *    names are used in his toggle-card layout.
 *  - Step 3 keeps his reader fields but disabled (reader behaviour undecided) and
 *    his consent sentence is replaced because no T&C documents exist.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit, initialStep: Int = 1) {
    var step by remember { mutableIntStateOf(initialStep) }
    val sheetTop = if (step == 3) 252.dp else 339.dp

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        Spacer(Modifier.height(sheetTop))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(SurfaceSoft)
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.size(width = 40.dp, height = 4.dp).clip(CircleShape).background(GrayLight))
            Spacer(Modifier.height(if (step == 3) 22.dp else 30.dp))
            Box(Modifier.fillMaxWidth().weight(1f)) {
                when (step) {
                    1 -> LanguageStep()
                    2 -> PermissionsStep()
                    else -> ReaderStep(onSkip = onDone)
                }
            }
            if (step < 3) {
                OrangeButton("Next") { step += 1 }
                Spacer(Modifier.height(25.dp))
            } else {
                OrangeButton("Get Started", onClick = onDone)
                Spacer(Modifier.height(10.dp))
                Text(
                    "By tapping \"Get Started\" you continue into the app. Terms and privacy documents are not published yet.",
                    fontFamily = Mono, fontSize = 8.sp, color = GrayText,
                    textAlign = TextAlign.Center, lineHeight = 12.sp,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun OrangeButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(47.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Orangs)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontFamily = Mono, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = InkBlack)
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = InkBlack,
        modifier = Modifier.fillMaxWidth())
}

@Composable
private fun LanguageStep() {
    var selected by remember { mutableStateOf("English") }
    Column {
        SheetTitle("Choose your language...")
        Spacer(Modifier.height(24.dp))
        listOf("English", "Hindi").forEach { lang ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(39.dp)
                    .clickable(enabled = lang == "English") { selected = lang }
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(lang, fontFamily = Mono, fontSize = 13.sp,
                    color = if (selected == lang) Orangs else if (lang == "English") InkBlack else GrayText,
                    modifier = Modifier.weight(1f))
                if (selected == lang) Text("✓", color = Orangs, fontSize = 16.sp)
                if (lang != "English") Text("not available yet", fontFamily = Mono, fontSize = 10.sp, color = GrayText)
            }
        }
    }
}

@Composable
private fun PermissionsStep() {
    var audioGranted by remember { mutableStateOf(false) }
    var notifGranted by remember { mutableStateOf(false) }

    val audioPermission = if (Build.VERSION.SDK_INT >= 33)
        Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> audioGranted = granted }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifGranted = granted }

    Column {
        SheetTitle("Permissions")
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, GrayLight, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            PermissionRow(
                "Music and audio", "Find music already on your phone. Files can also be picked manually.",
                audioGranted,
            ) { audioLauncher.launch(audioPermission) }
            Spacer(Modifier.height(14.dp))
            PermissionRow(
                "Notifications", "Download progress and library updates. Optional.",
                notifGranted,
            ) {
                if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else notifGranted = true
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, detail: String, on: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = Mono, fontSize = 13.sp, color = InkBlack)
            Text(detail, fontFamily = Mono, fontSize = 10.sp, color = GrayText, lineHeight = 13.sp)
        }
        Spacer(Modifier.size(8.dp))
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 22.dp)
                .clip(CircleShape)
                .background(if (on) InkBlack else Color(0xFFE3E6EC))
                .clickable(enabled = !on, onClick = onToggle)
                .padding(3.dp),
            contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(Modifier.size(16.dp).clip(CircleShape).background(PureWhite))
        }
    }
}

@Composable
private fun ReaderStep(onSkip: () -> Unit) {
    Column {
        SheetTitle("Enable Reader")
        Spacer(Modifier.height(14.dp))
        Text(
            "Enter your Google Gemini API key to enable the \"Read Now\" feature.\nYou can create an API key in Google AI Studio. Pricing and limits are set by Google and not verified here.",
            fontFamily = Mono, fontSize = 10.sp, color = InkBlack, lineHeight = 13.sp,
        )
        Spacer(Modifier.height(14.dp))
        Text("Create Google Gemini API", fontFamily = Mono, fontSize = 10.sp,
            color = Color(0xFF0000FF), modifier = Modifier.padding(start = 4.dp))
        Spacer(Modifier.height(14.dp))
        DisabledField("API Name")
        Spacer(Modifier.height(10.dp))
        DisabledField("API Key (....hmwg)")
        Spacer(Modifier.height(8.dp))
        Text("Reader setup is not connected yet; fields are disabled until its behaviour is decided.",
            fontSize = 9.sp, color = GrayText, fontFamily = Mono)
        Spacer(Modifier.weight(1f, fill = false).height(40.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth().height(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE3E3E1))
                .clickable(onClick = onSkip),
            contentAlignment = Alignment.Center,
        ) { Text("Skip & Get Started", fontFamily = Mono, fontSize = 13.sp, color = InkBlack) }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun DisabledField(hint: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth().height(49.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, GrayLight, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) { Text(hint, fontFamily = Mono, fontSize = 13.sp, color = GrayText) }
}
