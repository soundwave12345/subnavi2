package com.example.subnavi.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Albums : Screen("albums")
    data object Songs : Screen("songs")
    data object Playlists : Screen("playlists")
    data object AlbumDetail : Screen("album/{albumId}") {
        fun createRoute(albumId: String) = "album/$albumId"
    }
    data object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist/$playlistId"
    }
    data object Player : Screen("player")
    data object Settings : Screen("settings")
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Albums,
    Screen.Songs,
    Screen.Playlists
)
