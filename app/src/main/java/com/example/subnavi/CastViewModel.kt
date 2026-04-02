package com.example.subnavi

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subnavi.cast.CastHelper
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import androidx.mediarouter.app.MediaRouteButton
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

    // Hidden MediaRouteButton used only to trigger the Cast dialog
    private var mediaRouteButton: MediaRouteButton? = null

    fun ensureCastButton(context: Context) {
        if (mediaRouteButton != null) return
        try {
            val themedContext = ContextThemeWrapper(context, android.R.style.Theme_Material)
            val button = MediaRouteButton(themedContext)
            CastButtonFactory.setUpMediaRouteButton(context, button)
            mediaRouteButton = button
            startPolling(context)
        } catch (e: Exception) {
            // Cast not available on this device
        }
    }

    fun showRouteSelector() {
        mediaRouteButton?.performClick()
    }

    private fun startPolling(context: Context) {
        viewModelScope.launch {
            try {
                while (true) {
                    _isConnected.value = CastHelper.isConnected(context)
                    kotlinx.coroutines.delay(1000)
                }
            } catch (e: Exception) {
                // Cast not available
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaRouteButton = null
    }
}
