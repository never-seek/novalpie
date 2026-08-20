package com.novalpie.nativeapp.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
    var sourceNotice by remember { mutableStateOf<String?>(null) }
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
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { context ->
                WebView(context).apply {
                    tag = captchaWebViewStateMarker(webStateKey)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    addJavascriptInterface(CaptchaBridge { latestOnToken(it) }, "NovalPieCaptcha")
                    webViewClient = captchaWebViewClient { sourceNotice = it }
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

/**
 * Web fallback pages intentionally share the source WebView profile so a logged-in user can move
 * between native and source-only routes. A CAPTCHA must be requested in an anonymous source page,
 * though: otherwise Nuxt immediately redirects `/login` to the authenticated landing page and no
 * verification widget is rendered. Clear only the source page's injected local token and retry
 * once; the app's own stored token and all cookies remain untouched.
 */
private fun captchaWebViewClient(onSourceNotice: (String?) -> Unit): WebViewClient {
    return object : WebViewClient() {
        private var retriedAnonymousLogin = false

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            val uri = url?.let { raw -> runCatching { Uri.parse(raw) }.getOrNull() } ?: return
            if (!uri.host.equals("novalpie.cc", ignoreCase = true)) return

            if (uri.path?.trimEnd('/') == "/login") {
                onSourceNotice(null)
                view.evaluateJavascript(CAPTCHA_TOKEN_POLL, null)
                return
            }

            if (!retriedAnonymousLogin) {
                retriedAnonymousLogin = true
                onSourceNotice("正在准备源站安全验证…")
                view.evaluateJavascript(CAPTCHA_ANONYMOUS_LOGIN_RECOVERY, null)
            } else {
                onSourceNotice("源站现有网页登录会话阻止了验证码显示，请取消验证后使用源站网页登录切换账号。")
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

internal const val CAPTCHA_LOGIN_URL = "https://novalpie.cc/login"

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
