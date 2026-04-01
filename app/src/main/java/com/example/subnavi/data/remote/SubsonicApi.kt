package com.example.subnavi.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST

interface SubsonicApi {

    @GET("rest/ping")
    suspend fun ping(): SubsonicResponse

    @GET("rest/getAlbumList2")
    suspend fun getAlbumList2(
        @Query("type") type: String,
        @Query("size") size: Int = 20,
        @Query("offset") offset: Int = 0
    ): SubsonicResponse

    @GET("rest/getPlaylists")
    suspend fun getPlaylists(): SubsonicResponse

    @GET("rest/search3")
    suspend fun search3(
        @Query("query") query: String,
        @Query("albumCount") albumCount: Int = 20,
        @Query("songCount") songCount: Int = 20,
        @Query("artistCount") artistCount: Int = 0
    ): SubsonicResponse

    @GET("rest/getRandomSongs")
    suspend fun getRandomSongs(
        @Query("size") size: Int = 50
    ): SubsonicResponse

    @GET("rest/getAlbum")
    suspend fun getAlbum(
        @Query("id") id: String
    ): SubsonicResponse

    @GET("rest/getPlaylist")
    suspend fun getPlaylist(
        @Query("id") id: String
    ): SubsonicResponse

    @GET("rest/createPlaylist")
    suspend fun createPlaylist(
        @Query("name") name: String
    ): SubsonicResponse

    @GET("rest/deletePlaylist")
    suspend fun deletePlaylist(
        @Query("id") id: String
    ): SubsonicResponse

    @GET("rest/updatePlaylist")
    suspend fun updatePlaylist(
        @Query("playlistId") playlistId: String,
        @Query("name") name: String? = null,
        @Query("comment") comment: String? = null,
        @Query("public") public: Boolean? = null
    ): SubsonicResponse

    @GET("rest/scrobble")
    suspend fun scrobble(
        @Query("id") id: String,
        @Query("submission") submission: Boolean = true
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
    val playlists: PlaylistsWrapper? = null,
    @SerializedName("searchResult3") val searchResult3: SearchResult3? = null,
    @SerializedName("randomSongs") val randomSongs: RandomSongs? = null,
    val album: AlbumDetailDto? = null,
    val playlist: PlaylistDetailDto? = null
)

data class SearchResult3(
    val album: List<AlbumDto> = emptyList(),
    val song: List<SongDto> = emptyList()
)

data class RandomSongs(
    val song: List<SongDto> = emptyList()
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

data class AlbumDetailDto(
    val id: String,
    val name: String,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int = 0,
    val year: Int? = null,
    val playCount: Long? = null,
    val genre: String? = null,
    val song: List<SongDto> = emptyList()
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

data class PlaylistDetailDto(
    val id: String,
    val name: String,
    val songCount: Int = 0,
    val duration: Long = 0,
    val comment: String? = null,
    val owner: String? = null,
    val public: Boolean? = null,
    val coverArt: String? = null,
    val entry: List<SongDto> = emptyList()
)

data class SongDto(
    val id: String,
    val title: String,
    val album: String? = null,
    val albumId: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val duration: Int = 0,
    val track: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val size: Long = 0,
    val contentType: String? = null,
    val suffix: String? = null,
    val starred: Boolean? = null
)

data class SubsonicError(
    val code: Int,
    val message: String
)
