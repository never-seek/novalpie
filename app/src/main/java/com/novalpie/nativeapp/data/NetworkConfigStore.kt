package com.novalpie.nativeapp.data

import android.content.Context
import android.os.Build
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

data class ProxySettings(
    val enabled: Boolean = DEFAULT_PROXY_ENABLED,
    val host: String = DEFAULT_PROXY_HOST,
    val port: Int = DEFAULT_PROXY_PORT
) {
    fun toJavaProxy(): Proxy? {
        val normalizedHost = host.trim()
        if (!enabled || normalizedHost.isBlank() || port !in 1..65535) return null
        return Proxy(Proxy.Type.HTTP, InetSocketAddress(normalizedHost, port))
    }

    fun summary(): String {
        return if (enabled) {
            "$host:$port + fallback"
        } else {
            "auto: 127.0.0.1/10.0.2.2:$DEFAULT_PROXY_PORT + direct"
        }
    }

    /**
     * Ordered list of routes OkHttp should try.
     *
     * [emulatorRuntime] gates the development proxy fallbacks, and that gating is the fix for a
     * severe real-device defect. The fallbacks used to be appended unconditionally, with
     * [Proxy.NO_PROXY] only at the end, even when the proxy was disabled -- which is the default.
     * So on an ordinary phone every single API request first tried `10.0.2.2:7890`, an address that
     * only resolves inside an emulator. The SYN went nowhere, the request stalled for the full 12s
     * connect timeout, then `127.0.0.1:7890` failed fast, and only then did the real request go
     * out. With a 30s call timeout, two such hops could abort the call outright. That is a plausible
     * explanation for most of the app feeling slow or broken on real hardware.
     *
     * Emulator QA still needs the fallbacks, so they are kept -- just not for real users.
     */
    fun toProxyRoutes(emulatorRuntime: Boolean = false): List<Proxy> {
        val routes = mutableListOf<Proxy>()
        val seen = mutableSetOf<String>()
        toJavaProxy()?.let { proxy ->
            routes.add(proxy)
            proxy.proxyKey()?.let(seen::add)
        }

        if (emulatorRuntime) {
            // 127.0.0.1 first: the documented QA path uses `adb reverse tcp:7890 tcp:7890`.
            // 10.0.2.2 second, for a plain AVD without adb reverse.
            for (fallbackHost in DEFAULT_EMULATOR_PROXY_HOSTS) {
                val key = proxyKey(fallbackHost, DEFAULT_PROXY_PORT)
                if (seen.add(key)) {
                    routes.add(Proxy(Proxy.Type.HTTP, InetSocketAddress(fallbackHost, DEFAULT_PROXY_PORT)))
                }
            }
        }
        routes.add(Proxy.NO_PROXY)
        return routes
    }

    fun toProxySelector(emulatorRuntime: Boolean = false): ProxySelector {
        return FixedProxySelector(toProxyRoutes(emulatorRuntime))
    }

    companion object {
        const val DEFAULT_PROXY_ENABLED = false
        const val DEFAULT_PROXY_HOST = "10.0.2.2"
        const val DEFAULT_PROXY_PORT = 7890
        val DEFAULT_EMULATOR_PROXY_HOSTS = listOf("127.0.0.1", "10.0.2.2")

        private fun proxyKey(host: String, port: Int): String = "${host.lowercase()}:$port"

        private fun Proxy.proxyKey(): String? {
            val address = address() as? InetSocketAddress ?: return null
            return proxyKey(address.hostString, address.port)
        }
    }
}

/**
 * Whether this build is running on an emulator, and therefore whether the development proxy
 * fallbacks should be offered at all.
 *
 * This replaces a check that returned true for any x86/x86_64 ABI. That is not an emulator test:
 * x86_64 Chromebooks, Windows Subsystem for Android and x86 tablets are all real devices, and on
 * every one of them the old check silently routed all traffic at an emulator-only proxy address
 * first. Conversely it missed ARM emulators entirely.
 *
 * Build properties are the standard signal. Several are checked because emulators differ:
 * `goldfish`/`ranchu` are the AOSP emulator kernels, `vbox86` covers Genymotion and the
 * VirtualBox-derived Android-on-x86 products including the MuMu builds used for this project's QA,
 * and the generic fingerprint/model markers catch the rest.
 */
