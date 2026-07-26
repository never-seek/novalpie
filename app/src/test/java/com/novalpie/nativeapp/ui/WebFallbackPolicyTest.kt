package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.data.ProxySettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebFallbackPolicyTest {
    @Test
    fun explicitProxyAlwaysWins() {
        val settings = ProxySettings(enabled = true, host = "proxy.example", port = 8080)

        assertEquals(
            "http://proxy.example:8080",
            webViewProxyUrl(settings, useEmulatorFallback = true)
        )
    }

    @Test
    fun emulatorUsesTheSameHostProxyFallbackAsNativeApi() {
        val settings = ProxySettings(enabled = false)

        assertEquals(
            "http://10.0.2.2:7890",
            webViewProxyUrl(settings, useEmulatorFallback = true)
        )
    }

    @Test
    fun realDevicesKeepFollowingTheSystemNetwork() {
        assertNull(
            webViewProxyUrl(ProxySettings(enabled = false), useEmulatorFallback = false)
        )
    }
}
