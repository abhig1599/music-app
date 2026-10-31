package com.abhi.musicapp.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.Orangs
import com.abhi.musicapp.ui.theme.PureWhite
import com.abhi.musicapp.ui.theme.SurfaceSoft

/**
 * Onboarding, 3 steps matching his Figma:
 *  1. Language  2. Permissions  3. Optional reader ("Read Now").
 * Step 2 copy is the corrected wording (not his "Permission1/2" placeholders).
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        when (step) {
            1 -> LanguageStep()
            2 -> PermissionsStep()
            else -> ReaderStep()
        }

        Column {
            if (step > 1) {
                Text(
                    "Back",
                    color = GrayText,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .clickable { step -= 1 },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Orangs)
                    .clickable { if (step < 3) step += 1 else onDone() }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    if (step < 3) "Continue" else "Get started",
                    color = PureWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun LanguageStep() {
    var selected by remember { mutableStateOf("English") }
    Column {
        Text("Choose your language", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = InkBlack)
        Spacer(Modifier.height(6.dp))
        Text("You can change this later in Settings.", fontSize = 13.sp, color = GrayText)
        Spacer(Modifier.height(20.dp))
        listOf("English", "Hindi").forEach { lang ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceSoft)
                    .clickable { selected = lang }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(lang, fontSize = 15.sp, color = InkBlack, modifier = Modifier.weight(1f))
                if (selected == lang) Text("✓", color = Orangs, fontWeight = FontWeight.Bold)
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
        Text("Permissions", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = InkBlack)
        Spacer(Modifier.height(6.dp))
        Text("Only what the player needs. Nothing runs in the background without these.",
            fontSize = 13.sp, color = GrayText)
        Spacer(Modifier.height(20.dp))

        PermissionRow(
            title = "Music and audio",
            body = "Allow access to find music already on your phone. You can also choose files manually.",
            granted = audioGranted,
            onAllow = { audioLauncher.launch(audioPermission) },
        )
        PermissionRow(
            title = "Notifications",
            body = "For download progress and library updates. Play controls keep working even if you skip this.",
            granted = notifGranted,
            onAllow = {
                if (Build.VERSION.SDK_INT >= 33) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    notifGranted = true
                }
            },
        )
    }
}

@Composable
private fun PermissionRow(title: String, body: String, granted: Boolean, onAllow: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceSoft)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                color = InkBlack, modifier = Modifier.weight(1f))
            Text(
                if (granted) "Allowed" else "Allow",
                color = if (granted) GrayText else Orangs,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (granted) SurfaceSoft else PureWhite)
                    .clickable(enabled = !granted, onClick = onAllow)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(body, fontSize = 12.sp, color = GrayText)
    }
}

@Composable
private fun ReaderStep() {
    var readerEnabled by remember { mutableStateOf(false) }
    Column {
        Text("Read Now", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = InkBlack)
        Spacer(Modifier.height(6.dp))
        Text("Optional. Turns on the reading view for lyrics and documents in the app.",
            fontSize = 13.sp, color = GrayText)
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceSoft)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Enable Read Now", fontSize = 15.sp, color = InkBlack, fontWeight = FontWeight.Medium)
                Text("Off by default. Nothing is uploaded anywhere.", fontSize = 12.sp, color = GrayText)
            }
            Switch(
                checked = readerEnabled,
                onCheckedChange = { readerEnabled = it },
                colors = SwitchDefaults.colors(checkedTrackColor = Orangs),
            )
        }
        Spacer(Modifier.height(20.dp))
        // TODO: link real T&C / privacy pages once they exist (no documents published yet).
        Text(
            "By continuing you acknowledge that you have read and agree to our T&C and privacy policy.",
            fontSize = 11.sp,
            color = GrayText,
        )
    }
}
