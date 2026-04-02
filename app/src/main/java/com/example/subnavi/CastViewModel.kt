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
import kotlinx.coroutines.delay
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
    private var discoveryCallback: MediaRouter.Callback? = null

    fun init(context: Context) {
        if (castContext != null) return
        try {
            castContext = CastContext.getSharedInstance(context)
            mediaRouter = MediaRouter.getInstance(context)
            routeSelector = MediaRouteSelector.Builder()
                .addControlCategory(
                    CastMediaControlIntent.categoryForCast(
                        CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
                    )
                )
                .build()

            // Start active discovery immediately
            val callback = object : MediaRouter.Callback() {
                override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
                    Log.d(TAG, "Discovery: route added — ${route.name}")
                }
                override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
                    Log.d(TAG, "Discovery: route changed — ${route.name}")
                }
                override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
                    Log.d(TAG, "Discovery: route removed — ${route.name}")
                }
            }
            discoveryCallback = callback
            mediaRouter!!.addCallback(
                routeSelector!!,
                callback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY
            )
            Log.d(TAG, "init: discovery started")

            startPolling()
        } catch (e: Exception) {
            Log.e(TAG, "init FAILED", e)
        }
    }

    fun showRouteSelector(context: Context) {
        val router = mediaRouter
        val selector = routeSelector
        if (router == null || selector == null) {
            Log.e(TAG, "showRouteSelector: not initialized")
            return
        }

        // If connected, disconnect
        if (CastHelper.isConnected(context)) {
            Log.d(TAG, "showRouteSelector: disconnecting")
            try {
                castContext?.sessionManager?.endCurrentSession(true)
            } catch (e: Exception) {
                Log.e(TAG, "endCurrentSession FAILED", e)
            }
            return
        }

        // Trigger a scan burst, wait 2s for discovery, then show dialog
        viewModelScope.launch {
            // Request active scan
            discoveryCallback?.let { cb ->
                try {
                    router.addCallback(selector, cb, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)
                } catch (_: Exception) {}
            }

            delay(2000)

            val routes = router.routes.filter { route ->
                route.isEnabled && route.matchesSelector(selector)
            }
            Log.d(TAG, "showRouteSelector: found ${routes.size} routes after scan")

            if (routes.isEmpty()) {
                AlertDialog.Builder(context)
                    .setTitle("Cast")
                    .setMessage("No devices found. Make sure your Chromecast is on the same WiFi network.")
                    .setPositiveButton("OK", null)
                    .show()
                return@launch
            }

            val names = routes.map { it.name }.toTypedArray()
            AlertDialog.Builder(context)
                .setTitle("Cast to device")
                .setItems(names) { _, which ->
                    val selected = routes[which]
                    Log.d(TAG, "Selected: ${selected.name}")
                    router.selectRoute(selected)
                }
                .show()
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            try {
                val ctx = castContext ?: return@launch
                while (true) {
                    _isConnected.value =
                        ctx.sessionManager.currentCastSession?.isConnected == true
                    delay(1000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "startPolling FAILED", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        discoveryCallback?.let { mediaRouter?.removeCallback(it) }
        castContext = null
        mediaRouter = null
    }
}
