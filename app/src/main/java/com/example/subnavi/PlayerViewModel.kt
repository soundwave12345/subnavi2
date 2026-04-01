package com.example.subnavi

import androidx.lifecycle.ViewModel
import com.example.subnavi.data.remote.SongDto
import com.example.subnavi.playback.PlaybackManager
import com.example.subnavi.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val playbackManager: PlaybackManager
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    fun play(songs: List<SongDto>, startIndex: Int = 0) {
        playbackManager.play(songs, startIndex)
    }

    fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }

    fun skipNext() {
        playbackManager.skipNext()
    }

    fun skipPrevious() {
        playbackManager.skipPrevious()
    }
}
