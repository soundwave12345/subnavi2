package com.example.subnavi.ui.navigation

import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.subnavi.OnboardingScreen
import com.example.subnavi.ui.screen.AlbumDetailScreen
import com.example.subnavi.ui.screen.AlbumsScreen
import com.example.subnavi.ui.screen.HomeScreen
import com.example.subnavi.ui.screen.PlaylistDetailScreen
import com.example.subnavi.ui.screen.PlayerScreen
import com.example.subnavi.ui.screen.PlaylistsScreen
import com.example.subnavi.ui.screen.SettingsScreen
import com.example.subnavi.ui.screen.SongsScreen

@Composable
fun SubnaviNavHost(
    navController: NavHostController,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val startDestination = if (isConnected) Screen.Home.route else Screen.Onboarding.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onConnected = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Albums.route) { AlbumsScreen(navController) }
        composable(Screen.Songs.route) { SongsScreen(navController) }
        composable(Screen.Playlists.route) { PlaylistsScreen(navController) }
        composable(Screen.Player.route) { PlayerScreen(navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
        composable(Screen.AlbumDetail.route) {
            AlbumDetailScreen(navController)
        }
        composable(Screen.PlaylistDetail.route) {
            PlaylistDetailScreen(navController)
        }
    }
}
