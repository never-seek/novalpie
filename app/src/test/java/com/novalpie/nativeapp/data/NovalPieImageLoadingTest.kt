package com.novalpie.nativeapp.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.InetSocketAddress
import java.net.URI
import java.net.Proxy
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class NovalPieImageLoadingTest {
    @Test
    fun imageLoaderRegistersAnAnimatedImageDecoderForGifAndAnimatedWebp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imageLoader = buildNovalPieImageLoader(
            context = context,
            proxySettings = ProxySettings(enabled = false),
        )

        try {
            val componentRegistry = imageLoader.javaClass
                .getMethod("getComponents")
                .invoke(imageLoader)
            val decoderFactories = componentRegistry.javaClass
                .getMethod("getDecoderFactories")
                .invoke(componentRegistry) as List<*>

            assertTrue(
                "Animated GIF/WebP support must be registered with the shared Coil loader.",
                decoderFactories.any { factory ->
                    factory?.javaClass?.name.orEmpty().contains("ImageDecoderDecoder") ||
                        factory?.javaClass?.name.orEmpty().contains("GifDecoder")
                }
            )
            val decoderNames = decoderFactories.map { it?.javaClass?.name.orEmpty() }
            val gifIndex = decoderNames.indexOfFirst { it.contains("GifDecoder") }
            val platformIndex = decoderNames.indexOfFirst { it.contains("ImageDecoderDecoder") }
            assertTrue(
                "GIFs should use the lower-memory MovieDrawable before the platform animated decoder.",
                gifIndex >= 0 && (platformIndex < 0 || gifIndex < platformIndex),
            )
        } finally {
            imageLoader.shutdown()
        }
    }

    @Test
    fun imageLoaderLimitsBitmapFactoryParallelDecodingForCoverBursts() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imageLoader = buildNovalPieImageLoader(
            context = context,
            proxySettings = ProxySettings(enabled = false),
        )

        try {
            val options = imageLoader.javaClass.getMethod("getOptions").invoke(imageLoader)
            val parallelism = options.javaClass
                .getMethod("getBitmapFactoryMaxParallelism")
                .invoke(options) as Int
            assertEquals(
                2,
                parallelism,
            )
        } finally {
            imageLoader.shutdown()
        }
    }

    @Test
    fun visibleCoverWorkDoesNotUseTheSpeculativeFetchDispatcher() {
        assertTrue(!NovelCoverLoadPriority.Visible.usesBackgroundDispatcher())
        assertTrue(NovelCoverLoadPriority.Speculative.usesBackgroundDispatcher())
    }

    @Test
    fun smallForumCoverUsesAUsefulDisplayBoundInsteadOfTheFullGridDecode() {
        assertEquals(
            NovelCoverRequestSize(widthPx = 96, heightPx = 144),
            novalPieBookCoverTargetSize(widthPx = 30, heightPx = 45),
        )
    }

    @Test
    fun staticThumbnailRequestFreezesAnimatedAssetsAndUsesAnIsolatedCacheKey() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val method = Class.forName("com.novalpie.nativeapp.data.NovalPieImageLoadingKt")
            .declaredMethods
            .firstOrNull { it.name == "novalPieStaticImageRequest" }
        assertNotNull(
            "Forum thumbnails need a dedicated static-image request builder.",
            method,
        )

        val request = method!!.apply { isAccessible = true }.invoke(
            null,
            context,
            "https://novalpie.cc/uploads/shop_assets/frames/animated.webp",
            96,
            144,
        ) as ImageRequest

        assertTrue(request.decoderFactory is BitmapFactoryDecoder.Factory)
        assertTrue(request.memoryCacheKey?.key?.startsWith("novalpie-static-image:") == true)
        assertTrue(request.diskCacheKey?.startsWith("novalpie-static-image:") == true)
    }

    @Test
    fun unspecifiedCoverSizeKeepsTheSharedHighResolutionDefault() {
        assertEquals(
            NovelCoverRequestSize(
                widthPx = NOVALPIE_BOOK_COVER_WIDTH_PX,
                heightPx = NOVALPIE_BOOK_COVER_HEIGHT_PX,
            ),
            novalPieBookCoverTargetSize(widthPx = null, heightPx = null),
        )
    }

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

    /**
     * Replaces `imageHttpClientUsesFallbackProxySelectorWhenProxyDisabled`, whose name described
     * the defect: with the proxy disabled it asserted that image requests were still routed at
     * `10.0.2.2:7890` and `127.0.0.1:7890` first. On a real phone neither address resolves, so
     * every cover image stalled on a dead proxy for the connect timeout before falling through to
     * the network. That is why covers were slow or blank on real hardware.
     */
    @Test
    fun imageHttpClientGoesStraightToTheNetworkOnRealDevicesWhenProxyDisabled() {
        val client = novalPieImageOkHttpClient(
            ProxySettings(enabled = false, host = "127.0.0.1", port = 7890),
            emulatorRuntime = false,
        )

        val proxies = client.proxySelector.select(URI("https://novalpie.cc/cover.jpg"))

        assertEquals(listOf(Proxy.NO_PROXY), proxies)
    }

    /** Emulator QA still needs the host-proxy fallbacks, so they survive there. */
    @Test
    fun imageHttpClientKeepsFallbackProxiesOnEmulators() {
        val client = novalPieImageOkHttpClient(
            ProxySettings(enabled = false, host = "127.0.0.1", port = 7890),
            emulatorRuntime = true,
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
