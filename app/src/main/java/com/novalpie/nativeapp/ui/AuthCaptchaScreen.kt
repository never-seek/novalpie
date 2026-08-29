package com.novalpie.nativeapp.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.novalpie.nativeapp.data.ProxySettings
import com.novalpie.nativeapp.data.isEmulatorRuntime

/**
 * CAPTCHA is deliberately served by novalpie.cc. The native app owns credentials, validation,
 * request dispatch, and navigation; this isolated WebView exists only because CAPTCHA providers
 * validate the source website origin. It forwards a short-lived response token and never stores it.
 */
@SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
@Composable
fun AuthCaptchaScreen(
    proxySettings: ProxySettings,
    onToken: (String) -> Unit,
    onCancel: () -> Unit
) {
    val latestOnToken by rememberUpdatedState(onToken)
    val webStateKey = proxySettings.summary()
    var sourceNotice by remember(webStateKey) { mutableStateOf<String?>(null) }
    var pageLoading by remember(webStateKey) { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "请只完成验证码验证。账号密码仍在上一页的原生表单中填写。",
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        sourceNotice?.let { notice ->
            Text(
                notice,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        tag = captchaWebViewStateMarker(webStateKey)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.blockNetworkImage = false
                        installCaptchaAnonymousSourceGuard(this)
                        addJavascriptInterface(CaptchaBridge { latestOnToken(it) }, "NovalPieCaptcha")
                        webViewClient = captchaWebViewClient(
                            onSourceNotice = { sourceNotice = it },
                            onPageLoadingChanged = { pageLoading = it }
                        )
                        loadUrlAfterProxyReady(
                            webView = this,
                            url = CAPTCHA_LOGIN_URL,
                            settings = proxySettings,
                            webStateKey = webStateKey,
                            useEmulatorFallback = isEmulatorRuntime()
                        )
                    }
                },
                update = { webView ->
                    if (!webViewMatchesRequest(webView.tag, webStateKey, CAPTCHA_LOGIN_URL)) {
                        webView.tag = captchaWebViewStateMarker(webStateKey)
                        sourceNotice = null
                        pageLoading = true
                        loadUrlAfterProxyReady(
                            webView = webView,
                            url = CAPTCHA_LOGIN_URL,
                            settings = proxySettings,
                            webStateKey = webStateKey,
                            useEmulatorFallback = isEmulatorRuntime()
                        )
                    }
                }
            )
            if (pageLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                captchaLoadingStatusLabel(isLoading = true).orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("取消验证")
        }
    }
}

/**
 * The shared proxy loader only resumes a navigation when its route marker still matches.
 * CAPTCHA must use that marker too; a bare String tag causes the callback to skip /login.
 */
internal fun captchaWebViewStateMarker(webStateKey: String): WebViewStateMarker =
    WebViewStateMarker(
        stateKey = webStateKey,
        requestedUrl = CAPTCHA_LOGIN_URL
    )

internal fun captchaLoadingStatusLabel(isLoading: Boolean): String? =
    if (isLoading) "正在加载源站安全验证…" else null

/**
 * The source login page is a guest-only Nuxt route. A normal source WebView shares the embedded
 * site's persisted `auth_token`, so Nuxt redirects the CAPTCHA view to Collection after the
 * initial `/login` response has already finished. Hide only that one key before page JavaScript
 * runs; it remains intact for the normal authenticated web fallback and native auth is never read
 * or changed here.
 */
internal fun installCaptchaAnonymousSourceGuard(webView: WebView): Boolean {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return false
    return runCatching {
        WebViewCompat.addDocumentStartJavaScript(
            webView,
            CAPTCHA_ANONYMOUS_SOURCE_GUARD,
            setOf(CAPTCHA_SOURCE_ORIGIN)
        )
    }.isSuccess
}

/**
 * Web fallback pages intentionally share the source WebView profile so a logged-in user can move
 * between native and source-only routes. A CAPTCHA must be requested in an anonymous source page,
 * though: otherwise Nuxt immediately redirects `/login` to the authenticated landing page and no
 * verification widget is rendered. The document-start guard handles current WebViews; the retry
 * remains a fallback for old WebViews that cannot install that guard. Native auth is never read or
 * changed by either path.
 */
