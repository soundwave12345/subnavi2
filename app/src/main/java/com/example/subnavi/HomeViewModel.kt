package com.example.subnavi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subnavi.data.remote.AlbumDto
import com.example.subnavi.data.remote.PlaylistDto
import com.example.subnavi.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentAlbums: List<AlbumDto> = emptyList(),
    val newestAlbums: List<AlbumDto> = emptyList(),
    val playlists: List<PlaylistDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            var errorMsg: String? = null

            val recentResult = musicRepository.getRecentAlbums()
            if (recentResult.isFailure) errorMsg = recentResult.exceptionOrNull()?.message

            val newestResult = musicRepository.getNewestAlbums()
            if (newestResult.isFailure && errorMsg == null) errorMsg = newestResult.exceptionOrNull()?.message

            val playlistsResult = musicRepository.getPlaylists()
            if (playlistsResult.isFailure && errorMsg == null) errorMsg = playlistsResult.exceptionOrNull()?.message

            _uiState.value = _uiState.value.copy(
                recentAlbums = recentResult.getOrDefault(emptyList()),
                newestAlbums = newestResult.getOrDefault(emptyList()),
                playlists = playlistsResult.getOrDefault(emptyList()),
                isLoading = false,
                error = errorMsg
            )
        }
    }
}
