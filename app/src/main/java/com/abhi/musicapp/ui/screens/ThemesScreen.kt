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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.components.ScreenTitle
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.Orangs
import com.abhi.musicapp.ui.theme.PureWhite
import com.abhi.musicapp.ui.theme.SurfaceSoft
import com.abhi.musicapp.ui.theme.ThemeState

/** Theme picker: Light / Dark / System + accent swatches. Applies live. */
@Composable
fun ThemesScreen(onBack: () -> Unit) {
    val mode by ThemeState.mode
    val accent by ThemeState.accent

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable(onClick = onBack))
        }
        ScreenTitle("Theme")

        Row(Modifier.padding(horizontal = 20.dp)) {
            ThemeState.Mode.entries.forEach { m ->
                val active = mode == m
                Text(
                    m.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = if (active) PureWhite else MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { ThemeState.mode.value = m }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Accent", modifier = Modifier.padding(horizontal = 20.dp),
            fontSize = 15.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground)
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val swatches = listOf(
                Orangs to "Orangs",
                Color(0xFF0066FF) to "Blue",
                Color(0xFF00A86B) to "Green",
                Color(0xFFE91E63) to "Pink",
            )
            swatches.forEach { (color, label) ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { ThemeState.accent.value = color },
                    contentAlignment = Alignment.Center,
                ) {
                    if (accent == color) Text("✓", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            "Dark variant is our proposal - the Figma file is light-only.",
            fontSize = 12.sp, color = GrayText, modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}
