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

data class LyricsLine(
    val timeMs: Long,
    val text: String
)

data class LyricsUiState(
    val lines: List<LyricsLine> = emptyList(),
    val rawLyrics: String? = null,
    val isSynced: Boolean = false,
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

            val result = tryFetchFromLrcLib(artist, title)
                ?: tryFetchFromLrcLib(null, title)
                ?: tryFetchFromSubsonic(artist, title)

            val synced = result?.first
            val plain = result?.second
            val lines = if (synced != null) parseLrc(synced) else emptyList()
            _uiState.value = LyricsUiState(
                lines = lines,
                rawLyrics = synced ?: plain,
                isSynced = synced != null,
                isLoading = false
            )
        }
    }

    private suspend fun tryFetchFromLrcLib(
        artist: String?,
        title: String
    ): Pair<String?, String?>? {
        return try {
            if (artist != null) {
                val result = LrcLibClient.api.get(artist = artist, title = title)
                result.syncedLyrics to result.plainLyrics
            } else {
                val results = LrcLibClient.api.search(query = title)
                results.firstOrNull()?.let { it.syncedLyrics to it.plainLyrics }
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun tryFetchFromSubsonic(
        artist: String?,
        title: String
    ): Pair<String?, String?>? {
        return try {
            val result = musicRepository.getLyrics(artist = artist, title = title)
            val value = result.getOrNull()?.value
            if (value != null) null to value else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLrc(lrc: String): List<LyricsLine> {
        val lineRegex = Regex("""\[(\d{2}):(\d{2})[.:](\d{2,3})](.*)""")
        return lrc.lines().mapNotNull { line ->
            val match = lineRegex.matchEntire(line.trim()) ?: return@mapNotNull null
            val min = match.groupValues[1].toLong()
            val sec = match.groupValues[2].toLong()
            val ms = match.groupValues[3].let {
                if (it.length == 2) it.toLong() * 10 else it.toLong()
            }
            val text = match.groupValues[4].trim()
            if (text.isEmpty()) return@mapNotNull null
            LyricsLine(timeMs = min * 60_000 + sec * 1_000 + ms, text = text)
        }.sortedBy { it.timeMs }
    }

    fun getCurrentLineIndex(positionMs: Long): Int {
        val lines = _uiState.value.lines
        if (lines.isEmpty()) return -1
        var idx = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs) idx = i else break
        }
        return idx
    }
}
