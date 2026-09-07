package com.novalpie.nativeapp.ui

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.audit.CaptchaAuditActivity
import com.novalpie.nativeapp.core.AppContainer
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AuthCaptchaLiveDeviceTest {
    @Test fun sourceVerificationIsVisibleAndCancelDoesNotInvalidateTheExistingAccount() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val api = AppContainer.from(context).api
        val before = api.currentUser().id
        assertNotNull("Existing logged-in QA account required", before)
        var verified = false
        var summary: JSONObject? = null
        fun web(view: View): WebView? {
            if (view is WebView) return view
            if (view is ViewGroup) for (i in 0 until view.childCount) web(view.getChildAt(i))?.let { return it }
            return null
        }
        val scenario = ActivityScenario.launch(CaptchaAuditActivity::class.java)
        try {
            withTimeout(60000) {
                while (true) {
                    val probe = CompletableDeferred<String?>()
                    scenario.onActivity { activity ->
                        verified = activity.verified
                        if (verified) probe.complete(null) else web(activity.window.decorView)?.evaluateJavascript("""
                            JSON.stringify({path:location.pathname,height:innerHeight,
                              captchaVisible:[...document.querySelectorAll('iframe,input[name="cf-turnstile-response"]')].filter(e=>e.tagName==='INPUT'
                                ? e.parentElement.getBoundingClientRect().height>0
                                : e.getBoundingClientRect().height>0&&/challenges\.cloudflare|recaptcha|hcaptcha/.test(e.src)).length,
                              providers:[...document.querySelectorAll('button')].map(e=>e.textContent.trim()).filter(s=>['Turnstile','reCAPTCHA','hCaptcha'].includes(s))})
                        """.trimIndent()) { probe.complete(it) } ?: probe.complete(null)
                    }
                    val raw = withTimeout(2000) { probe.await() }
                    if (verified) break
                    if (raw != null && raw != "null") {
                        val jsonText = org.json.JSONArray("[$raw]").getString(0)
                        summary = runCatching { JSONObject(jsonText) }.getOrNull()
                        if (summary?.optInt("captchaVisible", 0) ?: 0 > 0) break
                    }
                    delay(400)
                }
            }
            assertTrue("必须显示真实验证控件或得到验证回填", verified || (summary?.optInt("captchaVisible") ?: 0) > 0)
        } finally {
            scenario.close()
            assertEquals("取消或完成验证不能退出原有账号", before, api.currentUser().id)
        }
        val report = (summary ?: JSONObject()).put("verifiedCallback", verified).put("existingAccountRetained", true).put("loginSubmitted", false)
        val folder = File(context.getExternalFilesDir(null), "beta7-qa").apply { mkdirs() }
        File(folder, "captcha-live.json").writeText(report.toString(2))
    }
}
