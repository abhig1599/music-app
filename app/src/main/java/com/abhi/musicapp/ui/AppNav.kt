package com.abhi.musicapp.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.abhi.musicapp.ui.screens.DownloadsScreen
import com.abhi.musicapp.ui.screens.FoldersScreen
import com.abhi.musicapp.ui.screens.HomeScreen
import com.abhi.musicapp.ui.screens.OnboardingScreen
import com.abhi.musicapp.ui.screens.PlayerScreen
import com.abhi.musicapp.ui.screens.PlaylistDetailScreen
import com.abhi.musicapp.ui.screens.SearchScreen
import com.abhi.musicapp.ui.screens.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val PLAYER = "player"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val DOWNLOADS = "downloads"
    const val FOLDERS = "folders"
    const val PLAYLIST_DETAIL = "playlist/{name}"
    fun playlistDetail(name: String) = "playlist/$name"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    // TODO(persistence): remember onboarding-done and start at HOME when completed.
    NavHost(navController = nav, startDestination = Routes.ONBOARDING) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onDone = {
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
        composable(Routes.SEARCH) { SearchScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.DOWNLOADS) { DownloadsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.FOLDERS) { FoldersScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.PLAYLIST_DETAIL) { entry ->
            PlaylistDetailScreen(
                name = entry.arguments?.getString("name") ?: "Playlist",
                onBack = { nav.popBackStack() },
            )
        }
    }
}
