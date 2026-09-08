package com.novalpie.nativeapp.feature.download

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.data.NativeDownloadControl
import com.novalpie.nativeapp.data.NovalPieApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

@RunWith(RobolectricTestRunner::class)
class DownloadAssetTypeTest {
    @get:Rule val temp = TemporaryFolder()
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 13, 10, 26, 10) + ByteArray(32)
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    @Test fun oldCachedHtmlIsRefetchedAndCorrectTypeIsStoredWithoutChangingOriginalBytes() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            val uri = server.url("/opaque.file").toString(); val key = sha(uri.toByteArray())
            val root = temp.newFolder()
            val html = "<!DOCTYPE html><html>Temporary error</html>".toByteArray()
            File(root, "$key.bin").writeBytes(html)
            File(root, "$key.json").writeText(JSONObject().put("bytes", html.size).put("mime", "image/png").put("sha256", sha(html)).toString())
            server.enqueue(MockResponse().setHeader("Content-Type", "image/webp").setBody(Buffer().write(png)))
            val runner = NativeDownloadTaskRunner(ApplicationProvider.getApplicationContext<Context>(), NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/')))
            val locks = ConcurrentHashMap<String, Mutex>()
            repeat(2) {
                val asset = runner.openResource(root, uri, NativeDownloadControl(), locks)
                assertEquals("image/png", asset.mediaType)
                asset.input.use { assertArrayEquals(png, it.readBytes()) }
            }
            assertEquals(1, server.requestCount)
            assertEquals("image/png", JSONObject(File(root, "$key.json").readText()).getString("mime"))
        } finally { server.shutdown() }
    }
    @Test fun newHtmlResponseIsNotCachedAsAValidAssetAndRetryCanRecover() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            val uri = server.url("/cover.file").toString(); val root = temp.newFolder()
            server.enqueue(MockResponse().setHeader("Content-Type", "image/png").setBody("<html><body>Unavailable</body></html>"))
            server.enqueue(MockResponse().setHeader("Content-Type", "application/octet-stream").setBody(Buffer().write(png)))
            val runner = NativeDownloadTaskRunner(ApplicationProvider.getApplicationContext<Context>(), NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/')))
            val locks = ConcurrentHashMap<String, Mutex>()
            assertTrue(runCatching { runner.openResource(root, uri, NativeDownloadControl(), locks) }.isFailure)
            assertTrue(root.listFiles().orEmpty().isEmpty())
            val asset = runner.openResource(root, uri, NativeDownloadControl(), locks)
            assertEquals("image/png", asset.mediaType); asset.input.close()
            assertEquals(2, server.requestCount)
        } finally { server.shutdown() }
    }
}
