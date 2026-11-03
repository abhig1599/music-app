package com.abhi.musicapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.abhi.musicapp.ui.AppNav
import com.abhi.musicapp.ui.theme.MusicAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Sleep timer: after the 10 s exit countdown, close the app task (playback was already paused).
        val seen = com.abhi.musicapp.playback.SleepTimer.exitRequests.value
        lifecycleScope.launch {
            com.abhi.musicapp.playback.SleepTimer.exitRequests.collect { if (it != seen) finishAndRemoveTask() }
        }
        setContent {
            MusicAppTheme {
                com.abhi.musicapp.ui.FitDesignWidth { AppNav() }
            }
        }
    }
}
