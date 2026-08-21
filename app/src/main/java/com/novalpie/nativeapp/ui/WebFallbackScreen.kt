package com.novalpie.nativeapp.ui

import android.annotation.SuppressLint
import android.webkit.DownloadListener
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import com.novalpie.nativeapp.data.ProxySettings
import com.novalpie.nativeapp.data.isEmulatorRuntime
import com.novalpie.nativeapp.data.preferredEmulatorProxyHosts
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executor

@SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
@Composable
fun WebFallbackScreen(
    url: String,
    proxySettings: ProxySettings,
    authToken: String?,
    onAuthTokenCaptured: (String) -> Unit
) {
    val proxyKey = proxySettings.summary()
    val webStateKey = "$proxyKey:${authToken.orEmpty()}"
    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val useEmulatorFallback = isEmulatorRuntime()
    val context = LocalContext.current
    val bridge = remember(context) { WebDownloadBridge(context) }

    DisposableEffect(bridge) {
        bridge.start()
        onDispose {
            bridge.stop()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                tag = WebViewStateMarker(webStateKey, url)
                setBackgroundColor(backgroundColor)
                webViewClient = authSyncingWebViewClient(authToken, onAuthTokenCaptured, bridge)
                settings.javaScriptEnabled = true
                // domStorageEnabled is the one that matters: the auth_token this screen
                // captures lives in localStorage. databaseEnabled gated the separate
                // WebSQL API, which modern WebView has removed outright, so setting it
                // was already a no-op.
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.loadsImagesAutomatically = true
                settings.blockNetworkImage = false
                settings.defaultTextEncodingName = "UTF-8"
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                addJavascriptInterface(bridge.javascriptInterface, "AndroidDownload")
                // Register before the first navigation: website EPUB generation can create and
                // revoke a Blob URL before onPageFinished is reached.
                bridge.installDocumentStartBridge(this)
                setDownloadListener(DownloadListener { downloadUrl, _, contentDisposition, mimeType, _ ->
                    bridge.captureDownloadListenerBlob(this, downloadUrl, filenameFromContentDisposition(contentDisposition), mimeType)
                })
                loadUrlAfterProxyReady(this, url, proxySettings, webStateKey, useEmulatorFallback)
            }
        },
        update = { webView ->
            webView.setBackgroundColor(backgroundColor)
            val marker = webView.tag as? WebViewStateMarker
            val stateChanged = marker == null || marker.stateKey != webStateKey
            val requestedUrlChanged = marker == null || marker.requestedUrl != url
            if (stateChanged || requestedUrlChanged) {
                webView.webViewClient = authSyncingWebViewClient(authToken, onAuthTokenCaptured, bridge)
                webView.tag = WebViewStateMarker(webStateKey, url)
                loadUrlAfterProxyReady(webView, url, proxySettings, webStateKey, useEmulatorFallback)
            }
        },
        onRelease = { webView ->
            // A WebView can outlive the route while an AndroidView is being detached. Destroy it
            // explicitly so a page cannot continue generating the same EPUB in the background.
            bridge.detachWebView(webView)
            webView.stopLoading()
            webView.webChromeClient = null
            webView.setDownloadListener(null)
            webView.removeJavascriptInterface("AndroidDownload")
            webView.destroy()
        }
    )
}

internal data class WebViewStateMarker(
    val stateKey: String,
    val requestedUrl: String,
)

/**
 * The proxy override callback is asynchronous. It must only navigate the WebView if this is still
 * the exact route that requested the override; matching only proxy/auth state can reload a stale
 * EPUB page after navigation or recomposition.
 */
internal fun webViewMatchesRequest(
    tag: Any?,
    stateKey: String,
    requestedUrl: String,
): Boolean = (tag as? WebViewStateMarker)?.let {
    it.stateKey == stateKey && it.requestedUrl == requestedUrl
} == true

