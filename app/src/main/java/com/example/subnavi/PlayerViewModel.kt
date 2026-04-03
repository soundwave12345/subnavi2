package com.example.subnavi

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subnavi.data.remote.PlaylistDto
import com.example.subnavi.data.remote.SongDto
import com.example.subnavi.data.repository.MusicRepository
import com.example.subnavi.playback.PlaybackManager
import com.example.subnavi.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val playbackManager: PlaybackManager,
    private val musicRepository: MusicRepository
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    private val _playlists = MutableStateFlow<List<PlaylistDto>>(emptyList())
    val playlists: StateFlow<List<PlaylistDto>> = _playlists.asStateFlow()

    private val _isStarred = MutableStateFlow(false)
    val isStarred: StateFlow<Boolean> = _isStarred.asStateFlow()

    fun play(context: Context, songs: List<SongDto>, startIndex: Int = 0) {
        playbackManager.play(context, songs, startIndex)
        checkStarred(songs.getOrNull(startIndex))
    }

    fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }

    fun skipNext() {
        playbackManager.skipNext()
        val state = playbackState.value
        val nextSong = state.queue.getOrNull(state.queueIndex + 1)
        checkStarred(nextSong)
    }

    fun skipPrevious() {
        playbackManager.skipPrevious()
        val state = playbackState.value
        val prevSong = state.queue.getOrNull(state.queueIndex - 1)
        checkStarred(prevSong)
    }

    fun toggleStar() {
        val song = playbackState.value.currentSong ?: return
        val wasStarred = _isStarred.value
        viewModelScope.launch {
            val result = if (wasStarred) {
                musicRepository.unstarSong(song.id)
            } else {
                musicRepository.starSong(song.id)
            }
            if (result.isSuccess) {
                _isStarred.value = !wasStarred
            }
        }
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            musicRepository.getPlaylists().onSuccess {
                _playlists.value = it
            }
        }
    }

    fun addToPlaylist(playlistId: String, songId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            musicRepository.addToPlaylist(playlistId, songId).onSuccess {
                onSuccess()
            }
        }
    }

    fun createPlaylistAndAdd(name: String, songId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            musicRepository.createPlaylist(name).onSuccess { playlist ->
                musicRepository.addToPlaylist(playlist.id, songId).onSuccess {
                    loadPlaylists()
                    onSuccess()
                }
            }
        }
    }

    private fun checkStarred(song: SongDto?) {
        song?.let {
            _isStarred.value = it.starred == true
        }
    }
}
