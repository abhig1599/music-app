package com.abhi.musicapp.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.abhi.musicapp.playback.PlayerManager
import com.abhi.musicapp.ui.screens.RecycleBinScreen
import com.abhi.musicapp.ui.screens.ReadNowScreen
import com.abhi.musicapp.ui.screens.DownloadsScreen
import com.abhi.musicapp.ui.screens.EqualiserScreen
import com.abhi.musicapp.ui.screens.FoldersScreen
import com.abhi.musicapp.ui.screens.HomeScreen
import com.abhi.musicapp.ui.screens.OnboardingScreen
import com.abhi.musicapp.ui.screens.PlayerScreen
import com.abhi.musicapp.ui.screens.PlaylistDetailScreen
import com.abhi.musicapp.ui.screens.SearchScreen
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import com.abhi.musicapp.ui.screens.BackupScreen
import com.abhi.musicapp.ui.screens.RingtoneMakerScreen
import com.abhi.musicapp.ui.screens.SettingsScreen
import com.abhi.musicapp.ui.screens.ThemesScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val PLAYER = "player"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val EQUALISER = "equaliser"
    const val THEMES = "themes"
    const val RINGTONE = "ringtone"
    const val BACKUP = "backup"
    const val DOWNLOADS = "downloads"
    const val FOLDERS = "folders"
    const val RECYCLE = "recycle"
    const val READ_NOW = "readnow"
    const val PLAYLIST_DETAIL = "playlist/{name}"
    fun playlistDetail(name: String) = "playlist/${android.net.Uri.encode(name)}"
}

private const val PREFS = "music_app_prefs"
private const val KEY_ONBOARDING_DONE = "onboarding_done"

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val context = LocalContext.current

    LaunchedEffect(Unit) { PlayerManager.connect(context) }

    val onboardingDone = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_ONBOARDING_DONE, false)

    NavHost(
        navController = nav,
        startDestination = if (onboardingDone) Routes.HOME else Routes.ONBOARDING,
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onDone = {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
                nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenPlayer = { nav.navigate(Routes.PLAYER) },
                onOpenSearch = { nav.navigate(Routes.SEARCH) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenDownloads = { nav.navigate(Routes.DOWNLOADS) },
                onOpenFolders = { nav.navigate(Routes.FOLDERS) },
                onOpenPlaylist = { name -> nav.navigate(Routes.playlistDetail(name)) },
            )
        }
        composable(Routes.PLAYER) { PlayerScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SEARCH) { SearchScreen(onBack = { nav.popBackStack() }, onOpenPlayer = { nav.navigate(Routes.PLAYER) }) }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onRowClick = { label ->
                    when (label) {
                        "Recycle bin" -> nav.navigate(Routes.RECYCLE)
                        "Read Now" -> nav.navigate(Routes.READ_NOW)
                        "Equaliser" -> nav.navigate(Routes.EQUALISER)
                        "Theme", "Accent color" -> nav.navigate(Routes.THEMES)
                        "Set ringtone" -> nav.navigate(Routes.RINGTONE)
                        "Google Drive backup" -> nav.navigate(Routes.BACKUP)
                        "Floating player" -> context.startActivity(
                            Intent(AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        )
                        else -> Unit
                    }
                },
            )
        }
        composable(Routes.RECYCLE) { RecycleBinScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.READ_NOW) { ReadNowScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.EQUALISER) { EqualiserScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.THEMES) { ThemesScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.RINGTONE) {
            RingtoneMakerScreen(
                onBack = { nav.popBackStack() },
                onOpenSystemConsent = {
                    context.startActivity(
                        Intent(AndroidSettings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                },
            )
        }
        composable(Routes.BACKUP) { BackupScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.DOWNLOADS) { DownloadsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.FOLDERS) { FoldersScreen(onBack = { nav.popBackStack() }, onOpenPlayer = { nav.navigate(Routes.PLAYER) }) }
        composable(Routes.PLAYLIST_DETAIL) { entry ->
            PlaylistDetailScreen(
                name = entry.arguments?.getString("name") ?: "Playlist",
                onBack = { nav.popBackStack() },
                onOpenPlayer = { nav.navigate(Routes.PLAYER) },
            )
        }
    }
}
