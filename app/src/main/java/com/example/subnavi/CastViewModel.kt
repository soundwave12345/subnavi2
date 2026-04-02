package com.example.subnavi

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subnavi.cast.CastHelper
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastContext
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
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

    private var castContext: CastContext? = null
    private var mediaRouter: MediaRouter? = null
    private var routeSelector: MediaRouteSelector? = null

    fun init(context: Context) {
        if (castContext != null) return
        try {
            castContext = CastContext.getSharedInstance(context)
            Log.d(TAG, "init: CastContext OK")

            mediaRouter = MediaRouter.getInstance(context)
            routeSelector = MediaRouteSelector.Builder()
                .addControlCategory(
                    CastMediaControlIntent.categoryForCast(
                        CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
                    )
                )
                .build()
            Log.d(TAG, "init: MediaRouter + selector OK")

            startPolling()
        } catch (e: Exception) {
            Log.e(TAG, "init FAILED", e)
        }
    }

    fun showRouteSelector(context: Context) {
        Log.d(TAG, "showRouteSelector called")
        val router = mediaRouter
        val selector = routeSelector

        if (router == null || selector == null) {
            Log.e(TAG, "showRouteSelector: router=$router selector=$selector — not initialized")
            return
        }

        // Check if connected — if so, disconnect
        if (CastHelper.isConnected(context)) {
            Log.d(TAG, "showRouteSelector: disconnecting current session")
            try {
                castContext?.sessionManager?.endCurrentSession(true)
            } catch (e: Exception) {
                Log.e(TAG, "endCurrentSession FAILED", e)
            }
            return
        }

        // Build list of available routes
        val routes = router.routes.filter { route ->
            route.isEnabled && route.matchesSelector(selector)
        }
        Log.d(TAG, "showRouteSelector: found ${routes.size} routes")

        if (routes.isEmpty()) {
            Log.d(TAG, "showRouteSelector: no routes, showing message")
            AlertDialog.Builder(context)
                .setTitle("Cast")
                .setMessage("No devices found. Make sure your Chromecast is on the same WiFi network.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val names = routes.map { it.name }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle("Cast to device")
            .setItems(names) { _, which ->
                val selected = routes[which]
                Log.d(TAG, "showRouteSelector: selected route=${selected.name} (${selected.id})")
                router.selectRoute(selected)
            }
            .show()
    }

    private fun startPolling() {
        viewModelScope.launch {
            try {
                val ctx = castContext ?: return@launch
                while (true) {
                    _isConnected.value =
                        ctx.sessionManager.currentCastSession?.isConnected == true
                    kotlinx.coroutines.delay(1000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "startPolling FAILED", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        castContext = null
        mediaRouter = null
    }
}