internal fun isEmulatorRuntime(
    fingerprint: String? = runCatching { Build.FINGERPRINT }.getOrNull(),
    model: String? = runCatching { Build.MODEL }.getOrNull(),
    manufacturer: String? = runCatching { Build.MANUFACTURER }.getOrNull(),
    brand: String? = runCatching { Build.BRAND }.getOrNull(),
    device: String? = runCatching { Build.DEVICE }.getOrNull(),
    product: String? = runCatching { Build.PRODUCT }.getOrNull(),
    hardware: String? = runCatching { Build.HARDWARE }.getOrNull(),
): Boolean {
    val fp = fingerprint.orEmpty().lowercase()
    val md = model.orEmpty().lowercase()
    val mf = manufacturer.orEmpty().lowercase()
    val br = brand.orEmpty().lowercase()
    val dv = device.orEmpty().lowercase()
    val pr = product.orEmpty().lowercase()
    val hw = hardware.orEmpty().lowercase()

    if (fp.startsWith("generic") || fp.startsWith("unknown") || fp.contains("emulator")) return true
    if (md.contains("emulator") || md.contains("android sdk built for") || md.contains("mumu")) return true
    if (mf.contains("genymotion") || mf.contains("netease")) return true
    if (br.startsWith("generic") && dv.startsWith("generic")) return true
    if (pr == "google_sdk" || pr == "sdk" || pr.contains("sdk_g") || pr.contains("emulator") ||
        pr.contains("simulator")
    ) {
        return true
    }
    if (hw == "goldfish" || hw == "ranchu" || hw.contains("vbox") || hw.contains("ttvm")) return true
    return false
}

/**
 * A [ProxySelector] returning a fixed route list.
 *
 * The `equals`/`hashCode` overrides are load-bearing rather than tidiness. OkHttp's `Address`
 * includes its `ProxySelector` in equality, and `RealConnectionPool` only reuses a connection when
 * addresses match. Without these, and given that a fresh selector was constructed for every single
 * request, no pooled connection ever matched: every API call paid a full TCP and TLS handshake.
 */
internal class FixedProxySelector(private val routes: List<Proxy>) : ProxySelector() {
    override fun select(uri: URI?): List<Proxy> {
        return routes.ifEmpty { listOf(Proxy.NO_PROXY) }
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) = Unit

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is FixedProxySelector && routes == other.routes
    }

    override fun hashCode(): Int = routes.hashCode()

    override fun toString(): String = "FixedProxySelector(routes=$routes)"
}

class NetworkConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("novalpie_native_network", Context.MODE_PRIVATE)

    fun loadProxySettings(): ProxySettings {
        val userConfigured = prefs.getBoolean(KEY_PROXY_USER_CONFIGURED, false)
        val host = prefs.getString(KEY_PROXY_HOST, ProxySettings.DEFAULT_PROXY_HOST)
            ?: ProxySettings.DEFAULT_PROXY_HOST
        val port = prefs.getInt(KEY_PROXY_PORT, ProxySettings.DEFAULT_PROXY_PORT)
        val savedEnabled = prefs.getBoolean(KEY_PROXY_ENABLED, ProxySettings.DEFAULT_PROXY_ENABLED)
        val enabled = if (
            !userConfigured &&
            savedEnabled &&
            host == ProxySettings.DEFAULT_PROXY_HOST &&
            port == ProxySettings.DEFAULT_PROXY_PORT
        ) {
            false
        } else {
            savedEnabled
        }
        return ProxySettings(
            enabled = enabled,
            host = host,
            port = port
        )
    }

    fun saveProxySettings(settings: ProxySettings) {
        prefs.edit()
            .putBoolean(KEY_PROXY_ENABLED, settings.enabled)
            .putString(KEY_PROXY_HOST, settings.host.trim().ifBlank { ProxySettings.DEFAULT_PROXY_HOST })
            .putInt(KEY_PROXY_PORT, settings.port.coerceIn(1, 65535))
            .putBoolean(KEY_PROXY_USER_CONFIGURED, true)
            .apply()
    }

    companion object {
        private const val KEY_PROXY_ENABLED = "proxy_enabled"
        private const val KEY_PROXY_HOST = "proxy_host"
        private const val KEY_PROXY_PORT = "proxy_port"
        private const val KEY_PROXY_USER_CONFIGURED = "proxy_user_configured"
    }
}
