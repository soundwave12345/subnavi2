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
}

data class SubsonicResponse(
    @SerializedName("subsonic-response") val response: SubsonicInner
)

data class SubsonicInner(
    val status: String,
    val version: String,
    val error: SubsonicError? = null
)

data class SubsonicError(
    val code: Int,
    val message: String
)
