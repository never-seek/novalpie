package com.novalpie.nativeapp.feature.download

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.data.*
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DownloadImageSettingsResumeTest {
    private data class Image(val name:String,val bytes:ByteArray,val method:Int)
    @Test fun enablingImageCompressionBeforeSavingRetryMustNotReuseAnUncompressedEpub()=runBlocking {
        retryWith(DownloadSettings(compressImages=false),DownloadSettings(compressImages=true,imageQuality=25)){first,second->
            assertTrue(first.name.endsWith(".png"));assertTrue(second.name.endsWith(".jpg"));assertTrue(second.bytes.size<first.bytes.size/2)
        }
    }
    @Test fun changingImageQualityBeforeSavingRetryMustReencodeTheExistingOriginal()=runBlocking {
        retryWith(DownloadSettings(compressImages=true,imageQuality=90),DownloadSettings(compressImages=true,imageQuality=25)){first,second->
            assertTrue(second.bytes.size<first.bytes.size)
        }
    }
    @Test fun changingZipLevelBeforeSavingRetryMustApplyTheNewContainerSetting()=runBlocking {
        retryWith(DownloadSettings(zipCompressionLevel=0),DownloadSettings(zipCompressionLevel=9)){first,second->
            assertEquals(ZipEntry.STORED,first.method);assertEquals(ZipEntry.DEFLATED,second.method);assertArrayEquals(first.bytes,second.bytes)
        }
    }
    private suspend fun retryWith(first:DownloadSettings,second:DownloadSettings,verify:(Image,Image)->Unit) {
        val server=MockWebServer();server.start()
        val image=opaqueRgbaPng(900,1200);val authorizations=AtomicInteger();val images=AtomicInteger()
        server.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest):MockResponse=when(request.requestUrl?.encodedPath){
            "/api/novels/2/detail"->MockResponse().setBody("""{"id":2,"title":"图片设置回归","author":"合成测试","full_cover_url":"/picture.png"}""")
            "/picture.png"->{images.incrementAndGet();MockResponse().setHeader("Content-Type","image/png").setBody(Buffer().write(image))}
            "/api/downloads"->{authorizations.incrementAndGet();MockResponse().setBody("""{"success":true,"file_name":"test.txt"}""")}
            "/api/downloads/test.txt"->MockResponse().setBody("第1章 设置测试\n只用于验证本机图片设置，不包含网站书籍正文。")
            else->MockResponse().setResponseCode(404)
        }}
        val app=ApplicationProvider.getApplicationContext<Application>();val settings=DownloadSettingsStore(app);val original=settings.load()
        val id=UUID.randomUUID().toString();var task=DownloadTask(id,1,2,"图片设置回归",DownloadFormat.Epub)
        val observed=mutableListOf<Image>()
        val runner=NativeDownloadTaskRunner(app,NovalPieApi(baseUrl=server.url("/").toString().trimEnd('/'))){_,file,_->
            ZipFile(file).use{zip->val entry=zip.entries().asSequence().single{it.name.startsWith("OEBPS/images/")};observed+=Image(entry.name,zip.getInputStream(entry).use{it.readBytes()},entry.method)}
            if(observed.size==1)throw IOException("Synthetic final save failure")
            "content://fixture/verified-$id"
        }
        try{
            settings.save(first)
            assertTrue(runCatching{runner.run(task,NativeDownloadControl()){updated:DownloadTask->task=updated}}.exceptionOrNull() is IOException)
            assertEquals(DownloadPhase.Saving,task.phase)
            settings.save(second)
            val result=runner.run(task,NativeDownloadControl()){updated:DownloadTask->task=updated}
            assertEquals(DownloadPhase.Completed,result.phase);assertEquals(2,observed.size)
            verify(observed[0],observed[1]);assertEquals("Do not deduct/authorize again",1,authorizations.get());assertEquals("Reuse the original asset instead of redownloading",1,images.get())
        }finally{
            settings.save(original);server.shutdown()
            val root=File(app.noBackupFilesDir,"download-work").canonicalFile;val work=File(root,id).canonicalFile
            check(work.parentFile==root);work.deleteRecursively()
            val receipt=DownloadPublicationJournal(app,id,"runner").file
            for(suffix in listOf("",".bak",".new"))File(receipt.path+suffix).delete()
        }
    }
}
