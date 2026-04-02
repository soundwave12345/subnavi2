package com.example.subnavi

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subnavi.cast.CastHelper
import com.google.android.gms.cast.framework.CastContext
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

    private var polling = false

    fun showRouteSelector(context: Context) {
        try {
            val castContext = CastContext.getSharedInstance(context)
            val sessionManager = castContext.sessionManager
            // If connected, end session. Otherwise, open the device picker dialog.
            if (sessionManager.currentCastSession?.isConnected == true) {
                sessionManager.endCurrentSession(true)
            } else {
                // Use MediaRouter to show the route chooser dialog
                val mediaRouter = androidx.mediarouter.media.MediaRouter.getInstance(context)
                val selector = androidx.mediarouter.media.MediaRouteSelector.Builder()
                    .addControlCategory(
                        com.google.android.gms.cast.CastMediaControlIntent
                            .categoryForCast(
                                com.google.android.gms.cast.CastMediaControlIntent
                                    .DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
                            )
                    )
                    .build()
                val dialog = androidx.mediarouter.app.MediaRouteChooserDialog(context)
                dialog.setRouteSelector(selector)
                dialog.show()
            }
        } catch (e: Exception) {
            // Cast not available on this device
        }
        startPolling(context)
    }

    private fun startPolling(context: Context) {
        if (polling) return
        polling = true
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
}
