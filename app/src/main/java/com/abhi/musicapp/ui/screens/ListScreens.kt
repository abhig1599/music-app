package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.components.ScreenTitle
import com.abhi.musicapp.ui.components.SongRow
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack

@Composable
fun FoldersScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Header("Folders", onBack)
        DemoData.folders.forEach { (name, count) ->
            SongRow(name, count, trailing = {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GrayText)
            }, onClick = {})
        }
    }
}

@Composable
fun PlaylistDetailScreen(name: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Header(name, onBack)
        Text("Songs in this playlist will appear here.",
            color = GrayText, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp))
    }
}

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.ArrowBack, contentDescription = "Back",
            tint = InkBlack, modifier = Modifier.clickable(onClick = onBack))
    }
    ScreenTitle(title)
}
