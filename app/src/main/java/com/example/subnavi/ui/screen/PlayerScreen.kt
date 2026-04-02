package com.example.subnavi.ui.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.example.subnavi.CastViewModel
import com.example.subnavi.LyricsViewModel
import com.example.subnavi.PlayerViewModel
import com.example.subnavi.SubnaviApp
import com.example.subnavi.cast.CastHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    navController: NavHostController,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    lyricsViewModel: LyricsViewModel = hiltViewModel(),
    castViewModel: CastViewModel = hiltViewModel()
) {
    val state by playerViewModel.playbackState.collectAsState()
    val lyricsState by lyricsViewModel.uiState.collectAsState()
    val castConnected by castViewModel.isConnected.collectAsState()
    var showLyrics by remember { mutableStateOf(false) }
    val song = state.currentSong
    val context = LocalContext.current

    // Initialize Cast context once
    LaunchedEffect(Unit) {
        castViewModel.init(context)
    }

    LaunchedEffect(song?.id, showLyrics) {
        if (song != null && showLyrics) {
            lyricsViewModel.loadLyrics()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        TopAppBar(
            title = { Text("Now Playing") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // Cast button — uses MediaRouter directly
                IconButton(onClick = {
                    castViewModel.showRouteSelector(context)
                }) {
                    Icon(
                        Icons.Default.Cast,
                        contentDescription = "Cast",
                        tint = if (castConnected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (song != null) {
                    IconButton(onClick = { showLyrics = !showLyrics }) {
                        Icon(
                            Icons.Default.Lyrics,
                            contentDescription = "Lyrics",
                            tint = if (showLyrics) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                // Cast: load song when connected
                if (castConnected && song != null) {
                    LaunchedEffect(song.id, castConnected) {
                        CastHelper.loadSong(
                            context,
                            SubnaviApp.instance.playbackManager.apiClient,
                            song
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (song != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.large)
            ) {
                AsyncImage(
                    model = song.coverArt,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (showLyrics) {
                    val context = LocalContext.current
                    val exoPlayer = SubnaviApp.instance.playbackManager.getPlayer(context)

                    if (lyricsState.isSynced && lyricsState.lines.isNotEmpty()) {
                        SyncedLyricsOverlay(
                            lines = lyricsState.lines,
                            exoPlayer = exoPlayer,
                            getCurrentLineIndex = lyricsViewModel::getCurrentLineIndex
                        )
                    } else {
                        // Plain lyrics fallback
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f))
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (lyricsState.isLoading) {
                                CircularProgressIndicator()
                            } else if (lyricsState.rawLyrics != null) {
                                Text(
                                    text = lyricsState.rawLyrics!!,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                    lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.8
                                )
                            } else {
                                Text(
                                    "No lyrics available",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(song.artist, song.album).joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            val context = LocalContext.current
            val exoPlayer = SubnaviApp.instance.playbackManager.getPlayer(context)
            PlayerSeekBar(exoPlayer)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = playerViewModel::skipPrevious) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = playerViewModel::togglePlayPause) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(56.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = playerViewModel::skipNext) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No track playing", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun SyncedLyricsOverlay(
    lines: List<com.example.subnavi.LyricsLine>,
    exoPlayer: ExoPlayer,
    getCurrentLineIndex: (Long) -> Int
) {
    val listState = rememberLazyListState()
    var currentLine by remember { mutableIntStateOf(-1) }

    // Poll position every 200ms
    val infiniteTransition = rememberInfiniteTransition(label = "lyricsPoll")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lyricsProgress"
    )

    // Update current line index based on playback position
    LaunchedEffect(progress) {
        val pos = exoPlayer.currentPosition
        currentLine = getCurrentLineIndex(pos)
    }

    // Auto-scroll to current line
    LaunchedEffect(currentLine) {
        if (currentLine >= 0) {
            listState.animateScrollToItem(currentLine, scrollOffset = -200)
        }
    }

    if (lines.isNotEmpty()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item { Spacer(modifier = Modifier.height(120.dp)) }

            itemsIndexed(lines) { index, line ->
                val isActive = index == currentLine
                val alpha = if (currentLine < 0) 0.7f
                    else if (isActive) 1f
                    else 0.35f

                Text(
                    text = line.text,
                    color = Color.White.copy(alpha = alpha),
                    fontSize = if (isActive) 18.sp else 15.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(120.dp)) }
        }
    }
}

@Composable
private fun PlayerSeekBar(exoPlayer: ExoPlayer) {
    var position by remember { mutableFloatStateOf(0f) }
    val duration = exoPlayer.duration.coerceAtLeast(1).toFloat()

    // Poll position every 500ms for smooth slider
    val infiniteTransition = rememberInfiniteTransition(label = "seekBar")
    val ticker by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "seekTicker"
    )

    LaunchedEffect(ticker) {
        if (duration > 0) {
            position = (exoPlayer.currentPosition / duration).coerceIn(0f, 1f)
        }
    }

    androidx.compose.runtime.DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPositionDiscontinuity(reason: Int) {
                position = (exoPlayer.currentPosition / duration).coerceIn(0f, 1f)
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatMs(exoPlayer.currentPosition),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = position,
            onValueChange = { newPos ->
                position = newPos
                exoPlayer.seekTo((newPos * duration).toLong())
            },
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        Text(
            text = formatMs(exoPlayer.duration.coerceAtLeast(0)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "$m:${s.toString().padStart(2, '0')}"
}
