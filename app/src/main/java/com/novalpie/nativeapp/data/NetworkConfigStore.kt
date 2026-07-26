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

    fun toProxyRoutes(preferEmulatorProxy: Boolean = false): List<Proxy> {
        val routes = mutableListOf<Proxy>()
        val seen = mutableSetOf<String>()
        toJavaProxy()?.let { proxy ->
            routes.add(proxy)
            proxy.proxyKey()?.let(seen::add)
        }

        for (fallbackHost in defaultProxyHosts(preferEmulatorProxy)) {
            val key = proxyKey(fallbackHost, DEFAULT_PROXY_PORT)
            if (seen.add(key)) {
                routes.add(Proxy(Proxy.Type.HTTP, InetSocketAddress(fallbackHost, DEFAULT_PROXY_PORT)))
            }
        }
        routes.add(Proxy.NO_PROXY)
        return routes
    }

    fun toProxySelector(preferEmulatorProxy: Boolean = false): ProxySelector {
        return FixedProxySelector(toProxyRoutes(preferEmulatorProxy))
    }

    companion object {
        const val DEFAULT_PROXY_ENABLED = false
        const val DEFAULT_PROXY_HOST = "10.0.2.2"
        const val DEFAULT_PROXY_PORT = 7890
        val DEFAULT_EMULATOR_PROXY_HOSTS = listOf("10.0.2.2", "127.0.0.1")

        private fun defaultProxyHosts(preferEmulatorProxy: Boolean): List<String> =
            if (preferEmulatorProxy) DEFAULT_EMULATOR_PROXY_HOSTS.asReversed() else DEFAULT_EMULATOR_PROXY_HOSTS

        private fun proxyKey(host: String, port: Int): String = "${host.lowercase()}:$port"

        private fun Proxy.proxyKey(): String? {
            val address = address() as? InetSocketAddress ?: return null
            return proxyKey(address.hostString, address.port)
        }
    }
}

internal fun shouldPreferEmulatorProxy(
    supportedAbis: Array<String>? = runCatching { Build.SUPPORTED_ABIS }.getOrNull()
): Boolean = supportedAbis.orEmpty().any { abi ->
    abi.equals("x86", ignoreCase = true) || abi.equals("x86_64", ignoreCase = true)
}

internal class FixedProxySelector(private val routes: List<Proxy>) : ProxySelector() {
    override fun select(uri: URI?): List<Proxy> {
        return routes.ifEmpty { listOf(Proxy.NO_PROXY) }
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) = Unit
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
