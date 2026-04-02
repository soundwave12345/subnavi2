package com.example.subnavi.cast

import android.content.Context
import android.net.Uri
import com.example.subnavi.data.remote.SongDto
import com.example.subnavi.data.remote.SubsonicApiClient
import com.google.android.gms.cast.CastDevice
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage

object CastHelper {

    fun getCastContext(context: Context): CastContext {
        return CastContext.getSharedInstance(context)
    }

    fun getCastState(context: Context): Int {
        return try {
            getCastContext(context).castState
        } catch (e: Exception) {
            CastState.NO_DEVICES_AVAILABLE
        }
    }

    fun getSelectedDevice(context: Context): CastDevice? {
        return try {
            getCastContext(context).sessionManager.currentCastSession?.castDevice
        } catch (e: Exception) {
            null
        }
    }

    fun isConnected(context: Context): Boolean {
        return try {
            getCastContext(context).sessionManager.currentCastSession?.isConnected == true
        } catch (e: Exception) {
            false
        }
    }

    fun loadSong(context: Context, apiClient: SubsonicApiClient, song: SongDto) {
        val castContext = getCastContext(context)
        val session = castContext.sessionManager.currentCastSession ?: return
        val remoteMediaClient = session.remoteMediaClient ?: return

        val streamUrl = apiClient.getStreamUrl(song.id)
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            putString(MediaMetadata.KEY_TITLE, song.title)
            putString(MediaMetadata.KEY_ARTIST, song.artist ?: "")
            putString(MediaMetadata.KEY_ALBUM_TITLE, song.album ?: "")
            song.coverArt?.let { addImage(WebImage(Uri.parse(it))) }
        }

        val mediaInfo = MediaInfo.Builder(streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("audio/mpeg")
            .setMetadata(metadata)
            .build()

        remoteMediaClient.load(MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .build())
    }
}
