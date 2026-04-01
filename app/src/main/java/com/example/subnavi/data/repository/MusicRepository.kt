package com.example.subnavi.data.repository

import com.example.subnavi.data.local.ServerConfigStore
import com.example.subnavi.data.remote.AlbumDto
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
}
