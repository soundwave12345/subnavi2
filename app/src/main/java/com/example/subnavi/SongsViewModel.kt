package com.example.subnavi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subnavi.data.remote.SongDto
import com.example.subnavi.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SongsUiState(
    val songs: List<SongDto> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class SongsViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongsUiState())
    val uiState: StateFlow<SongsUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        loadSongs()
    }

    fun loadSongs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, canLoadMore = true)
            val result = musicRepository.getRandomSongs()
            _uiState.value = _uiState.value.copy(
                songs = result.getOrDefault(emptyList()),
                isLoading = false,
                canLoadMore = result.getOrDefault(emptyList()).size >= 50,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || !_uiState.value.canLoadMore || _uiState.value.searchQuery.isNotBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            val result = musicRepository.getRandomSongs()
            val newSongs = result.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(
                songs = _uiState.value.songs + newSongs,
                isLoadingMore = false,
                canLoadMore = newSongs.size >= 50
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            loadSongs()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.value = _uiState.value.copy(isSearching = true)
            val result = musicRepository.searchSongs(query)
            _uiState.value = _uiState.value.copy(
                songs = result.getOrDefault(emptyList()),
                isSearching = false
            )
        }
    }
}
