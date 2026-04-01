package com.example.subnavi.data.repository

import com.example.subnavi.data.local.ServerConfig
import com.example.subnavi.data.local.ServerConfigStore
import com.example.subnavi.data.remote.SubsonicApiClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val configStore: ServerConfigStore,
    private val apiClient: SubsonicApiClient
) {
    val serverConfig = configStore.config

    suspend fun testConnection(config: ServerConfig): Result<String> {
        return try {
            val api = apiClient.connect(config)
            val response = api.ping()
            val inner = response.subsonicResponse
            if (inner.status == "ok") {
                Result.success("Connection successful (server v${inner.version})")
            } else {
                val error = inner.error
                Result.failure(Exception(error?.message ?: "Authentication failed (code ${error?.code})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.localizedMessage}"))
        }
    }

    suspend fun saveConfig(config: ServerConfig) {
        configStore.save(config)
    }
}
