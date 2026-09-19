package com.novalpie.nativeapp.feature.download

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.data.NativeDownloadControl
import com.novalpie.nativeapp.data.NovalPieApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class NativeDownloadTaskRunnerTest {
    @Test fun finalChapterTotalsSurviveThrottledProgressCallbacks()=runBlocking {
        val server=MockWebServer();server.start()
        server.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest):MockResponse=when(request.requestUrl?.encodedPath){
            "/api/novels/2/detail"->MockResponse().setBody("""{"id":2,"title":"测试书","author":"测试"}""")
            "/api/downloads"->MockResponse().setBody("""{"success":true,"file_name":"test.txt"}""")
            "/api/downloads/test.txt"->MockResponse().setBody("第1章 起点\n第一段。\n\n第2章 终点\n第二段。")
            else->MockResponse().setResponseCode(404)
        }}
        try {
            val app=ApplicationProvider.getApplicationContext<Application>()
            val runner=NativeDownloadTaskRunner(app,NovalPieApi(baseUrl=server.url("/").toString().trimEnd('/'))){_,_,_->"content://downloads/complete"}
            val result=runner.run(DownloadTask(UUID.randomUUID().toString(),1,2,"测试书",DownloadFormat.Epub),NativeDownloadControl()){}
            assertEquals(2,result.totalChapters)
            assertEquals(2,result.completedChapters)
        }finally{server.shutdown()}
    }
    @Test fun sameLengthCorruptSourceCheckpointIsRefetchedWithoutASecondAuthorization()=runBlocking {
        val server=MockWebServer();server.start()
        val payments=AtomicInteger();val sources=AtomicInteger()
        val source="第1章 测试\n完整的正文。"
        server.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest):MockResponse=when(request.requestUrl?.encodedPath){
            "/api/novels/2/detail"->MockResponse().setBody("""{"id":2,"title":"测试书","author":"测试"}""")
            "/api/downloads"->{payments.incrementAndGet();MockResponse().setBody("""{"success":true,"file_name":"test.txt"}""")}
            "/api/downloads/test.txt"->{sources.incrementAndGet();MockResponse().setBody(source)}
            else->MockResponse().setResponseCode(404)
        }}
        try {
            val app=ApplicationProvider.getApplicationContext<Application>()
            var last=DownloadTask(UUID.randomUUID().toString(),1,2,"测试书",DownloadFormat.Txt)
            var fail=true
            val runner=NativeDownloadTaskRunner(app,NovalPieApi(baseUrl=server.url("/").toString().trimEnd('/'))){_,file,_->
                if(fail)error("磁盘保存失败")
                assertEquals(source,file.readText())
                "content://downloads/test"
            }
            assertTrue(runCatching{runner.run(last,NativeDownloadControl()){last=it}}.isFailure)
            val cached=java.io.File(app.noBackupFilesDir,"download-work/${last.id}/source.txt")
            val bytes=cached.readBytes();bytes[bytes.lastIndex]='x'.code.toByte();cached.writeBytes(bytes)
            fail=false
            assertEquals(DownloadPhase.Completed,runner.run(last.copy(phase=DownloadPhase.NeedsRetry),NativeDownloadControl()){last=it}.phase)
            assertEquals(1,payments.get())
            assertEquals(2,sources.get())
        } finally {server.shutdown()}
    }
    @Test fun failedFinalSaveRetriesWithTheSameAuthorizationAndCompletedSourceFile()=runBlocking {
        val server=MockWebServer();server.start()
        val payments=AtomicInteger();val sources=AtomicInteger()
        val source="第1章 测试\n完整的正文。\n\n第2章 继续\n第二章。"
        server.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest):MockResponse=when(request.requestUrl?.encodedPath){
            "/api/novels/2/detail"->MockResponse().setBody("""{"id":2,"title":"测试书","author":"测试"}""")
            "/api/downloads"->{payments.incrementAndGet();MockResponse().setBody("""{"success":true,"file_name":"test.txt"}""")}
            "/api/downloads/test.txt"->{sources.incrementAndGet();MockResponse().setBody(source)}
            else->MockResponse().setResponseCode(404)
        }}
        try {
            val app=ApplicationProvider.getApplicationContext<Application>()
            var last=DownloadTask(UUID.randomUUID().toString(),1,2,"测试书",DownloadFormat.Txt)
            var fail=true
            val runner=NativeDownloadTaskRunner(app,NovalPieApi(baseUrl=server.url("/").toString().trimEnd('/'))){_,file,_->
                assertEquals(source,file.readText())
                if(fail)error("磁盘保存失败")
                assertEquals("重试保存不应该重建已完成包",1234000L,file.lastModified())
                "content://downloads/test"
            }
            assertTrue(runCatching{runner.run(last,NativeDownloadControl()){last=it}}.isFailure)
            assertEquals("test.txt",last.authorizationFile)
            val packaged=java.io.File(app.noBackupFilesDir,"download-work/${last.id}/result.txt")
            assertTrue(packaged.setLastModified(1234000))
            fail=false
            val completed=runner.run(last.copy(phase=DownloadPhase.NeedsRetry),NativeDownloadControl()){last=it}
            assertEquals(DownloadPhase.Completed,completed.phase)
            assertEquals(1,payments.get())
            assertEquals(1,sources.get())
        } finally {server.shutdown()}
    }

    @Test
    fun epubDownloadBundlesCoverAndFullMetadataAttributesIntoResultArchive() = runBlocking {
        val server = MockWebServer(); server.start()
        val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0, 16, 0x4A, 0x46, 0x49, 0x46) + ByteArray(32)
        val source = "第1章 旅程\n这是正文文字。"
        val detail = """{
            "id": 2,
            "title": "真实书名",
            "author": "测试大师",
            "platform": "novelPia",
            "status": "已完结",
            "description": "这是正品简介内容。",
            "tags": ["恋爱", "日常"],
            "full_cover_url": "/covers/high-res.jpg"
        }"""
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.requestUrl?.encodedPath) {
                "/api/novels/2/detail" -> MockResponse().setBody(detail)
                "/covers/high-res.jpg" -> MockResponse().setHeader("Content-Type", "image/jpeg").setBody(okio.Buffer().write(jpegBytes))
                "/api/downloads" -> MockResponse().setBody("""{"success":true,"file_name":"test.txt"}""")
                "/api/downloads/test.txt" -> MockResponse().setBody(source)
                else -> MockResponse().setResponseCode(404)
            }
        }
        try {
            val app = ApplicationProvider.getApplicationContext<Application>()
            var publishedBytes: ByteArray? = null
            val runner = NativeDownloadTaskRunner(app, NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/'))) { _, file, _ ->
                publishedBytes = file.readBytes()
                "content://downloads/complete"
            }
            val task = DownloadTask(UUID.randomUUID().toString(), 1, 2, "占位标题", DownloadFormat.Epub)
            val completed = runner.run(task, NativeDownloadControl()) {}
            assertEquals(DownloadPhase.Completed, completed.phase)
            assertNotNull(publishedBytes)

            val entries = linkedMapOf<String, ByteArray>()
            java.util.zip.ZipInputStream(publishedBytes!!.inputStream()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entries[entry.name] = zip.readBytes()
                }
            }

            assertTrue("包含封面图片", entries.containsKey("OEBPS/images/cover.jpg"))
            assertArrayEquals(jpegBytes, entries.getValue("OEBPS/images/cover.jpg"))
            assertFalse("正文不包含intro.xhtml", entries.containsKey("OEBPS/intro.xhtml"))
            val chapter = entries.getValue("OEBPS/chapter-1.xhtml").toString(Charsets.UTF_8)
            assertTrue("第1章包含正文", chapter.contains("这是正文文字。"))

            val opf = entries.getValue("OEBPS/content.opf").toString(Charsets.UTF_8)
            assertTrue("OPF包含书名", opf.contains("<dc:title>真实书名</dc:title>"))
            assertTrue("OPF包含作者", opf.contains("<dc:creator>测试大师</dc:creator>"))
            assertTrue("OPF包含恋爱标签", opf.contains("<dc:subject>恋爱</dc:subject>"))
            assertTrue("OPF包含日常标签", opf.contains("<dc:subject>日常</dc:subject>"))
            assertTrue("OPF包含正品简介", opf.contains("<dc:description>这是正品简介内容。</dc:description>"))
            assertTrue("OPF包含状态", opf.contains("<meta name=\"status\" content=\"已完结\"/>"))
            assertTrue("OPF包含来源", opf.contains("<dc:source>NovelPia</dc:source>"))
            assertTrue("OPF包含封面引用", opf.contains("id=\"cover-image\" href=\"images/cover.jpg\""))
            assertFalse("OPF不包含简介页引用", opf.contains("intro-page"))
        } finally {
            server.shutdown()
        }
    }
}
