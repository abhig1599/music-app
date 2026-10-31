package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.ui.components.SongRow
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.Orangs
import com.abhi.musicapp.ui.theme.SurfaceSoft

/** Search: field + results from the (currently demo) library. His hint text kept as placeholder style. */
@Composable
fun SearchScreen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = DemoData.songs.filter { query.isBlank() || it.first.contains(query, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back",
                tint = InkBlack, modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
            placeholder = { Text("Search what you need baby...", color = GrayText) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = GrayText) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceSoft,
                unfocusedContainerColor = SurfaceSoft,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Orangs,
            ),
        )
        Spacer(Modifier.height(16.dp))
        if (results.isEmpty()) {
            Text("Nothing found for \"$query\"", color = GrayText, fontSize = 14.sp)
        } else {
            results.forEach { (title, sub) ->
                SongRow(title, sub, trailing = {
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GrayText)
                }, onClick = {})
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Search matches the on-device library only.", color = GrayText, fontSize = 11.sp,
            fontWeight = FontWeight.Normal)
    }
}
