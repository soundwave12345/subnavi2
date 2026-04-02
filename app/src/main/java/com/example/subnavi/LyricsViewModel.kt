package com.example.subnavi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subnavi.data.remote.LyricsDto
import com.example.subnavi.data.repository.MusicRepository
import com.example.subnavi.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LyricsUiState(
    val lyrics: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LyricsUiState())
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    fun loadLyrics() {
        val song = playbackManager.playbackState.value.currentSong ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = musicRepository.getLyrics(artist = song.artist, title = song.title)
            val text = result.getOrNull()?.value
            _uiState.value = LyricsUiState(lyrics = text, isLoading = false)
        }
    }
}
