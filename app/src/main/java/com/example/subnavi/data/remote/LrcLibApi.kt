package com.example.subnavi.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class LrcLibResult(
    val id: Long = 0,
    @SerializedName("trackName") val trackName: String? = null,
    @SerializedName("artistName") val artistName: String? = null,
    @SerializedName("syncedLyrics") val syncedLyrics: String? = null,
    @SerializedName("plainLyrics") val plainLyrics: String? = null
)

interface LrcLibApiService {

    @GET("/api/search")
    suspend fun search(
        @Query("q") query: String
    ): List<LrcLibResult>

    @GET("/api/get")
    suspend fun get(
        @Query("artist_name") artist: String,
        @Query("track_name") title: String
    ): LrcLibResult
}

object LrcLibClient {
    val api: LrcLibApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://lrclib.net")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LrcLibApiService::class.java)
    }
}
