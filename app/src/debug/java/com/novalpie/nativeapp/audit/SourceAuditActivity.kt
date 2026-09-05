package com.novalpie.nativeapp.audit

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.ui.NovalPieTheme
import com.novalpie.nativeapp.ui.WebFallbackScreen

/**
 * Only present in developer builds. Reuses the application's existing authenticated source
 * WebView for mobile control/API auditing; it is not a substitute for any native feature.
 * No session values are sent to the audit host. The beta/release source sets exclude this class.
 */
class SourceAuditActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra("path")?.takeIf {
            it.startsWith('/') && !it.startsWith("//") && !it.contains('\\')
        } ?: "/favorites"
        val container = AppContainer.from(this)
        WebView.setWebContentsDebuggingEnabled(true)
        setContent {
            NovalPieTheme {
                WebFallbackScreen(
                    url = "https://novalpie.cc$path",
                    proxySettings = container.environment.proxy,
                    authToken = container.environment.token,
                    onAuthTokenCaptured = container.environment::setToken,
                )
            }
        }
    }

    override fun onDestroy() {
        WebView.setWebContentsDebuggingEnabled(false)
        super.onDestroy()
    }
}
