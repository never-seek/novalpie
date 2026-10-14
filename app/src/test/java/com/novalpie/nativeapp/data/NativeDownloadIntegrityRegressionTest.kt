package com.novalpie.nativeapp.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.feature.download.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream
import java.io.ByteArrayOutputStream
import java.io.StringReader

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NativeDownloadIntegrityRegressionTest {
    @Test fun rejectsJsonAsImageEvenWhenServerSaysImage() {
        val f = File.createTempFile("synthetic-json", ".bin")
        try { f.writeText("{\"error\":\"denied\"}")
            assertTrue(runCatching { inspectNativeEpubFileType(f, "application/json") }.isFailure)
            assertTrue(runCatching { inspectNativeEpubFileType(f, "image/png") }.isFailure)
        } finally { f.delete() }
    }
    @Test fun signedResourceQueryNeverAppearsInError() = runBlocking {
        MockWebServer().use { s -> s.start(); s.enqueue(MockResponse().setResponseCode(403))
            val error = runCatching { NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/'))
                .streamAsset(s.url("/a.png?token=SYNTHETIC_SECRET").toString()) { _, _ -> } }.exceptionOrNull()
            assertNotNull(error); assertFalse(error!!.message.orEmpty().contains("SYNTHETIC_SECRET"))
        }
    }
    private suspend fun txt(response: MockResponse, corrupt: Boolean = false): Pair<Result<DownloadTask>, String> {
        MockWebServer().use { s -> s.start(); s.dispatcher = object : Dispatcher() {
            override fun dispatch(r: RecordedRequest) = when (r.requestUrl!!.encodedPath) {
                "/api/novels/2/detail" -> MockResponse().setBody("{\"id\":2,\"title\":\"QA\",\"author\":\"QA\"}")
                "/api/downloads/fixture.txt" -> response
                else -> MockResponse().setResponseCode(404)
            }
        }
            val app=ApplicationProvider.getApplicationContext<Application>(); val id="qa-${UUID.randomUUID()}"
            val work=File(app.noBackupFilesDir,"download-work/$id")
            if(corrupt) { work.mkdirs(); File(work,"source.txt").writeText("第1章\nold"); File(work,"source.complete").writeText("{BROKEN") }
            var published=""
            val runner=NativeDownloadTaskRunner(app,NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/'))) { _, f, _ ->
                published=f.readText(); "content://qa/fixture"
            }
            val task=DownloadTask(id,1,2,"QA",DownloadFormat.Txt,authorizationAttempted=true,authorizationFile="fixture.txt")
            val result=runCatching { runner.run(task,NativeDownloadControl()) {} }
            return result to published
        }
    }
    @Test fun htmlErrorCannotBecomeSuccessfulTxt() = runBlocking {
        val (r,t)=txt(MockResponse().setHeader("content-type","text/html").setBody("<!DOCTYPE html><html>unavailable</html>"))
        assertTrue(r.isFailure); assertEquals("",t)
    }
    @Test fun jsonErrorCannotBecomeSuccessfulTxt() = runBlocking {
        val (r,t)=txt(MockResponse().setHeader("content-type","application/json").setBody("{\"success\":false}"))
        assertTrue(r.isFailure); assertEquals("",t)
    }
    @Test fun ordinaryTxtRetainsUtf8BomAndSourceHtmlAttributes() = runBlocking {
        val source="\uFEFF第1章\n<span title=\"Alice\">Alice</span>"
        val (r,t)=txt(MockResponse().setHeader("content-type","application/octet-stream").setBody(source))
        assertTrue(r.isSuccess); assertEquals(source,t)
    }
    @Test fun malformedSidecarRecoversUsingExistingAuthorization() = runBlocking {
        val (r,t)=txt(MockResponse().setBody("第1章\nRecovered"),corrupt=true)
        assertTrue("Recovery failed: ${r.exceptionOrNull()}",r.isSuccess); assertTrue(t.contains("Recovered"))
    }
    @Test fun interruptedSourceRetriesDoNotLeaveNonresumablePartFiles() = runBlocking {
        MockWebServer().use { s -> s.start(); s.dispatcher=object:Dispatcher() {
            override fun dispatch(r:RecordedRequest)= if(r.requestUrl!!.encodedPath.contains("/downloads/"))
                MockResponse().setBody("x".repeat(8192)).setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY)
            else MockResponse().setBody("{\"id\":2,\"title\":\"QA\"}")
        }
            val app=ApplicationProvider.getApplicationContext<Application>();val id="qa-${UUID.randomUUID()}"
            val runner=NativeDownloadTaskRunner(app,NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/'))) { _,_,_->error("must not publish") }
            val task=DownloadTask(id,1,2,"QA",DownloadFormat.Txt,authorizationAttempted=true,authorizationFile="fixture.txt")
            repeat(2) { assertTrue(runCatching {runner.run(task,NativeDownloadControl()) {}}.isFailure) }
            assertTrue(File(app.noBackupFilesDir,"download-work/$id").listFiles().orEmpty().none {it.name.startsWith("source-")&&it.extension=="part"})
        }
    }
    @Test fun zipLevelZeroDoesNotCompressChapterText() = runBlocking {
        val out=ByteArrayOutputStream()
        NativeEpubArchiveWriter.write(out,NativeEpubMetadata("QA","QA"),StringReader("第1章\n"+"合成正文".repeat(10000)),
            openAsset={error("no images")},zipCompressionLevel=0)
        ZipInputStream(out.toByteArray().inputStream()).use {zip ->
            while(true) {val entry=zip.nextEntry ?: break
                if(entry.name.matches(Regex("OEBPS/chapter-\\d+\\.xhtml"))) {
                    zip.readBytes(); assertTrue(entry.compressedSize>=entry.size); return@runBlocking
                }
            }
        }
        fail("Missing chapter")
    }
}
