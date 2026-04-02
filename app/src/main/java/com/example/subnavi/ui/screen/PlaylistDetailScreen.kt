package com.example.subnavi.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.example.subnavi.PlayerViewModel
import com.example.subnavi.PlaylistDetailViewModel
import com.example.subnavi.data.remote.SongDto
import com.example.subnavi.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    navController: NavHostController,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Playlist") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else if (state.error != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = viewModel::loadPlaylist) { Text("Retry") }
            }
        } else if (state.playlist != null) {
            val playlist = state.playlist
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    PlaylistHeader(playlistName = playlist!!.name, songCount = playlist.songCount)
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { playerViewModel.play(context, playlist!!.entry, 0) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Play")
                        }
                        OutlinedButton(
                            onClick = { playerViewModel.play(context, playlist!!.entry.shuffled(), 0) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Shuffle")
                        }
                    }
                }
                itemsIndexed(playlist!!.entry) { index, song ->
                    PlaylistSongRow(
                        song = song,
                        index = index + 1,
                        onClick = {
                            playerViewModel.play(context, playlist.entry, index)
                            navController.navigate(Screen.Player.route)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistHeader(playlistName: String, songCount: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlistName,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (songCount > 0) {
            Text(
                text = "$songCount songs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PlaylistSongRow(song: SongDto, index: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$index",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist ?: song.album ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        if (song.duration > 0) {
            Text(
                text = formatPlaylistDuration(song.duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatPlaylistDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}