private fun captchaWebViewClient(
    onSourceNotice: (String?) -> Unit,
    onPageLoadingChanged: (Boolean) -> Unit
): WebViewClient {
    return object : WebViewClient() {
        private var retriedAnonymousLogin = false

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            onPageLoadingChanged(true)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            val uri = url?.let { raw -> runCatching { Uri.parse(raw) }.getOrNull() } ?: return
            if (!uri.host.equals("novalpie.cc", ignoreCase = true)) return
            // Nuxt performs the guest-route redirect with history APIs after this callback. Probe
            // after a short settle window so old WebViews without document-start scripts still
            // recover instead of leaving a fully rendered but unrelated Collection page here.
            view.postDelayed({
                view.evaluateJavascript(CAPTCHA_PAGE_SETTLE_PROBE) { rawState ->
                    when (rawState?.trim()) {
                        "\"login\"" -> {
                            onSourceNotice(null)
                            onPageLoadingChanged(false)
                            view.evaluateJavascript(CAPTCHA_TOKEN_POLL, null)
                        }

                        else -> recoverAnonymousLogin(view, onSourceNotice, onPageLoadingChanged)
                    }
                }
            }, CAPTCHA_PAGE_SETTLE_DELAY_MS)
        }

        private fun recoverAnonymousLogin(
            view: WebView,
            onSourceNotice: (String?) -> Unit,
            onPageLoadingChanged: (Boolean) -> Unit
        ) {
            onPageLoadingChanged(true)
            if (!retriedAnonymousLogin) {
                retriedAnonymousLogin = true
                onSourceNotice("正在准备源站安全验证…")
                view.evaluateJavascript(CAPTCHA_ANONYMOUS_LOGIN_RECOVERY, null)
            } else {
                onSourceNotice("源站网页登录会话仍在拦截安全验证，请返回后重试。")
            }
        }
    }
}

private class CaptchaBridge(private val onToken: (String) -> Unit) {
    private var delivered = false

    @JavascriptInterface
    fun complete(rawToken: String?) {
        val token = rawToken?.trim().takeIf { !it.isNullOrBlank() && it.length >= 20 } ?: return
        if (delivered) return
        delivered = true
        Handler(Looper.getMainLooper()).post { onToken(token) }
    }
}

internal const val CAPTCHA_LOGIN_URL = "https://novalpie.cc/login?native_captcha=1"

private const val CAPTCHA_SOURCE_ORIGIN = "https://novalpie.cc"
private const val CAPTCHA_PAGE_SETTLE_DELAY_MS = 600L

/**
 * Runs in the CAPTCHA WebView's document realm before Nuxt reads its storage. Do not remove the
 * stored source token: the ordinary web fallback intentionally retains it. The guard only masks
 * it for this guest-only page and blocks page code from reintroducing it during the same document.
 */
private const val CAPTCHA_ANONYMOUS_SOURCE_GUARD = """
(function () {
  var key = 'auth_token';
  try {
    document.cookie = key + '=; path=/; max-age=0; SameSite=Lax';
  } catch (e) {}
  try {
    var storagePrototype = Object.getPrototypeOf(window.localStorage);
    if (!storagePrototype.__novalpieCaptchaAuthGuard) {
      var getItem = storagePrototype.getItem;
      var setItem = storagePrototype.setItem;
      var removeItem = storagePrototype.removeItem;
      storagePrototype.getItem = function (name) {
        return String(name) === key ? null : getItem.call(this, name);
      };
      storagePrototype.setItem = function (name, value) {
        if (String(name) === key) return;
        return setItem.call(this, name, value);
      };
      storagePrototype.removeItem = function (name) {
        if (String(name) === key) return;
        return removeItem.call(this, name);
      };
      Object.defineProperty(storagePrototype, '__novalpieCaptchaAuthGuard', { value: true });
    }
  } catch (e) {}
})();
"""

private const val CAPTCHA_PAGE_SETTLE_PROBE = """
(function () {
  var path = String(location.pathname || '').replace(/\/+$/, '') || '/';
  return path === '/login' ? 'login' : 'other';
})()
"""

private const val CAPTCHA_TOKEN_POLL = """
(function () {
  if (window.__novalpieCaptchaPollInstalled) return true;
  window.__novalpieCaptchaPollInstalled = true;
  var delivered = '';
  function valueOfCaptchaResponse() {
    var selectors = [
      'input[name="cf-turnstile-response"]',
      'textarea[name="cf-turnstile-response"]',
      'textarea[name="g-recaptcha-response"]',
      'textarea[name="h-captcha-response"]',
      'input[name*="turnstile"]'
    ];
    for (var i = 0; i < selectors.length; i++) {
      var element = document.querySelector(selectors[i]);
      var value = element && element.value ? String(element.value).trim() : '';
      if (value.length >= 20) return value;
    }
    return '';
  }
  function emit() {
    var token = valueOfCaptchaResponse();
    if (token && token !== delivered && window.NovalPieCaptcha) {
      delivered = token;
      window.NovalPieCaptcha.complete(token);
    }
  }
  setInterval(emit, 350);
  new MutationObserver(emit).observe(document.documentElement, { childList: true, subtree: true, attributes: true });
  emit();
  return true;
})()
"""

private const val CAPTCHA_ANONYMOUS_LOGIN_RECOVERY = """
(function () {
  try {
    localStorage.removeItem('auth_token');
    sessionStorage.removeItem('auth_token');
  } catch (e) {}
  location.replace('/login?native_captcha=1');
  return true;
})()
"""
