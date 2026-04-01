package com.example.subnavi.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.subnavi.data.remote.SongDto
import com.example.subnavi.data.remote.SubsonicApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackState(
    val currentSong: SongDto? = null,
    val queue: List<SongDto> = emptyList(),
    val queueIndex: Int = -1,
    val isPlaying: Boolean = false
)

@Singleton
class PlaybackManager @Inject constructor(
    private val apiClient: SubsonicApiClient
) {
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun getPlayer(context: Context): ExoPlayer {
        return player ?: ExoPlayer.Builder(context).build().also {
            player = it
            it.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val state = _playbackState.value
                    val newIndex = state.queue.indexOfFirst { song ->
                        mediaItem?.mediaId == song.id
                    }.takeIf { it >= 0 } ?: state.queueIndex
                    _playbackState.value = state.copy(
                        queueIndex = newIndex,
                        currentSong = state.queue.getOrNull(newIndex)
                    )
                }
            })
        }
    }

    fun play(songs: List<SongDto>, startIndex: Int = 0) {
        val exoPlayer = player ?: return
        val items = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(apiClient.getStreamUrl(song.id))
                .build()
        }
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItems(items, startIndex, 0)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        _playbackState.value = PlaybackState(
            currentSong = songs.getOrNull(startIndex),
            queue = songs,
            queueIndex = startIndex,
            isPlaying = true
        )
    }

    fun togglePlayPause() {
        val exoPlayer = player ?: return
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    fun skipNext() {
        player?.seekToNext()
    }

    fun skipPrevious() {
        player?.seekToPrevious()
    }

    fun release() {
        player?.release()
        player = null
    }
}
