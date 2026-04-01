package com.example.subnavi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subnavi.data.remote.PlaylistDto
import com.example.subnavi.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistsUiState(
    val playlists: List<PlaylistDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val createError: String? = null
)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistsUiState())
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    init {
        loadPlaylists()
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = musicRepository.getPlaylists()
            _uiState.value = _uiState.value.copy(
                playlists = result.getOrDefault(emptyList()),
                isLoading = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true, createError = null)
    }

    fun hideCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false, createError = null)
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = musicRepository.createPlaylist(name)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(showCreateDialog = false)
                loadPlaylists()
            } else {
                _uiState.value = _uiState.value.copy(
                    createError = result.exceptionOrNull()?.message ?: "Failed to create playlist"
                )
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            musicRepository.deletePlaylist(playlistId)
            loadPlaylists()
        }
    }
}
