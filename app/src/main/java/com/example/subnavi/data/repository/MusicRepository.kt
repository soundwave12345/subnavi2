package com.example.subnavi.data.repository

import com.example.subnavi.data.local.ServerConfig
import com.example.subnavi.data.local.ServerConfigStore
import com.example.subnavi.data.remote.AlbumDto
import com.example.subnavi.data.remote.PlaylistDto
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

    suspend fun getRecentAlbums(): Result<List<AlbumDto>> = try {
        val api = getApi()
        val response = api.getAlbumList2(type = "recent")
        val albums = response.subsonicResponse.albumList2?.album ?: emptyList()
        Result.success(albums)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getNewestAlbums(): Result<List<AlbumDto>> = try {
        val api = getApi()
        val response = api.getAlbumList2(type = "newest")
        val albums = response.subsonicResponse.albumList2?.album ?: emptyList()
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
}
