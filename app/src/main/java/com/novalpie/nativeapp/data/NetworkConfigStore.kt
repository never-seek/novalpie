package com.novalpie.nativeapp.data

import android.annotation.SuppressLint
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
    fun toProxyRoutes(
        emulatorRuntime: Boolean = false,
        emulatorProxyHosts: List<String> = preferredEmulatorProxyHosts(),
    ): List<Proxy> {
        val routes = mutableListOf<Proxy>()
        val seen = mutableSetOf<String>()
        toJavaProxy()?.let { proxy ->
            routes.add(proxy)
            proxy.proxyKey()?.let(seen::add)
        }

        if (emulatorRuntime) {
            // 127.0.0.1 first: the documented QA path uses `adb reverse tcp:7890 tcp:7890`.
            // 10.0.2.2 second, for a plain AVD without adb reverse.
            for (fallbackHost in emulatorProxyHosts) {
                if (fallbackHost.isBlank()) continue
                val key = proxyKey(fallbackHost, DEFAULT_PROXY_PORT)
                if (seen.add(key)) {
                    routes.add(Proxy(Proxy.Type.HTTP, InetSocketAddress(fallbackHost, DEFAULT_PROXY_PORT)))
                }
            }
        }
        routes.add(Proxy.NO_PROXY)
        return routes
    }

    fun toProxySelector(
        emulatorRuntime: Boolean = false,
        emulatorProxyHosts: List<String> = preferredEmulatorProxyHosts(),
    ): ProxySelector {
        return FixedProxySelector(toProxyRoutes(emulatorRuntime, emulatorProxyHosts))
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
 * MuMu's VirtualBox runtime reaches the host through adb reverse, not the AVD-only 10.0.2.2
 * gateway. Keeping that dead gateway out of the route list avoids a full connect-timeout penalty
 * whenever the optional host proxy is not running, while ordinary AVDs retain both fallbacks.
 */
internal fun preferredEmulatorProxyHosts(
    hypervisorPlatform: String? = readAndroidSystemProperty("ro.build.hv.platform"),
): List<String> = if (hypervisorPlatform.orEmpty().contains("vbox", ignoreCase = true)) {
    listOf("127.0.0.1")
} else {
    ProxySettings.DEFAULT_EMULATOR_PROXY_HOSTS
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
    hypervisorPlatform: String? = readAndroidSystemProperty("ro.build.hv.platform"),
    kernelQemu: String? = readAndroidSystemProperty("ro.kernel.qemu"),
    bootQemu: String? = readAndroidSystemProperty("ro.boot.qemu"),
    qemuAvdName: String? = readAndroidSystemProperty("ro.boot.qemu.avd_name"),
    qemudService: String? = readAndroidSystemProperty("init.svc.qemud"),
    supportedAbis: Array<String>? = runCatching { Build.SUPPORTED_ABIS }.getOrNull(),
): Boolean {
    val fp = fingerprint.orEmpty().lowercase()
    val md = model.orEmpty().lowercase()
    val mf = manufacturer.orEmpty().lowercase()
    val br = brand.orEmpty().lowercase()
    val dv = device.orEmpty().lowercase()
    val pr = product.orEmpty().lowercase()
    val hw = hardware.orEmpty().lowercase()
    val hv = hypervisorPlatform.orEmpty().lowercase()
    val qemuKernel = kernelQemu.orEmpty().lowercase()
    val qemuBoot = bootQemu.orEmpty().lowercase()
    val avdName = qemuAvdName.orEmpty().lowercase()
    val qemud = qemudService.orEmpty().lowercase()

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
    // MuMu can spoof a complete Redmi/Oppo Build profile, including the model, fingerprint and
    // hardware strings. Its hypervisor property remains vbox, which is a reliable emulator-only
    // signal and lets the documented adb-reverse proxy route work in that configuration.
    if (hv.contains("vbox") || hv.contains("qemu") || hv.contains("ranchu") ||
        hv.contains("goldfish") || hv.contains("ttvm")
    ) {
        return true
    }

    // Android 15 blocks some hidden Build fields on vendor-spoofed emulators. MuMu still exposes
    // one or more of these lower-level QEMU properties, which lets the app select its host proxy
    // before the first source request is sent.
    if (
        qemuKernel.isEnabledEmulatorFlag() ||
        qemuBoot.isEnabledEmulatorFlag() ||
        avdName.isNotBlank() ||
        qemud in setOf("running", "stopped")
    ) {
        return true
    }

    // The MuMu profile used for this project can present itself as an OPPO phone. An actual OPPO
    // handset is ARM, whereas this particular spoof remains x86/x86_64. Keep this deliberately
    // narrow so Chromebooks and real x86 Android devices do not inherit emulator proxy routing.
    val isOppoProfile = md.contains("oppo") || mf.contains("oppo") || br.contains("oppo")
    val hasX86Abi = supportedAbis.orEmpty().any { abi -> abi.contains("x86", ignoreCase = true) }
    if (isOppoProfile && hasX86Abi) return true

    // MuMu Android 15 can also use the complete HUAWEI Nicole/NCO-AL00 identity seen in QA.
    // The physical handset is ARM-only; the precise profile plus an x86 ABI is therefore a
    // narrow emulator marker when Android blocks access to its otherwise reliable vbox property.
    val isHuaweiNicoleProfile =
        md == "nco-al00" &&
            mf == "huawei" &&
            br == "huawei" &&
            dv == "nicole" &&
            pr == "nicole"
    if (isHuaweiNicoleProfile && hasX86Abi) return true

    return false
}

/**
 * Android does not expose the relevant virtualization properties through [Build].
 * `SystemProperties` is hidden from the public SDK, so access it reflectively and degrade to null
 * on devices where the hidden API policy blocks it. The ordinary [Build] markers above remain the
 * primary production path.
 */
@SuppressLint("PrivateApi") // Narrow, guarded emulator detection; ordinary Build markers remain primary.
private fun readAndroidSystemProperty(key: String): String? {
    val javaProperty = runCatching { System.getProperty(key) }
        .getOrNull()
        ?.trim()
        ?.takeIf(String::isNotBlank)
    if (javaProperty != null) return javaProperty

    return runCatching {
        val systemProperties = Class.forName("android.os.SystemProperties")
        val get = systemProperties.getMethod("get", String::class.java, String::class.java)
        (get.invoke(null, key, "") as? String)
            ?.trim()
            ?.takeIf(String::isNotBlank)
    }.getOrNull()
}

private fun String.isEnabledEmulatorFlag(): Boolean =
    this in setOf("1", "true", "yes", "on") ||
        contains("qemu") ||
        contains("vbox") ||
        contains("ranchu") ||
        contains("goldfish") ||
        contains("ttvm")

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
