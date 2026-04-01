package com.example.subnavi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subnavi.data.remote.PlaylistDetailDto
import com.example.subnavi.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistDetailUiState(
    val playlist: PlaylistDetailDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val playlistId: String = checkNotNull(savedStateHandle["playlistId"])

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    init {
        loadPlaylist()
    }

    fun loadPlaylist() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = musicRepository.getPlaylistDetail(playlistId)
            _uiState.value = _uiState.value.copy(
                playlist = result.getOrNull(),
                isLoading = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }
}
