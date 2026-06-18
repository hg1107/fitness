package com.example.fitnesstracker.ui.screens.track

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fitnesstracker.service.LocationPoint

@Composable
fun ActivityMapView(
    routePoints: List<LocationPoint>,
    currentLocation: LocationPoint?,
    isDarkMode: Boolean,
    fitRouteBounds: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapInitialized by remember { mutableStateOf(false) }
    // track how many points were already sent to avoid re-sending the full route
    var lastSentPointCount by remember { mutableStateOf(0) }

    // Build only the new incremental points (delta) instead of the full array
    val newPointsJson = remember(routePoints) {
        if (routePoints.size <= lastSentPointCount) return@remember "[]"
        val newPoints = routePoints.drop(lastSentPointCount)
        val sb = StringBuilder()
        sb.append("[")
        newPoints.forEachIndexed { index, p ->
            sb.append("{\"latitude\":${p.latitude},\"longitude\":${p.longitude}}")
            if (index < newPoints.size - 1) sb.append(",")
        }
        sb.append("]")
        sb.toString()
    }

    // For initial load we still need the full route
    val fullPointsJson = remember(routePoints) {
        val sb = StringBuilder()
        sb.append("[")
        routePoints.forEachIndexed { index, p ->
            sb.append("{\"latitude\":${p.latitude},\"longitude\":${p.longitude}}")
            if (index < routePoints.size - 1) sb.append(",")
        }
        sb.append("]")
        sb.toString()
    }

    LaunchedEffect(isDarkMode, isMapInitialized) {
        if (isMapInitialized) {
            val themeStr = if (isDarkMode) "dark" else "light"
            webViewRef?.evaluateJavascript("setMapTheme('$themeStr')", null)
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewRef = this
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true           // needed for android_asset/
                @Suppress("DEPRECATION")
                settings.allowFileAccessFromFileURLs = false // disable unsafe file access
                @Suppress("DEPRECATION")
                settings.allowUniversalAccessFromFileURLs = false // disable unsafe universal access
                
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        android.util.Log.d("MapWebView", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isMapInitialized = true
                        
                        val startLat = currentLocation?.latitude ?: 0.0
                        val startLon = currentLocation?.longitude ?: 0.0
                        val themeStr = if (isDarkMode) "dark" else "light"
                        
                        evaluateJavascript("initMap($startLat, $startLon, '$themeStr')", null)
                        
                        if (routePoints.isNotEmpty()) {
                            evaluateJavascript("setRoute('$fullPointsJson', $fitRouteBounds)", null)
                            lastSentPointCount = routePoints.size
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        android.util.Log.e("MapWebView", "Network Error: ${error?.description} for URL: ${request?.url}")
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        errorResponse: android.webkit.WebResourceResponse?
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        android.util.Log.e("MapWebView", "HTTP Error: ${errorResponse?.statusCode} ${errorResponse?.reasonPhrase} for URL: ${request?.url}")
                    }
                }
                loadUrl("file:///android_asset/map.html")
            }
        },
        update = { webView ->
            if (isMapInitialized) {
                if (currentLocation != null) {
                    webView.evaluateJavascript("updateCurrentLocation(${currentLocation.latitude}, ${currentLocation.longitude})", null)
                }
                if (routePoints.size > lastSentPointCount && newPointsJson != "[]") {
                    webView.evaluateJavascript("appendRoute('$newPointsJson', $fitRouteBounds)", null)
                    lastSentPointCount = routePoints.size
                } else if (routePoints.isEmpty()) {
                    webView.evaluateJavascript("clearRoute()", null)
                    lastSentPointCount = 0
                }
            }
        },
        modifier = modifier
    )
}
