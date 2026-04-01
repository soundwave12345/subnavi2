package com.example.subnavi

import android.app.Application
import com.example.subnavi.playback.PlaybackManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SubnaviApp : Application() {
    @Inject lateinit var playbackManager: PlaybackManager

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: SubnaviApp
            private set
    }
}
