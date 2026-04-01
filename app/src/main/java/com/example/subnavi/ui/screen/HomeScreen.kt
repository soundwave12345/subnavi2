package com.example.subnavi.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.example.subnavi.HomeUiState
import com.example.subnavi.HomeViewModel
import com.example.subnavi.data.remote.AlbumDto
import com.example.subnavi.data.remote.PlaylistDto
import com.example.subnavi.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
    } else if (state.error != null && state.recentAlbums.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = viewModel::loadData) { Text("Retry") }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            AlbumSection(
                title = "Recently Played",
                albums = state.recentAlbums,
                onAlbumClick = { album ->
                    navController.navigate(Screen.AlbumDetail.createRoute(album.id))
                }
            )
            AlbumSection(
                title = "Recently Added",
                albums = state.newestAlbums,
                onAlbumClick = { album ->
                    navController.navigate(Screen.AlbumDetail.createRoute(album.id))
                }
            )
            PlaylistSection(
                playlists = state.playlists,
                onPlaylistClick = { playlist ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlist.id))
                }
            )
        }
    }
}

@Composable
fun AlbumSection(
    title: String,
    albums: List<AlbumDto>,
    onAlbumClick: (AlbumDto) -> Unit
) {
    if (albums.isEmpty()) return
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(albums) { album ->
                AlbumCard(album = album, onClick = { onAlbumClick(album) })
            }
        }
    }
}

@Composable
fun AlbumCard(album: AlbumDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AsyncImage(
            model = album.coverArt,
            contentDescription = album.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop
        )
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
        Text(
            text = album.artist ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun PlaylistSection(
    playlists: List<PlaylistDto>,
    onPlaylistClick: (PlaylistDto) -> Unit
) {
    if (playlists.isEmpty()) return
    Column {
        Text(
            text = "Playlists",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            playlists.forEach { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlaylistClick(playlist) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(playlist.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${playlist.songCount} songs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
