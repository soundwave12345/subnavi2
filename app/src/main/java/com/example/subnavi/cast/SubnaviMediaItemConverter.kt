package com.example.subnavi.cast

import android.net.Uri
import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.cast.MediaItemConverter
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.subnavi.data.remote.SubsonicApiClient
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata as CastMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.common.images.WebImage

/**
 * Converts between Media3 MediaItem (with Subsonic metadata) and Cast MediaQueueItem.
 * Ensures stream URLs and cover art are HTTP-reachable from the Cast device.
 */
class SubnaviMediaItemConverter(
    private val apiClient: SubsonicApiClient
) : MediaItemConverter {

    private val fallbackConverter = DefaultMediaItemConverter()

    override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
        val streamUrl = mediaItem.localConfiguration?.uri?.toString()
            ?: apiClient.getStreamUrl(mediaItem.mediaId)

        val mm = mediaItem.mediaMetadata
        val castMeta = CastMetadata(CastMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            putString(CastMetadata.KEY_TITLE, mm.title?.toString() ?: "")
            putString(CastMetadata.KEY_ARTIST, mm.artist?.toString() ?: "")
            putString(CastMetadata.KEY_ALBUM_TITLE, mm.albumTitle?.toString() ?: "")
            mm.artworkUri?.let { addImage(WebImage(it)) }
        }

        val mediaInfo = MediaInfo.Builder(streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("audio/mpeg")
            .setMetadata(castMeta)
            .build()

        return MediaQueueItem.Builder(mediaInfo).build()
    }

    override fun toMediaItem(queueItem: MediaQueueItem): MediaItem {
        val info = queueItem.media ?: return fallbackConverter.toMediaItem(queueItem)
        val meta = info.metadata
        val builder = MediaItem.Builder()
            .setMediaId(info.contentId ?: "")
            .setUri(info.contentId)

        if (meta != null) {
            builder.setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(meta.getString(CastMetadata.KEY_TITLE))
                    .setArtist(meta.getString(CastMetadata.KEY_ARTIST))
                    .setAlbumTitle(meta.getString(CastMetadata.KEY_ALBUM_TITLE))
                    .build()
            )
        }
        return builder.build()
    }
}
