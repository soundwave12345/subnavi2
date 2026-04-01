package com.example.subnavi.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.subnavi.SubnaviApp

class SubnaviPlaybackService : MediaLibraryService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val playbackManager = SubnaviApp.instance.playbackManager
        val player = playbackManager.getPlayer(this)
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(object : MediaLibraryService.MediaLibrarySession.Callback {})
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession as? MediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
