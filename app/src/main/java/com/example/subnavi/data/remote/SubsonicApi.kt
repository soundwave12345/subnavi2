package com.example.subnavi.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface SubsonicApi {

    @GET("rest/ping")
    suspend fun ping(
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "Subnavi",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/getAlbumList2")
    suspend fun getAlbumList2(
        @Query("type") type: String,
        @Query("size") size: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("u") username: String = "",
        @Query("t") token: String = "",
        @Query("s") salt: String = "",
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "Subnavi",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/getPlaylists")
    suspend fun getPlaylists(
        @Query("u") username: String = "",
        @Query("t") token: String = "",
        @Query("s") salt: String = "",
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "Subnavi",
        @Query("f") format: String = "json"
    ): SubsonicResponse
}

data class SubsonicResponse(
    @SerializedName("subsonic-response") val subsonicResponse: SubsonicInner
)

data class SubsonicInner(
    val status: String,
    val version: String,
    val error: SubsonicError? = null,
    @SerializedName("albumList2") val albumList2: AlbumList2? = null,
    val playlists: PlaylistsWrapper? = null
)

data class AlbumList2(
    val album: List<AlbumDto> = emptyList()
)

data class AlbumDto(
    val id: String,
    val name: String,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int = 0,
    val year: Int? = null,
    val playCount: Long? = null
)

data class PlaylistsWrapper(
    val playlist: List<PlaylistDto> = emptyList()
)

data class PlaylistDto(
    val id: String,
    val name: String,
    val songCount: Int = 0,
    val duration: Long = 0,
    val comment: String? = null,
    val owner: String? = null,
    val public: Boolean? = null,
    val coverArt: String? = null
)

data class SubsonicError(
    val code: Int,
    val message: String
)
