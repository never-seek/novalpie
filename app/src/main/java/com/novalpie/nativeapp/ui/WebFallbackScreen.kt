package com.novalpie.nativeapp.ui

import android.annotation.SuppressLint
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.novalpie.nativeapp.data.ProxySettings
import com.novalpie.nativeapp.data.shouldPreferEmulatorProxy
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executor

@SuppressLint("SetJavaScriptEnabled")
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
    val useEmulatorFallback = shouldPreferEmulatorProxy()

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                tag = webStateKey
                setBackgroundColor(backgroundColor)
                webViewClient = authSyncingWebViewClient(authToken, onAuthTokenCaptured)
                settings.javaScriptEnabled = true
                // domStorageEnabled is the one that matters: the auth_token this screen
                // captures lives in localStorage. databaseEnabled gated the separate
                // WebSQL API, which modern WebView has removed outright, so setting it
                // was already a no-op.
                settings.domStorageEnabled = true
                loadUrlAfterProxyReady(this, url, proxySettings, webStateKey, useEmulatorFallback)
            }
        },
        update = { webView ->
            webView.setBackgroundColor(backgroundColor)
            webView.webViewClient = authSyncingWebViewClient(authToken, onAuthTokenCaptured)
            val proxyChanged = webView.tag != webStateKey
            if (proxyChanged) webView.tag = webStateKey
            if (proxyChanged || webView.url != url) {
                loadUrlAfterProxyReady(webView, url, proxySettings, webStateKey, useEmulatorFallback)
            }
        }
    )
}

private fun loadUrlAfterProxyReady(
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
                if (webView.tag == webStateKey) {
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
        webView.loadUrl(url)
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
    return "http://${ProxySettings.DEFAULT_EMULATOR_PROXY_HOSTS.first()}:${ProxySettings.DEFAULT_PROXY_PORT}"
}

private fun authSyncingWebViewClient(
    authToken: String?,
    onAuthTokenCaptured: (String) -> Unit
): WebViewClient {
    return object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            syncAuthToken(view, authToken, onAuthTokenCaptured)
        }
    }
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
