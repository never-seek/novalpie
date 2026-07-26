package com.novalpie.nativeapp.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.InetSocketAddress
import java.net.Proxy

@RunWith(RobolectricTestRunner::class)
class NetworkConfigStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("novalpie_native_network", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun defaultProxySettingsFollowDeviceNetwork() {
        val settings = NetworkConfigStore(context).loadProxySettings()

        assertFalse(settings.enabled)
        assertNull(settings.toJavaProxy())
    }

    @Test
    fun defaultProxyRoutesPreferHostProxyBeforeDirectNetwork() {
        val settings = NetworkConfigStore(context).loadProxySettings()

        val routes = settings.toProxyRoutes()
        val firstAddress = routes[0].address() as InetSocketAddress
        val secondAddress = routes[1].address() as InetSocketAddress

        assertTrue(routes.first().type() == Proxy.Type.HTTP)
        assertTrue(firstAddress.hostString == "10.0.2.2")
        assertTrue(firstAddress.port == 7890)
        assertTrue(secondAddress.hostString == "127.0.0.1")
        assertTrue(secondAddress.port == 7890)
        assertTrue(routes.last() == Proxy.NO_PROXY)
    }

    @Test
    fun emulatorPreferredRoutesTryHostProxyBeforeDirectNetwork() {
        val settings = NetworkConfigStore(context).loadProxySettings()

        val routes = settings.toProxyRoutes(preferEmulatorProxy = true)
        val firstAddress = routes.first().address() as InetSocketAddress
        val secondAddress = routes[1].address() as InetSocketAddress

        assertTrue(routes.first().type() == Proxy.Type.HTTP)
        assertTrue(firstAddress.hostString == "127.0.0.1")
        assertTrue(firstAddress.port == 7890)
        assertTrue(secondAddress.hostString == "10.0.2.2")
        assertTrue(secondAddress.port == 7890)
        assertTrue(routes.last() == Proxy.NO_PROXY)
    }

    @Test
    fun explicitProxyStillKeepsAutomaticFallbackRoutes() {
        val settings = ProxySettings(enabled = true, host = "192.168.31.10", port = 7890)

        val routes = settings.toProxyRoutes(preferEmulatorProxy = true)
        val addresses = routes.mapNotNull { it.address() as? InetSocketAddress }

        assertEquals("192.168.31.10", addresses[0].hostString)
        assertEquals("127.0.0.1", addresses[1].hostString)
        assertEquals("10.0.2.2", addresses[2].hostString)
        assertTrue(routes.last() == Proxy.NO_PROXY)
    }

    @Test
    fun x86RuntimePrefersEmulatorProxyButArmRuntimeDoesNot() {
        assertTrue(shouldPreferEmulatorProxy(arrayOf("x86_64", "arm64-v8a")))
        assertFalse(shouldPreferEmulatorProxy(arrayOf("arm64-v8a", "armeabi-v7a")))
    }
}
