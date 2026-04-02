package com.example.subnavi

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subnavi.cast.CastHelper
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CastViewModel @Inject constructor() : ViewModel() {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var contextSet = false

    fun setContext(context: Context) {
        if (contextSet) return
        contextSet = true
        viewModelScope.launch {
            try {
                val castContext = CastContext.getSharedInstance(context)
                val sessionManager = castContext.sessionManager
                // Poll cast state
                while (true) {
                    _isConnected.value = CastHelper.isConnected(context)
                    kotlinx.coroutines.delay(1000)
                }
            } catch (e: Exception) {
                // Cast not available
            }
        }
    }
}
