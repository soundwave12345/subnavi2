package com.example.subnavi.data.repository

import com.example.subnavi.data.local.ServerConfigStore
import com.example.subnavi.data.remote.AlbumDetailDto
import com.example.subnavi.data.remote.AlbumDto
import com.example.subnavi.data.remote.PlaylistDetailDto
import com.example.subnavi.data.remote.PlaylistDto
import com.example.subnavi.data.remote.SongDto
import com.example.subnavi.data.remote.SubsonicApiClient
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val apiClient: SubsonicApiClient,
    private val configStore: ServerConfigStore
) {
    private suspend fun getApi() = apiClient.connect(configStore.config.first())

    private fun AlbumDto.withCoverArtUrl() = copy(
        coverArt = apiClient.getCoverArtUrl(coverArt)
    )

    private fun SongDto.withCoverArtUrl() = copy(
        coverArt = apiClient.getCoverArtUrl(coverArt)
    )

    private fun AlbumDetailDto.withCoverArtUrls() = copy(
        coverArt = apiClient.getCoverArtUrl(coverArt),
        song = song.map { it.withCoverArtUrl() }
    )

    private fun PlaylistDetailDto.withCoverArtUrls() = copy(
        coverArt = apiClient.getCoverArtUrl(coverArt),
        entry = entry.map { it.withCoverArtUrl() }
    )

    suspend fun getRecentAlbums(): Result<List<AlbumDto>> = try {
        val api = getApi()
        val response = api.getAlbumList2(type = "recent")
        val albums = response.subsonicResponse.albumList2?.album?.map { it.withCoverArtUrl() } ?: emptyList()
        Result.success(albums)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getNewestAlbums(): Result<List<AlbumDto>> = try {
        val api = getApi()
        val response = api.getAlbumList2(type = "newest")
        val albums = response.subsonicResponse.albumList2?.album?.map { it.withCoverArtUrl() } ?: emptyList()
        Result.success(albums)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getPlaylists(): Result<List<PlaylistDto>> = try {
        val api = getApi()
        val response = api.getPlaylists()
        val playlists = response.subsonicResponse.playlists?.playlist ?: emptyList()
        Result.success(playlists)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun searchAlbums(query: String): Result<List<AlbumDto>> = try {
        val api = getApi()
        val response = api.search3(query = query)
        val albums = response.subsonicResponse.searchResult3?.album?.map { it.withCoverArtUrl() } ?: emptyList()
        Result.success(albums)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getAllAlbums(): Result<List<AlbumDto>> = try {
        val api = getApi()
        val response = api.getAlbumList2(type = "alphabeticalByName", size = 100)
        val albums = response.subsonicResponse.albumList2?.album?.map { it.withCoverArtUrl() } ?: emptyList()
        Result.success(albums)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getRandomSongs(): Result<List<SongDto>> = try {
        val api = getApi()
        val response = api.getRandomSongs()
        val songs = response.subsonicResponse.randomSongs?.song?.map { it.withCoverArtUrl() } ?: emptyList()
        Result.success(songs)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun searchSongs(query: String): Result<List<SongDto>> = try {
        val api = getApi()
        val response = api.search3(query = query, albumCount = 0, songCount = 50)
        val songs = response.subsonicResponse.searchResult3?.song?.map { it.withCoverArtUrl() } ?: emptyList()
        Result.success(songs)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getAlbumDetail(albumId: String): Result<AlbumDetailDto> = try {
        val api = getApi()
        val response = api.getAlbum(id = albumId)
        val album = response.subsonicResponse.album
            ?: return Result.failure(IllegalStateException("Album not found"))
        Result.success(album.withCoverArtUrls())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getPlaylistDetail(playlistId: String): Result<PlaylistDetailDto> = try {
        val api = getApi()
        val response = api.getPlaylist(id = playlistId)
        val playlist = response.subsonicResponse.playlist
            ?: return Result.failure(IllegalStateException("Playlist not found"))
        Result.success(playlist.withCoverArtUrls())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun createPlaylist(name: String): Result<PlaylistDto> = try {
        val api = getApi()
        val response = api.createPlaylist(name = name)
        val playlist = response.subsonicResponse.playlists?.playlist?.firstOrNull()
            ?: return Result.failure(IllegalStateException("Created playlist not found in response"))
        Result.success(playlist)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deletePlaylist(playlistId: String): Result<Unit> = try {
        val api = getApi()
        api.deletePlaylist(id = playlistId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updatePlaylist(
        playlistId: String,
        name: String? = null,
        comment: String? = null,
        public: Boolean? = null
    ): Result<Unit> = try {
        val api = getApi()
        api.updatePlaylist(playlistId = playlistId, name = name, comment = comment, public = public)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