@SuppressLint("RequiresFeature") // The explicit guard below precedes every ProxyController call.
internal fun loadUrlAfterProxyReady(
    webView: WebView,
    url: String,
    settings: ProxySettings,
    webStateKey: String,
    useEmulatorFallback: Boolean
) {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
        webView.loadUrl(url)
        return
    }

    val executor = Executor { command ->
        Handler(Looper.getMainLooper()).post(command)
    }

    runCatching {
        val controller = ProxyController.getInstance()
        val loadWhenReady = Runnable {
            webView.post {
                if (webViewMatchesRequest(webView.tag, webStateKey, url)) {
                    webView.loadUrl(url)
                }
            }
        }
        val proxyUrl = webViewProxyUrl(settings, useEmulatorFallback)
        if (proxyUrl != null) {
            val config = ProxyConfig.Builder()
                .addProxyRule(proxyUrl)
                .addBypassRule("127.0.0.1")
                .addBypassRule("localhost")
                .bypassSimpleHostnames()
                .build()
            controller.setProxyOverride(config, executor, loadWhenReady)
        } else {
            controller.clearProxyOverride(executor, loadWhenReady)
        }
    }.onFailure {
        // ProxyController can fail asynchronously after the route has changed. Keep the
        // fallback subject to the same marker check as the success callback so an old WebView
        // request cannot resurrect a download page after the user navigates away.
        webView.post {
            if (webViewMatchesRequest(webView.tag, webStateKey, url)) {
                webView.loadUrl(url)
            }
        }
    }
}

internal fun webViewProxyUrl(
    settings: ProxySettings,
    useEmulatorFallback: Boolean
): String? {
    val host = settings.host.trim()
    if (settings.enabled && host.isNotBlank() && settings.port in 1..65535) {
        return "http://$host:${settings.port}"
    }
    if (!useEmulatorFallback) return null
    // WebView accepts one proxy rule, so it needs the same first route that OkHttp uses. MuMu's
    // VBox runtime only exposes the host proxy through adb reverse at 127.0.0.1; hard-coding the
    // AVD-only 10.0.2.2 address made the CAPTCHA page fail before it could paint anything.
    val emulatorHost = preferredEmulatorProxyHosts().firstOrNull() ?: return null
    return "http://$emulatorHost:${ProxySettings.DEFAULT_PROXY_PORT}"
}

private fun authSyncingWebViewClient(
    authToken: String?,
    onAuthTokenCaptured: (String) -> Unit,
    bridge: WebDownloadBridge
): WebViewClient {
    return object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            bridge.injectInto(view)
            syncAuthToken(view, authToken, onAuthTokenCaptured)
        }
    }
}

internal fun filenameFromContentDisposition(value: String?): String? {
    val text = value?.trim().orEmpty()
    if (text.isBlank()) return null
    val encoded = Regex("filename\\*\\s*=\\s*(?:UTF-8'')?([^;]+)", RegexOption.IGNORE_CASE)
        .find(text)?.groupValues?.getOrNull(1)
    val plain = Regex("filename\\s*=\\s*(\"[^\"]+\"|[^;]+)", RegexOption.IGNORE_CASE)
        .find(text)?.groupValues?.getOrNull(1)
    return (encoded ?: plain)
        ?.trim()
        ?.removeSurrounding("\"")
        ?.let { raw -> runCatching { java.net.URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw) }
}

private fun syncAuthToken(
    webView: WebView,
    authToken: String?,
    onAuthTokenCaptured: (String) -> Unit
) {
    authToken?.takeIf { it.isNotBlank() }?.let { token ->
        val quoted = JSONObject.quote(token)
        webView.evaluateJavascript(
            """
            (function(){
              try {
                if (localStorage.getItem('auth_token') !== $quoted) {
                  localStorage.setItem('auth_token', $quoted);
                  if (location.pathname !== '/login') location.reload();
                }
              } catch (e) {}
              return true;
            })()
            """.trimIndent(),
            null
        )
    }

    webView.evaluateJavascript(
        """
        (function(){
          try {
            var token = localStorage.getItem('auth_token') || '';
            if (!token) {
              var match = document.cookie.match(/(?:^|;\s*)auth_token=([^;]+)/);
              if (match) token = decodeURIComponent(match[1]);
            }
            return token || '';
          } catch (e) {
            return '';
          }
        })()
        """.trimIndent()
    ) { raw ->
        decodeJavascriptString(raw)
            ?.takeIf { it.isNotBlank() }
            ?.let(onAuthTokenCaptured)
    }
}

private fun decodeJavascriptString(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.isBlank() || value == "null") return null
    return runCatching { JSONArray("[$value]").optString(0) }.getOrNull()
}
