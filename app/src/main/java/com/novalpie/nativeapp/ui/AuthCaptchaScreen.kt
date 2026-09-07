package com.novalpie.nativeapp.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.webkit.SslErrorHandler
import android.net.http.SslError
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
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
    var attempt by remember { mutableIntStateOf(0) }
    val webStateKey = "${proxySettings.summary()}:captcha:$attempt"
    var sourceNotice by remember(webStateKey) { mutableStateOf<String?>(null) }
    var pageLoading by remember(webStateKey) { mutableStateOf(true) }
    LaunchedEffect(webStateKey, pageLoading) {
        if (pageLoading) {
            kotlinx.coroutines.delay(20000)
            pageLoading = false
            sourceNotice = "验证页面加载超时。可在下方继续验证、切换网站提供的验证方式，或重试。"
        }
    }
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
        if (pageLoading) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(captchaLoadingStatusLabel(true).orEmpty(), style = MaterialTheme.typography.bodySmall)
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            key(webStateKey) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        tag = captchaWebViewStateMarker(webStateKey)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.blockNetworkImage = false
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        installCaptchaAnonymousSourceGuard(this)
                        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                            var delivered = false
                            WebViewCompat.addWebMessageListener(this, "NovalPieCaptcha", setOf(CAPTCHA_SOURCE_ORIGIN)) { _, message, origin, mainFrame, _ ->
                                val token = message.data?.trim().orEmpty()
                                if (!delivered && mainFrame && origin.scheme == "https" && origin.host == "novalpie.cc" && token.length in 20..16384) {
                                    delivered = true
                                    latestOnToken(token)
                                }
                            }
                        } else sourceNotice = "系统 WebView 太旧，无法安全回填验证结果；请更新 Android System WebView 后重试。"
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
                onRelease = { webView ->
                    webView.tag = null
                    webView.stopLoading()
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) WebViewCompat.removeWebMessageListener(webView, "NovalPieCaptcha")
                    webView.destroy()
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
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { attempt++ }, modifier = Modifier.weight(1f)) { Text("重新加载验证") }
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("取消验证") }
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
internal fun captchaWebViewClient(
    onSourceNotice: (String?) -> Unit,
    onPageLoadingChanged: (Boolean) -> Unit
): WebViewClient {
    return object : WebViewClient() {
        private var retriedAnonymousLogin = false

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            onPageLoadingChanged(true)
        }

        override fun onPageCommitVisible(view: WebView, url: String?) {
            onPageLoadingChanged(false)
        }

        @Suppress("DEPRECATION")
        override fun onReceivedError(view: WebView, code: Int, description: String?, failingUrl: String?) {
            onPageLoadingChanged(false)
            onSourceNotice("验证页面连接失败（$code），请检查网络后重新加载。")
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) {
                onPageLoadingChanged(false)
                onSourceNotice("验证页面连接失败（${error.errorCode}），请检查网络后重新加载。")
            }
        }

        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
            if (request.isForMainFrame) {
                onPageLoadingChanged(false)
                onSourceNotice("验证页面返回 HTTP ${errorResponse.statusCode}；下方若有网站验证请先完成，或重新加载。")
            }
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            handler.cancel()
            onPageLoadingChanged(false)
            onSourceNotice("验证页面安全连接失败，请检查网络或设备时间后重试。")
        }

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            onPageLoadingChanged(false)
            val uri = url?.let { raw -> runCatching { Uri.parse(raw) }.getOrNull() } ?: return
            if (!uri.host.equals("novalpie.cc", ignoreCase = true)) return
            // Nuxt performs the guest-route redirect with history APIs after this callback. Probe
            // after a short settle window so old WebViews without document-start scripts still
            // recover instead of leaving a fully rendered but unrelated Collection page here.
            view.postDelayed({
                if (view.tag == null) return@postDelayed
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
            onPageLoadingChanged(false)
            if (!retriedAnonymousLogin) {
                retriedAnonymousLogin = true
                onSourceNotice("正在准备源站安全验证…")
                view.evaluateJavascript(CAPTCHA_ANONYMOUS_LOGIN_RECOVERY, null)
            } else {
                onSourceNotice("源站没有展示登录验证。请重新加载；未清除你的网页登录状态。")
            }
        }
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
internal const val CAPTCHA_ANONYMOUS_SOURCE_GUARD = """
(function () {
  var key = 'auth_token';
  try {
    // Mask only this document's view, never delete the shared cookie jar.
    var cookieOwner = Document.prototype;
    var cookie = Object.getOwnPropertyDescriptor(cookieOwner, 'cookie');
    if (!cookie && window.HTMLDocument) cookie = Object.getOwnPropertyDescriptor(HTMLDocument.prototype, 'cookie');
    if (cookie && cookie.get && cookie.set) Object.defineProperty(document, 'cookie', {
      configurable: true,
      get: function () { return cookie.get.call(document).split(';').filter(function (item) { return item.trim().split('=')[0] !== key; }).join(';'); },
      set: function (value) { if (String(value).trim().split('=')[0] !== key) cookie.set.call(document, value); }
    });
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
      window.NovalPieCaptcha.postMessage(token);
    }
  }
  setInterval(emit, 350);
  new MutationObserver(emit).observe(document.documentElement, { childList: true, subtree: true, attributes: true });
  emit();
  return true;
})()
"""

internal const val CAPTCHA_ANONYMOUS_LOGIN_RECOVERY = """
(function () {
  location.replace('/login?native_captcha=1');
  return true;
})()
"""
