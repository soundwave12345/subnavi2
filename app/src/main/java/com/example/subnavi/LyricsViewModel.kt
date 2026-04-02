package com.example.subnavi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subnavi.data.remote.LrcLibClient
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
        val artist = song.artist
        val title = song.title

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Try lrclib.net first
            val lyrics = tryFetchFromLrcLib(artist, title)
                ?: tryFetchFromLrcLib(null, title) // retry without artist
                ?: tryFetchFromSubsonic(artist, title)

            _uiState.value = LyricsUiState(lyrics = lyrics, isLoading = false)
        }
    }

    private suspend fun tryFetchFromLrcLib(artist: String?, title: String): String? {
        return try {
            if (artist != null) {
                val result = LrcLibClient.api.get(artist = artist, title = title)
                result.syncedLyrics ?: result.plainLyrics
            } else {
                val results = LrcLibClient.api.search(query = title)
                results.firstOrNull()?.let {
                    it.syncedLyrics ?: it.plainLyrics
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun tryFetchFromSubsonic(artist: String?, title: String): String? {
        return try {
            val result = musicRepository.getLyrics(artist = artist, title = title)
            result.getOrNull()?.value
        } catch (e: Exception) {
            null
        }
    }
}
