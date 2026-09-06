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
                if(fail)error("磁盘保存失败") else "content://downloads/test"
            }
            assertTrue(runCatching{runner.run(last,NativeDownloadControl()){last=it}}.isFailure)
            assertEquals("test.txt",last.authorizationFile)
            fail=false
            val completed=runner.run(last.copy(phase=DownloadPhase.NeedsRetry),NativeDownloadControl()){last=it}
            assertEquals(DownloadPhase.Completed,completed.phase)
            assertEquals(1,payments.get())
            assertEquals(1,sources.get())
        } finally {server.shutdown()}
    }
}
