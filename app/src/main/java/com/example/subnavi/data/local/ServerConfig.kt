package com.example.subnavi.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.serverDataStore: DataStore<Preferences> by preferencesDataStore(name = "server_config")

data class ServerConfig(
    val url: String = "",
    val username: String = "",
    val password: String = ""
)

@Singleton
class ServerConfigStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val URL = stringPreferencesKey("server_url")
        val USERNAME = stringPreferencesKey("server_username")
        val PASSWORD = stringPreferencesKey("server_password")
    }

    val config: Flow<ServerConfig> = context.serverDataStore.data.map { prefs ->
        ServerConfig(
            url = prefs[Keys.URL] ?: "",
            username = prefs[Keys.USERNAME] ?: "",
            password = prefs[Keys.PASSWORD] ?: ""
        )
    }

    suspend fun save(config: ServerConfig) {
        context.serverDataStore.edit { prefs ->
            prefs[Keys.URL] = config.url.trimEnd('/')
            prefs[Keys.USERNAME] = config.username
            prefs[Keys.PASSWORD] = config.password
        }
    }
}
