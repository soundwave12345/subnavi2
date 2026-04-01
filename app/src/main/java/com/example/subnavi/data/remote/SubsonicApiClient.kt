package com.example.subnavi.data.remote

import com.example.subnavi.data.local.ServerConfig
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubsonicApiClient @Inject constructor() {

    private var currentBaseUrl: String = ""
    private var currentSalt: String = ""
    private var _api: SubsonicApi? = null

    val api: SubsonicApi
        get() = _api ?: throw IllegalStateException("Call connect() first")

    fun connect(config: ServerConfig): SubsonicApi {
        val baseUrl = normalizeUrl(config.url)
        val (token, salt) = SubsonicAuth.generateToken(config.password)
        currentSalt = salt

        if (_api == null || baseUrl != currentBaseUrl) {
            val authInterceptor = Interceptor { chain ->
                val original = chain.request()
                val newUrl = original.url.newBuilder()
                    .addQueryParameter("u", config.username)
                    .addQueryParameter("t", token)
                    .addQueryParameter("s", salt)
                    .addQueryParameter("v", "1.16.1")
                    .addQueryParameter("c", "Subnavi")
                    .addQueryParameter("f", "json")
                    .build()
                chain.proceed(original.newBuilder().url(newUrl).build())
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
                .build()

            _api = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SubsonicApi::class.java)

            currentBaseUrl = baseUrl
        }
        return _api!!
    }

    fun getCoverArtUrl(coverArtId: String?, size: Int = 300): String? {
        if (coverArtId.isNullOrBlank() || currentBaseUrl.isBlank()) return null
        return "${currentBaseUrl}rest/getCoverArt?id=$coverArtId&size=$size"
    }

    private fun normalizeUrl(url: String): String {
        var base = url.trimEnd('/')
        return "$base/"
    }
}
