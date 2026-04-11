package com.example.subnavi.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import com.example.subnavi.R
import com.example.subnavi.SubnaviApp
import com.example.subnavi.data.remote.SongDto
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.guava.asListenableFuture
import kotlinx.coroutines.launch

class SubnaviPlaybackService : MediaLibraryService() {

    companion object {
        private const val TAG = "SubnaviPlaybackService"
        private const val CHANNEL_ID = "subnavi_playback"
        private const val NOTIFICATION_ID = 1

        private const val ROOT_ID = "[root]"
        private const val ALBUMS_ID = "[albums]"
        private const val PLAYLISTS_ID = "[playlists]"
        private const val RECENT_ID = "[recent]"
        private const val RANDOM_SONGS_ID = "[random]"
        private const val ALBUM_PREFIX = "[album]"
        private const val PLAYLIST_PREFIX = "[playlist]"
    }

    private var mediaSession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var cachedSearchResults: List<SongDto> = emptyList()

    override fun onCreate() {
        super.onCreate()

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
            SubnaviLibraryCallback()
        ).build()

        addSession(mediaSession!!)
    }

    override fun onGetSession(controllerInfo: ControllerInfo): MediaLibrarySession? {
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

    private inner class SubnaviLibraryCallback : MediaLibrarySession.Callback {

        override fun onAddMediaItems(
            session: MediaSession,
            controller: ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            // Android Auto sends items without URI — resolve from mediaId
            val resolvedItems = mediaItems.map { item ->
                if (item.localConfiguration != null) {
                    item
                } else {
                    resolveMediaItem(item.mediaId)
                }
            }
            return Futures.immediateFuture(resolvedItems)
        }

        override fun onGetLibraryRoot(
            session: MediaLibraryService.MediaLibrarySession,
            browser: ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Subnavi")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibraryService.MediaLibrarySession,
            browser: ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return when (parentId) {
                ROOT_ID -> {
                    val items = ImmutableList.of(
                        buildCategoryItem(ALBUMS_ID, "Albums"),
                        buildCategoryItem(PLAYLISTS_ID, "Playlists"),
                        buildCategoryItem(RECENT_ID, "Recently Played"),
                        buildCategoryItem(RANDOM_SONGS_ID, "Random Songs")
                    )
                    Futures.immediateFuture(LibraryResult.ofItemList(items, params))
                }
                ALBUMS_ID -> loadAlbums(params)
                PLAYLISTS_ID -> loadPlaylists(params)
                RECENT_ID -> loadRecentAlbums(params)
                RANDOM_SONGS_ID -> loadRandomSongs(params)
                else -> {
                    if (parentId.startsWith(ALBUM_PREFIX)) {
                        loadAlbumSongs(parentId.removePrefix(ALBUM_PREFIX), params)
                    } else if (parentId.startsWith(PLAYLIST_PREFIX)) {
                        loadPlaylistSongs(parentId.removePrefix(PLAYLIST_PREFIX), params)
                    } else {
                        // Search results: return cached search results
                        val items = ImmutableList.copyOf(
                            cachedSearchResults.map { buildSongMediaItem(it) }
                        )
                        Futures.immediateFuture(LibraryResult.ofItemList(items, params))
                    }
                }
            }
        }

        override fun onGetItem(
            session: MediaLibraryService.MediaLibrarySession,
            browser: ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return loadItem(mediaId)
        }

        override fun onSearch(
            session: MediaLibraryService.MediaLibrarySession,
            browser: ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            serviceScope.launch {
                val repo = getRepository()
                if (repo != null) {
                    cachedSearchResults = repo.searchSongs(query).getOrNull() ?: emptyList()
                    session.notifySearchResultChanged(browser, query, cachedSearchResults.size, params)
                }
            }
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }
    }

    // --- Resolve media items without URI (from Android Auto) ---

    private fun resolveMediaItem(mediaId: String): MediaItem {
        val apiClient = SubnaviApp.instance.playbackManager.apiClient
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(apiClient.getStreamUrl(mediaId))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build()
            )
            .build()
    }

    // --- Async data loaders using serviceScope.async ---

    private fun loadAlbums(
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return serviceScope.async(Dispatchers.IO) {
            val repo = getRepository() ?: return@async errImmutable("No repository")
            val albums = repo.getAllAlbums().getOrNull() ?: return@async errImmutable("Load failed")
            val items = albums.map { a ->
                MediaItem.Builder()
                    .setMediaId("$ALBUM_PREFIX${a.id}")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(a.name)
                            .setSubtitle(a.artist)
                            .setArtworkUri(a.coverArt?.let { Uri.parse(it) })
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .build()
                    )
                    .build()
            }
            LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
        }.asListenableFuture()
    }

    private fun loadPlaylists(
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return serviceScope.async(Dispatchers.IO) {
            val repo = getRepository() ?: return@async errImmutable("No repository")
            val playlists = repo.getPlaylists().getOrNull() ?: return@async errImmutable("Load failed")
            val items = playlists.map { p ->
                MediaItem.Builder()
                    .setMediaId("$PLAYLIST_PREFIX${p.id}")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(p.name)
                            .setSubtitle("${p.songCount} songs")
                            .setArtworkUri(p.coverArt?.let { Uri.parse(it) })
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .build()
                    )
                    .build()
            }
            LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
        }.asListenableFuture()
    }

    private fun loadRecentAlbums(
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return serviceScope.async(Dispatchers.IO) {
            val repo = getRepository() ?: return@async errImmutable("No repository")
            val albums = repo.getRecentAlbums().getOrNull() ?: return@async errImmutable("Load failed")
            val items = albums.map { a ->
                MediaItem.Builder()
                    .setMediaId("$ALBUM_PREFIX${a.id}")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(a.name)
                            .setSubtitle(a.artist)
                            .setArtworkUri(a.coverArt?.let { Uri.parse(it) })
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .build()
                    )
                    .build()
            }
            LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
        }.asListenableFuture()
    }

    private fun loadRandomSongs(
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return serviceScope.async(Dispatchers.IO) {
            val repo = getRepository() ?: return@async errImmutable("No repository")
            val songs = repo.getRandomSongs().getOrNull() ?: return@async errImmutable("Load failed")
            LibraryResult.ofItemList(ImmutableList.copyOf(songs.map { buildSongMediaItem(it) }), params)
        }.asListenableFuture()
    }

    private fun loadAlbumSongs(
        albumId: String,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return serviceScope.async(Dispatchers.IO) {
            val repo = getRepository() ?: return@async errImmutable("No repository")
            val album = repo.getAlbumDetail(albumId).getOrNull() ?: return@async errImmutable("Album not found")
            LibraryResult.ofItemList(ImmutableList.copyOf(album.song.map { buildSongMediaItem(it) }), params)
        }.asListenableFuture()
    }

    private fun loadPlaylistSongs(
        playlistId: String,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return serviceScope.async(Dispatchers.IO) {
            val repo = getRepository() ?: return@async errImmutable("No repository")
            val playlist = repo.getPlaylistDetail(playlistId).getOrNull()
                ?: return@async errImmutable("Playlist not found")
            LibraryResult.ofItemList(ImmutableList.copyOf(playlist.entry.map { buildSongMediaItem(it) }), params)
        }.asListenableFuture()
    }

    private fun loadItem(mediaId: String): ListenableFuture<LibraryResult<MediaItem>> {
        return serviceScope.async(Dispatchers.IO) {
            val repo = getRepository() ?: return@async LibraryResult.ofError<MediaItem>(
                LibraryResult.RESULT_ERROR_UNKNOWN
            )
            val song = repo.searchSongs(mediaId).getOrNull()?.firstOrNull()
                ?: return@async LibraryResult.ofError<MediaItem>(
                    LibraryResult.RESULT_ERROR_UNKNOWN
                )
            LibraryResult.ofItem(buildSongMediaItem(song), null)
        }.asListenableFuture()
    }

    // --- Helpers ---

    private fun buildSongMediaItem(song: SongDto): MediaItem {
        val apiClient = SubnaviApp.instance.playbackManager.apiClient
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(apiClient.getStreamUrl(song.id))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.coverArt?.let { Uri.parse(it) })
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }

    private fun buildCategoryItem(id: String, title: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }

    private fun getRepository() = try {
        SubnaviApp.instance.musicRepository
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get repository", e)
        null
    }

    private fun errImmutable(msg: String): LibraryResult<ImmutableList<MediaItem>> {
        return LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN)
    }
}
