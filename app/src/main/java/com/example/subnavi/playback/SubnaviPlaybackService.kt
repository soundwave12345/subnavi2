package com.example.subnavi.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
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
        private const val PLAY_ALL_PREFIX = "[play_all]"
        private const val SHUFFLE_ALL_PREFIX = "[shuffle_all]"
    }

    private var mediaSession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Cache last browsed song list for queue context
    private var lastBrowsedSongs: List<SongDto> = emptyList()
    private var cachedSearchQuery: String = ""
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
            Log.d(TAG, "onAddMediaItems: ${mediaItems.size} items, ids=${mediaItems.map { it.mediaId }}")

            val firstId = mediaItems.firstOrNull()?.mediaId ?: ""

            // Handle Play All / Shuffle All — expand to full song list
            if (firstId.startsWith(PLAY_ALL_PREFIX) || firstId.startsWith(SHUFFLE_ALL_PREFIX)) {
                val isShuffle = firstId.startsWith(SHUFFLE_ALL_PREFIX)
                val parentId = if (isShuffle) {
                    firstId.removePrefix(SHUFFLE_ALL_PREFIX)
                } else {
                    firstId.removePrefix(PLAY_ALL_PREFIX)
                }
                if (isShuffle) session.player.shuffleModeEnabled = true

                return serviceScope.async(Dispatchers.IO) {
                    val songs = loadSongsForParent(parentId)
                    Log.d(TAG, "Play/Shuffle All: ${songs.size} songs for $parentId")
                    songs.map { buildSongMediaItem(it) }
                }.asListenableFuture()
            }

            // Voice search: Android Auto sends playFromSearch() which arrives here
            // with the search query as mediaId (not a Subsonic song ID).
            // Detect non-numeric IDs and search for them.
            if (mediaItems.size == 1 && !firstId.startsWith("[") && !isSubsonicId(firstId)) {
                val query = firstId.ifBlank {
                    mediaItems.first().mediaMetadata.title?.toString() ?: ""
                }
                if (query.isNotBlank()) {
                    Log.d(TAG, "onAddMediaItems: voice search for '$query'")
                    return serviceScope.async(Dispatchers.IO) {
                        val repo = getRepository()
                        if (repo != null) {
                            val results = repo.searchSongs(query).getOrDefault(emptyList())
                            Log.d(TAG, "Voice search results: ${results.size} songs for '$query'")
                            if (results.isNotEmpty()) {
                                lastBrowsedSongs = results
                                return@async results.map { buildSongMediaItem(it) }
                            }
                        }
                        // Fallback: try resolving as a song ID
                        listOf(resolveMediaItem(firstId))
                    }.asListenableFuture()
                }
            }

            // Single song from browse — expand to full queue context
            if (mediaItems.size == 1 && !firstId.startsWith("[")) {
                val cached = lastBrowsedSongs
                val index = cached.indexOfFirst { it.id == firstId }
                if (index >= 0 && cached.size > 1) {
                    Log.d(TAG, "onAddMediaItems: expanding single song to queue of ${cached.size}, start=$index")
                    val items = cached.map { buildSongMediaItem(it) }
                    // Seek to correct position after items are set
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            val p = session.player
                            if (p.mediaItemCount == items.size) {
                                p.seekTo(index, 0)
                                Log.d(TAG, "onAddMediaItems: seeked to index $index")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "onAddMediaItems: seek failed", e)
                        }
                    }, 300)
                    return Futures.immediateFuture(items)
                }
            }

            // Fallback — resolve URI from mediaId
            val resolvedItems = mediaItems.map { item ->
                if (item.localConfiguration != null) {
                    item
                } else {
                    Log.d(TAG, "onAddMediaItems: resolving URI for ${item.mediaId}")
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
            Log.d(TAG, "onGetChildren: parentId=$parentId page=$page pageSize=$pageSize")
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
                        // Search results
                        val results = if (parentId == cachedSearchQuery) cachedSearchResults else emptyList()
                        Log.d(TAG, "onGetChildren search: query=$parentId results=${results.size}")
                        if (results.isNotEmpty()) {
                            lastBrowsedSongs = results
                        }
                        val items = ImmutableList.copyOf(results.map { buildSongMediaItem(it) })
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
            Log.d(TAG, "onSearch: query=$query")
            cachedSearchQuery = query
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val repo = getRepository()
                    cachedSearchResults = if (repo != null) {
                        repo.searchSongs(query).getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onSearch failed for '$query'", e)
                    cachedSearchResults = emptyList()
                }
                Log.d(TAG, "onSearch results: ${cachedSearchResults.size} songs for '$query'")
                // Must notify on main thread — Android Auto ignores notifications
                // from background threads
                Handler(Looper.getMainLooper()).post {
                    session.notifySearchResultChanged(
                        browser, query, cachedSearchResults.size, params
                    )
                }
            }
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }
    }

    // --- Resolve media items without URI ---

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

    private fun buildPlayAllItem(parentId: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId("$PLAY_ALL_PREFIX$parentId")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Play All")
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }

    private fun buildShuffleAllItem(parentId: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId("$SHUFFLE_ALL_PREFIX$parentId")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Shuffle All")
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }

    private suspend fun loadSongsForParent(parentId: String): List<SongDto> {
        val repo = getRepository() ?: return emptyList()
        return when {
            parentId.startsWith(ALBUM_PREFIX) -> {
                repo.getAlbumDetail(parentId.removePrefix(ALBUM_PREFIX))
                    .getOrNull()?.song ?: emptyList()
            }
            parentId.startsWith(PLAYLIST_PREFIX) -> {
                repo.getPlaylistDetail(parentId.removePrefix(PLAYLIST_PREFIX))
                    .getOrNull()?.entry ?: emptyList()
            }
            parentId == RANDOM_SONGS_ID -> {
                repo.getRandomSongs().getOrNull() ?: emptyList()
            }
            else -> emptyList()
        }
    }

    // --- Async data loaders ---

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
            lastBrowsedSongs = songs
            val items = listOf(
                buildPlayAllItem(RANDOM_SONGS_ID),
                buildShuffleAllItem(RANDOM_SONGS_ID)
            ) + songs.map { buildSongMediaItem(it) }
            LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
        }.asListenableFuture()
    }

    private fun loadAlbumSongs(
        albumId: String,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return serviceScope.async(Dispatchers.IO) {
            val repo = getRepository() ?: return@async errImmutable("No repository")
            val album = repo.getAlbumDetail(albumId).getOrNull() ?: return@async errImmutable("Album not found")
            lastBrowsedSongs = album.song
            val parentId = "$ALBUM_PREFIX$albumId"
            val items = listOf(
                buildPlayAllItem(parentId),
                buildShuffleAllItem(parentId)
            ) + album.song.map { buildSongMediaItem(it) }
            LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
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
            lastBrowsedSongs = playlist.entry
            val parentId = "$PLAYLIST_PREFIX$playlistId"
            val items = listOf(
                buildPlayAllItem(parentId),
                buildShuffleAllItem(parentId)
            ) + playlist.entry.map { buildSongMediaItem(it) }
            LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
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

    /** Subsonic IDs are numeric (e.g., "1234"). Voice queries like "Queen" are not. */
    private fun isSubsonicId(id: String): Boolean {
        return id.isNotEmpty() && id.all { it.isDigit() }
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
