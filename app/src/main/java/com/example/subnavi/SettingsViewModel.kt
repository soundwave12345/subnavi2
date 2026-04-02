package com.example.subnavi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subnavi.data.local.ServerConfig
import com.example.subnavi.data.local.ServerConfigStore
import com.example.subnavi.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val serverUrl: String = "",
    val username: String = "",
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val isLoggedOut: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val configStore: ServerConfigStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var currentConfig = ServerConfig()

    init {
        viewModelScope.launch {
            configStore.config.collect { config ->
                currentConfig = config
                _uiState.value = _uiState.value.copy(
                    serverUrl = config.url,
                    username = config.username
                )
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true, testResult = null)
            val result = authRepository.testConnection(currentConfig)
            _uiState.value = _uiState.value.copy(
                isTesting = false,
                testResult = result.getOrDefault("OK")
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            configStore.save(ServerConfig())
            _uiState.value = _uiState.value.copy(isLoggedOut = true)
        }
    }
}
