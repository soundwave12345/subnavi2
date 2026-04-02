package com.example.subnavi.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.RemoteCastPlayer
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.subnavi.cast.SubnaviMediaItemConverter
import com.example.subnavi.data.remote.SongDto
import com.example.subnavi.data.remote.SubsonicApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackState(
    val currentSong: SongDto? = null,
    val queue: List<SongDto> = emptyList(),
    val queueIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isCasting: Boolean = false
)

@Singleton
class PlaybackManager @Inject constructor(
    val apiClient: SubsonicApiClient
) {
    companion object {
        private const val TAG = "PlaybackManager"
    }

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var castPlayer: CastPlayer? = null

    fun getPlayer(context: Context): Player {
        return castPlayer ?: buildPlayer(context).also { castPlayer = it }
    }

    private fun buildPlayer(context: Context): CastPlayer {
        val exoPlayer = ExoPlayer.Builder(context).build()

        val remotePlayer = RemoteCastPlayer.Builder(context)
            .setMediaItemConverter(SubnaviMediaItemConverter(apiClient))
            .build()

        val player = CastPlayer.Builder(context)
            .setLocalPlayer(exoPlayer)
            .setRemotePlayer(remotePlayer)
            .setTransferCallback(object : CastPlayer.TransferCallback {
                override fun transferState(
                    oldPlayer: Player,
                    newPlayer: Player
                ) {
                    Log.d(TAG, "Transfer: ${oldPlayer.deviceInfo} → ${newPlayer.deviceInfo}")
                    val state = _playbackState.value
                    if (state.queue.isEmpty()) return

                    // Preserve position
                    val pos = oldPlayer.currentPosition
                    val idx = oldPlayer.currentMediaItemIndex

                    // Rebuild media items on new player
                    val items = state.queue.map { song ->
                        buildMediaItem(song)
                    }
                    newPlayer.setMediaItems(items, idx, pos)
                    newPlayer.prepare()
                    newPlayer.playWhenReady = oldPlayer.playWhenReady

                    val isRemote = newPlayer.deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE
                    _playbackState.value = state.copy(isCasting = isRemote)
                }
            })
            .build()

        player.addListener(object : Player.Listener {
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

            override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
                val isRemote = deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE
                _playbackState.value = _playbackState.value.copy(isCasting = isRemote)
            }
        })

        return player
    }

    fun play(context: Context, songs: List<SongDto>, startIndex: Int = 0) {
        val player = getPlayer(context)
        val items = songs.map { song -> buildMediaItem(song) }
        player.clearMediaItems()
        player.setMediaItems(items, startIndex, 0)
        player.prepare()
        player.playWhenReady = true

        // Start the media service for notification controls
        try {
            val intent = Intent(context, SubnaviPlaybackService::class.java)
            context.startForegroundService(intent)
        } catch (_: Exception) {}

        _playbackState.value = PlaybackState(
            currentSong = songs.getOrNull(startIndex),
            queue = songs,
            queueIndex = startIndex,
            isPlaying = true,
            isCasting = _playbackState.value.isCasting
        )
    }

    private fun buildMediaItem(song: SongDto): MediaItem {
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(apiClient.getStreamUrl(song.id))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.coverArt?.let { Uri.parse(it) })
                    .build()
            )
            .build()
    }

    fun togglePlayPause() {
        val p = castPlayer ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun skipNext() {
        castPlayer?.seekToNext()
    }

    fun skipPrevious() {
        castPlayer?.seekToPrevious()
    }

    fun release() {
        castPlayer?.release()
        castPlayer = null
    }
}
