package com.example.subnavi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.subnavi.data.local.ServerConfig
import com.example.subnavi.data.repository.AuthRepository
import com.example.subnavi.PlayerViewModel
import com.example.subnavi.playback.PlaybackManager
import com.example.subnavi.playback.PlaybackState
import com.example.subnavi.ui.navigation.Screen
import com.example.subnavi.ui.navigation.SubnaviBottomBar
import com.example.subnavi.ui.navigation.SubnaviNavHost
import com.example.subnavi.ui.screen.MiniPlayer
import com.example.subnavi.ui.theme.SubnaviTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SubnaviTheme {
                SubnaviMain()
            }
        }
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _hasCredentials = MutableStateFlow<Boolean?>(null)
    val hasCredentials: StateFlow<Boolean?> = _hasCredentials.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.serverConfig.collect { config ->
                _hasCredentials.value =
                    config.url.isNotBlank() &&
                    config.username.isNotEmpty() &&
                    config.password.isNotEmpty()
            }
        }
    }
}

@Composable
fun SubnaviMain() {
    val viewModel: MainViewModel = hiltViewModel()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val isConnected by viewModel.hasCredentials.collectAsState()
    val playbackState by playerViewModel.playbackState.collectAsState()

    // Wait for credentials check before rendering navigation
    if (isConnected == null) {
        return
    }
    val connected = isConnected!! // safe after null check
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Albums.route,
        Screen.Songs.route,
        Screen.Playlists.route
    )
    val showMiniPlayer = playbackState.currentSong != null &&
        currentRoute != Screen.Player.route &&
        currentRoute != Screen.Onboarding.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                SubnaviBottomBar(navController)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            SubnaviNavHost(
                navController = navController,
                isConnected = connected,
                modifier = Modifier.fillMaxSize()
            )

            // MiniPlayer floats above everything, anchored to bottom
            if (showMiniPlayer) {
                MiniPlayer(
                    state = playbackState,
                    onPlayPause = playerViewModel::togglePlayPause,
                    onNext = playerViewModel::skipNext,
                    onClick = { navController.navigate(Screen.Player.route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
