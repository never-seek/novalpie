package com.novalpie.nativeapp.feature.download

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.data.*
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DownloadCompressionSettingsTest {
    @Test fun independentNativeTasksUseSelectedSettingsForTheirActualPublishedEpub() = runBlocking {
        val server = MockWebServer(); server.start()
        val cover = "GIF89a".toByteArray() + ByteArray(256 * 1024) { (it % 8).toByte() }
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.requestUrl?.encodedPath) {
                "/api/novels/2/detail" -> MockResponse().setBody("""{"id":2,"title":"压缩测试","author":"测试","full_cover_url":"/cover.gif"}""")
                "/cover.gif" -> MockResponse().setHeader("Content-Type","image/gif").setBody(Buffer().write(cover))
                "/api/downloads" -> MockResponse().setBody("""{"success":true,"file_name":"test.txt"}""")
                "/api/downloads/test.txt" -> MockResponse().setBody("第1章 验证\n" + "原生下载压缩正文。".repeat(2000))
                else -> MockResponse().setResponseCode(404)
            }
        }
        val app = ApplicationProvider.getApplicationContext<Application>()
        val store = DownloadSettingsStore(app)
        val original = store.load()
        val outputs = mutableListOf<File>()
        try {
            for (level in listOf(0,9)) {
                store.save(original.copy(zipCompressionLevel = level, compressImages = false))
                val file = File.createTempFile("native-published-$level-", ".epub").also(outputs::add)
                val runner = NativeDownloadTaskRunner(app, NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/'))) { _, source, _ ->
                    source.copyTo(file, overwrite = true)
                    "content://fixture/compression-$level"
                }
                val result = runner.run(DownloadTask(UUID.randomUUID().toString(),1,2,"压缩测试",DownloadFormat.Epub), NativeDownloadControl()) {}
                assertEquals(DownloadPhase.Completed, result.phase)
                ZipFile(file).use { zip ->
                    val image = zip.getEntry("OEBPS/images/cover.gif")
                    assertEquals(if (level == 0) ZipEntry.STORED else ZipEntry.DEFLATED, image.method)
                    assertArrayEquals(cover, zip.getInputStream(image).use { it.readBytes() })
                    assertEquals(ZipEntry.STORED,zip.getEntry("mimetype").method)
                }
            }
            assertTrue(outputs[1].length() < outputs[0].length() / 3)
            println("NATIVE_TASK_COMPRESSION level0=${outputs[0].length()} level9=${outputs[1].length()}")
        } finally {
            store.save(original); outputs.forEach(File::delete); server.shutdown()
        }
    }
}
