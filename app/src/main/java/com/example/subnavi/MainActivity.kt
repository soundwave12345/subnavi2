package com.example.subnavi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.subnavi.ui.navigation.Screen
import com.example.subnavi.ui.navigation.SubnaviBottomBar
import com.example.subnavi.ui.navigation.SubnaviNavHost
import com.example.subnavi.ui.theme.SubnaviTheme
import dagger.hilt.android.AndroidEntryPoint

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

@Composable
fun SubnaviMain() {
    val navController = rememberNavController()
    var isConnected by rememberSaveable { mutableStateOf(false) }
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Albums.route,
        Screen.Songs.route,
        Screen.Playlists.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                SubnaviBottomBar(navController)
            }
        }
    ) { innerPadding ->
        SubnaviNavHost(
            navController = navController,
            isConnected = isConnected,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
