package com.example.subnavi.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.subnavi.R
import com.example.subnavi.SubnaviApp

class SubnaviPlaybackService : MediaLibraryService() {

    companion object {
        private const val CHANNEL_ID = "subnavi_playback"
        private const val NOTIFICATION_ID = 1
    }

    private var mediaSession: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()

        // Start foreground IMMEDIATELY to avoid ForegroundServiceDidNotStartInTimeException
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildLoadingNotification())

        val player = SubnaviApp.instance.playbackManager.getPlayer(this)
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )

        mediaSession = MediaLibrarySession.Builder(
                this,
                player,
                object : MediaLibrarySession.Callback {}
            ).build()

        // Register the session so Media3 manages its notification automatically
        addSession(mediaSession!!)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows now playing controls"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = ContextCompat.getSystemService(this, NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildLoadingNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Subnavi")
            .setContentText("Loading...")
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .build()
    }
}
