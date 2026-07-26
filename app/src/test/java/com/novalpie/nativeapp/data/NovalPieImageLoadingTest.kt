package com.novalpie.nativeapp.data

import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.net.URI
import java.net.Proxy
import java.util.concurrent.TimeUnit

class NovalPieImageLoadingTest {
    @Test
    fun imageHttpClientUsesProxySettingsAsFirstSelectorRoute() {
        val client = novalPieImageOkHttpClient(
            ProxySettings(enabled = true, host = "127.0.0.1", port = 7890)
        )

        val proxies = client.proxySelector.select(URI("https://images.novelpia.com/cover.jpg"))
        val address = proxies.first().address() as InetSocketAddress

        assertEquals(Proxy.Type.HTTP, proxies.first().type())
        assertEquals("127.0.0.1", address.hostString)
        assertEquals(7890, address.port)
    }

    @Test
    fun imageHttpClientSkipsProxyWhenDisabled() {
        val client = novalPieImageOkHttpClient(
            ProxySettings(enabled = false, host = "127.0.0.1", port = 7890)
        )

        assertNull(client.proxy)
    }

    @Test
    fun imageHttpClientUsesFallbackProxySelectorWhenProxyDisabled() {
        val client = novalPieImageOkHttpClient(
            ProxySettings(enabled = false, host = "127.0.0.1", port = 7890)
        )

        val proxies = client.proxySelector.select(URI("https://novalpie.cc/cover.jpg"))
        val fallbackAddresses = proxies.mapNotNull { it.address() as? InetSocketAddress }

        assertTrue(proxies.contains(Proxy.NO_PROXY))
        assertTrue(fallbackAddresses.any { it.hostString == "10.0.2.2" && it.port == 7890 })
        assertTrue(fallbackAddresses.any { it.hostString == "127.0.0.1" && it.port == 7890 })
    }

    @Test
    fun imageHttpClientAllowsSlowRemoteImageHosts() {
        val client = novalPieImageOkHttpClient(
            ProxySettings(enabled = false, host = "127.0.0.1", port = 7890)
        )

        assertTrue(client.readTimeoutMillis >= 30_000)
        assertTrue(client.callTimeoutMillis >= 60_000)
    }

    @Test
    fun imageHttpClientSendsWebsiteHeaders() {
        val client = novalPieImageOkHttpClient(ProxySettings(enabled = false))
        val chain = RecordingChain(Request.Builder().url("http://images.novelpia.test/cover.jpg").build())

        client.interceptors.single().intercept(chain).close()

        val request = chain.proceededRequest ?: throw AssertionError("image request did not proceed")
        assertEquals("https://novalpie.cc/", request.header("referer"))
        assertTrue(request.header("user-agent").orEmpty().contains("NovalPieNative"))
    }

    private class RecordingChain(private val inputRequest: Request) : Interceptor.Chain {
        var proceededRequest: Request? = null

        override fun request(): Request = inputRequest

        override fun proceed(request: Request): Response {
            proceededRequest = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("ok".toResponseBody("text/plain".toMediaType()))
                .build()
        }

        override fun connection(): Connection? = null
        override fun call(): Call = throw UnsupportedOperationException("call is not needed for header inspection")
        override fun connectTimeoutMillis(): Int = 0
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun readTimeoutMillis(): Int = 0
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 0
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }
}
