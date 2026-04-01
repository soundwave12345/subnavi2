package com.example.subnavi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subnavi.data.local.ServerConfig
import com.example.subnavi.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val result: String? = null,
    val isError: Boolean = false,
    val isConnected: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updateUrl(url: String) = _uiState.value.copy(url = url).update()
    fun updateUsername(username: String) = _uiState.value.copy(username = username).update()
    fun updatePassword(password: String) = _uiState.value.copy(password = password).update()

    fun testConnection() {
        val state = _uiState.value
        if (state.url.isBlank() || state.username.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(result = "All fields are required", isError = true)
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, result = null)
            val config = ServerConfig(state.url, state.username, state.password)
            val result = authRepository.testConnection(config)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                result = result.getOrNull() ?: result.exceptionOrNull()?.message,
                isError = result.isFailure,
                isConnected = result.isSuccess
            )
            if (result.isSuccess) {
                authRepository.saveConfig(config)
            }
        }
    }

    private fun OnboardingUiState.update() {
        _uiState.value = this
    }
}
