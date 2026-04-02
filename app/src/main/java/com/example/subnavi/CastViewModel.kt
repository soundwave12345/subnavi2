package com.example.subnavi

import android.content.Context
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subnavi.cast.CastHelper
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import androidx.mediarouter.app.MediaRouteButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CastViewModel @Inject constructor() : ViewModel() {

    companion object {
        private const val TAG = "SUBNAVI_CAST"
    }

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var mediaRouteButton: MediaRouteButton? = null

    fun ensureCastButton(context: Context) {
        if (mediaRouteButton != null) {
            Log.d(TAG, "ensureCastButton: already initialized")
            return
        }
        Log.d(TAG, "ensureCastButton: initializing...")
        try {
            val themedContext = ContextThemeWrapper(context, android.R.style.Theme_Material)
            val button = MediaRouteButton(themedContext)
            Log.d(TAG, "ensureCastButton: MediaRouteButton created OK")

            CastButtonFactory.setUpMediaRouteButton(context, button)
            Log.d(TAG, "ensureCastButton: CastButtonFactory wired OK")

            mediaRouteButton = button

            // Check cast state
            val castState = CastContext.getSharedInstance(context).castState
            Log.d(TAG, "ensureCastButton: castState=$castState " +
                "(0=NO_DEVICES, 1=NOT_CONNECTED, 2=CONNECTING, 3=CONNECTED)")

            startPolling(context)
        } catch (e: Exception) {
            Log.e(TAG, "ensureCastButton FAILED", e)
        }
    }

    fun showRouteSelector() {
        Log.d(TAG, "showRouteSelector: button=${mediaRouteButton}")
        if (mediaRouteButton == null) {
            Log.e(TAG, "showRouteSelector: mediaRouteButton is NULL, cannot show dialog")
            return
        }
        try {
            val result = mediaRouteButton!!.performClick()
            Log.d(TAG, "showRouteSelector: performClick returned $result")
        } catch (e: Exception) {
            Log.e(TAG, "showRouteSelector: performClick FAILED", e)
        }
    }

    private fun startPolling(context: Context) {
        viewModelScope.launch {
            try {
                while (true) {
                    _isConnected.value = CastHelper.isConnected(context)
                    kotlinx.coroutines.delay(1000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "startPolling FAILED", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaRouteButton = null
    }
}
