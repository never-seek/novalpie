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

    /**
     * The important case, and the one that was broken. On a real device with the proxy off -- the
     * default -- there must be no proxy route at all. Previously `10.0.2.2:7890` and
     * `127.0.0.1:7890` were prepended unconditionally, so every request stalled on an
     * emulator-only address for the full connect timeout before falling through to the network.
     */
    @Test
    fun realDeviceWithProxyDisabledGoesStraightToTheNetwork() {
        val settings = NetworkConfigStore(context).loadProxySettings()

        val routes = settings.toProxyRoutes(emulatorRuntime = false)

        assertEquals(listOf(Proxy.NO_PROXY), routes)
    }

    @Test
    fun emulatorRuntimeTriesHostProxiesBeforeDirectNetwork() {
        val settings = NetworkConfigStore(context).loadProxySettings()

        val routes = settings.toProxyRoutes(emulatorRuntime = true)
        val firstAddress = routes.first().address() as InetSocketAddress
        val secondAddress = routes[1].address() as InetSocketAddress

        assertTrue(routes.first().type() == Proxy.Type.HTTP)
        // adb reverse is the documented QA path, so the loopback route is tried first.
        assertEquals("127.0.0.1", firstAddress.hostString)
        assertEquals(7890, firstAddress.port)
        assertEquals("10.0.2.2", secondAddress.hostString)
        assertEquals(7890, secondAddress.port)
        assertTrue(routes.last() == Proxy.NO_PROXY)
    }

    @Test
    fun vboxEmulatorFallbackUsesOnlyTheReverseLoopbackProxy() {
        val settings = NetworkConfigStore(context).loadProxySettings()

        val routes = settings.toProxyRoutes(
            emulatorRuntime = true,
            emulatorProxyHosts = preferredEmulatorProxyHosts(hypervisorPlatform = "vbox"),
        )
        val addresses = routes.mapNotNull { it.address() as? InetSocketAddress }

        assertEquals(listOf("127.0.0.1"), addresses.map(InetSocketAddress::getHostString))
        assertEquals(listOf(7890), addresses.map(InetSocketAddress::getPort))
        assertEquals(Proxy.NO_PROXY, routes.last())
    }

    /** An explicitly configured proxy is honoured on a real device, and is tried first. */
    @Test
    fun explicitProxyIsUsedOnRealDevicesWithoutEmulatorFallbacks() {
        val settings = ProxySettings(enabled = true, host = "192.168.31.10", port = 7890)

        val routes = settings.toProxyRoutes(emulatorRuntime = false)
        val addresses = routes.mapNotNull { it.address() as? InetSocketAddress }

        assertEquals(1, addresses.size)
        assertEquals("192.168.31.10", addresses[0].hostString)
        assertTrue(routes.last() == Proxy.NO_PROXY)
    }

    @Test
    fun explicitProxyStillKeepsEmulatorFallbackRoutesOnEmulators() {
        val settings = ProxySettings(enabled = true, host = "192.168.31.10", port = 7890)

        val routes = settings.toProxyRoutes(emulatorRuntime = true)
        val addresses = routes.mapNotNull { it.address() as? InetSocketAddress }

        assertEquals("192.168.31.10", addresses[0].hostString)
        assertEquals("127.0.0.1", addresses[1].hostString)
        assertEquals("10.0.2.2", addresses[2].hostString)
        assertTrue(routes.last() == Proxy.NO_PROXY)
    }

    /**
     * Replaces a test asserting that any x86 ABI means "emulator". x86_64 Chromebooks, WSA and x86
     * tablets are real devices, and treating them as emulators is what routed real users' traffic
     * at a dead proxy address.
     */
    @Test
    fun emulatorDetectionUsesBuildMarkersNotCpuArchitecture() {
        // AOSP emulator
        assertTrue(
            isEmulatorRuntime(
                fingerprint = "generic/sdk_gphone64_x86_64/emulator:14/UPB5/1234:userdebug/test-keys",
                model = "sdk_gphone64_x86_64",
                hardware = "ranchu",
            )
        )
        // VirtualBox-derived emulators, including the MuMu builds used for QA
        assertTrue(isEmulatorRuntime(fingerprint = "x/y/z", hardware = "vbox86p"))
        assertTrue(isEmulatorRuntime(model = "MuMu"))
        // MuMu Android 15 can spoof a Redmi profile, but exposes its VirtualBox hypervisor.
        assertTrue(
            isEmulatorRuntime(
                fingerprint = "Redmi/vermeer/vermeer:15/V417IR/423:user/release-keys",
                model = "23113RKC6C",
                manufacturer = "Redmi",
                brand = "Redmi",
                device = "vermeer",
                product = "vermeer",
                hardware = "Redmi",
                hypervisorPlatform = "vbox",
            )
        )
        // The current MuMu image can spoof an OPPO profile. Its x86 runtime is still an emulator
        // marker, unlike a real OPPO handset which reports ARM ABIs.
        assertTrue(
            isEmulatorRuntime(
                fingerprint = "OPPO/CPH2531/OP4B00L1:15/AP3A/987:user/release-keys",
                model = "OPPO CPH2531",
                manufacturer = "OPPO",
                brand = "OPPO",
                device = "OP4B00L1",
                product = "OP4B00L1",
                hardware = "qcom",
                supportedAbis = arrayOf("x86_64", "x86"),
            )
        )
        assertTrue(
            isEmulatorRuntime(
                fingerprint = "OPPO/CPH2531/OP4B00L1:15/AP3A/987:user/release-keys",
                model = "OPPO CPH2531",
                manufacturer = "OPPO",
                brand = "OPPO",
                device = "OP4B00L1",
                product = "OP4B00L1",
                hardware = "qcom",
                kernelQemu = "1",
                supportedAbis = arrayOf("arm64-v8a"),
            )
        )
        // MuMu Android 15 can also spoof a HUAWEI Nicole/NCO-AL00 profile. Its mixed x86 ABI
        // list is not possible on that physical ARM handset, and hidden property reflection is
        // blocked on this image, so it needs a narrow Build-based fallback for adb reverse.
        assertTrue(
            isEmulatorRuntime(
                fingerprint = "HUAWEI/Nicole/Nicole:15/V417IR/828:user/release-keys",
                model = "NCO-AL00",
                manufacturer = "HUAWEI",
                brand = "HUAWEI",
                device = "Nicole",
                product = "Nicole",
                hardware = "HUAWEI",
                supportedAbis = arrayOf("x86_64", "arm64-v8a", "x86"),
            )
        )

        // A real x86_64 device must NOT be treated as an emulator.
        assertFalse(
            isEmulatorRuntime(
                fingerprint = "google/nocturne/nocturne:11/x/y:user/release-keys",
                model = "Pixel Slate",
                manufacturer = "Google",
                brand = "google",
                device = "nocturne",
                product = "nocturne",
                hardware = "eve",
            )
        )
        // A real ARM phone.
        assertFalse(
            isEmulatorRuntime(
                fingerprint = "samsung/x1s/x1s:13/TP1A/y:user/release-keys",
                model = "SM-G981B",
                manufacturer = "samsung",
                brand = "samsung",
                device = "x1s",
                product = "x1sxxx",
                hardware = "exynos990",
            )
        )
        assertFalse(
            isEmulatorRuntime(
                fingerprint = "OPPO/CPH2531/OP4B00L1:15/AP3A/987:user/release-keys",
                model = "OPPO CPH2531",
                manufacturer = "OPPO",
                brand = "OPPO",
                device = "OP4B00L1",
                product = "OP4B00L1",
                hardware = "qcom",
                supportedAbis = arrayOf("arm64-v8a", "armeabi-v7a"),
            )
        )
    }

    /**
     * OkHttp includes the ProxySelector in Address equality and only reuses a pooled connection
     * when addresses match. Without these overrides -- and a fresh selector was built per request
     * -- no connection was ever reused, so every API call paid a fresh TCP and TLS handshake.
     */
    @Test
    fun proxySelectorsWithEqualRoutesAreEqualSoConnectionsCanBePooled() {
        val settings = ProxySettings(enabled = true, host = "10.0.0.1", port = 7890)

        val first = settings.toProxySelector(emulatorRuntime = false)
        val second = settings.toProxySelector(emulatorRuntime = false)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())

        val different = settings.toProxySelector(emulatorRuntime = true)
        assertTrue(first != different)
    }
}
