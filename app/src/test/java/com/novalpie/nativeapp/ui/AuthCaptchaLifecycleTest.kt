package com.novalpie.nativeapp.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthCaptchaLifecycleTest {
    @Test fun mainFrameErrorStopsTheUnendingLoadingState() {
        var loading = true; var notice: String? = null
        val view = WebView(ApplicationProvider.getApplicationContext())
        val client = captchaWebViewClient({ notice = it }, { loading = it })
        @Suppress("DEPRECATION")
        client.onReceivedError(view, WebViewClient.ERROR_HOST_LOOKUP, "fixture", CAPTCHA_LOGIN_URL)
        assertFalse(loading)
        assertFalse(notice.isNullOrBlank())
        view.destroy()
    }
    @Test fun firstVisiblePageIsNotCoveredUntilThirdPartyResourcesFinish() {
        var loading = true
        val view = WebView(ApplicationProvider.getApplicationContext())
        captchaWebViewClient({}, { loading = it }).onPageCommitVisible(view, CAPTCHA_LOGIN_URL)
        assertFalse(loading)
        view.destroy()
    }
    @Test fun anonymousCaptchaGuardNeverDeletesOrExpiresSharedLoginStorage() {
        val scripts = CAPTCHA_ANONYMOUS_SOURCE_GUARD + CAPTCHA_ANONYMOUS_LOGIN_RECOVERY
        assertFalse("不能让验证码流程清掉网页登录Cookie", scripts.contains("max-age=0", ignoreCase = true))
        assertFalse("不能清共享localStorage", scripts.contains("localStorage.removeItem('auth_token')"))
        assertFalse("不能清共享sessionStorage", scripts.contains("sessionStorage.removeItem('auth_token')"))
    }
}
