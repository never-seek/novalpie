package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.data.ProxySettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebFallbackPolicyTest {
    @Test
    fun existingWebTokenIsNeverOverwrittenByAnOlderNativeToken() {
        assertNull(
            webFallbackTokenToSeed(
                existingWebToken = "fresh-web-session",
                nativeToken = "stale-native-session",
            )
        )
    }

    @Test
    fun explicitProxyAlwaysWins() {
        val settings = ProxySettings(enabled = true, host = "proxy.example", port = 8080)

        assertEquals(
            "http://proxy.example:8080",
            webViewProxyUrl(settings, useEmulatorFallback = true)
        )
    }

    @Test
    fun emulatorWebViewUsesTheReverseLoopbackProxyBeforeLoadingCaptcha() {
        val settings = ProxySettings(enabled = false)

        assertEquals(
            "http://127.0.0.1:7890",
            webViewProxyUrl(settings, useEmulatorFallback = true)
        )
    }

    @Test
    fun realDevicesKeepFollowingTheSystemNetwork() {
        assertNull(
            webViewProxyUrl(ProxySettings(enabled = false), useEmulatorFallback = false)
        )
    }

    @Test
    fun stateMarkerPreventsAnOlderProxyCallbackFromReloadingTheDownloadPage() {
        val marker = WebViewStateMarker(
            stateKey = "auto: 127.0.0.1/10.0.2.2:7890 + direct:token",
            requestedUrl = "https://novalpie.cc/book/354491"
        )

        assertEquals(
            true,
            webViewMatchesRequest(
                marker,
                "auto: 127.0.0.1/10.0.2.2:7890 + direct:token",
                "https://novalpie.cc/book/354491"
            )
        )
        assertEquals(
            false,
            webViewMatchesRequest(
                marker,
                "auto: 127.0.0.1/10.0.2.2:7890 + direct:token",
                "https://novalpie.cc/book/354491?download=epub"
            )
        )
        assertEquals(
            false,
            webViewMatchesRequest(
                WebViewStateMarker("new-route", "https://novalpie.cc/search"),
                marker.stateKey,
                marker.requestedUrl,
            )
        )
    }
}
