package com.novalpie.nativeapp.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.audit.ReaderTestActivity
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.feature.download.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream

/** Explicit opt-in, real authorized export. Every asset is verified with bounded buffers. */
class NativeLargeDownloadLiveDeviceTest {
    @Test fun largeOriginalEpubPreservesEveryResourceByteAndCanFinishOutsideTheReaderPage()=runBlocking {
        val inst=InstrumentationRegistry.getInstrumentation()
        val args=InstrumentationRegistry.getArguments()
        val bookId=args.getString("bookId")?.toLongOrNull() ?: error("Requires explicit reviewed bookId")
        val minimumBytes=args.getString("minimumBytes")?.toLongOrNull() ?: 1_073_741_824L
        val app=inst.targetContext.applicationContext
        val container=AppContainer.from(app)
        container.refreshEnvironmentFromStores()
        val user=container.api.currentUser()
        val book=container.api.bookDetail(bookId)
        assertTrue("源站未允许下载",book.allowDownload!=false)
        val account=requireNotNull(user.id)
        assertFalse("已有下载任务，不能覆盖",container.downloads.state.value.busy)
        val previous=container.downloadStore.recover(account).tasks.firstOrNull {
            it.id.startsWith("beta7-large-$bookId-")&&it.bookId==bookId&&it.format==DownloadFormat.Epub&&!it.applyReplacement&&it.phase in setOf(DownloadPhase.NeedsRetry,DownloadPhase.Failed,DownloadPhase.Paused)&&it.authorizationFile!=null
        }
        val task=previous?.copy(phase=DownloadPhase.Queued) ?: DownloadTask("beta7-large-$bookId-${UUID.randomUUID()}",account,bookId,book.title,DownloadFormat.Epub,requestedConcurrency=8)
        val evidence=File(app.cacheDir,"beta7-large-download").apply{mkdirs()}
        val expectedDigests=mutableSetOf<String>()
        var sourceImageOccurrences=0
        var sourceCaptured=false
        val monitor=launch(Dispatchers.IO) {
            val saving=container.downloads.state.first {it.task?.id==task.id&&it.task.phase==DownloadPhase.Saving}
            val work=File(app.noBackupFilesDir,"download-work/${task.id}")
            val marker=Regex("[\\[［]\\s*图片\\s*[:：]\\s*(.+?)[\\]］]")
            File(work,"source.txt").bufferedReader().useLines{lines->lines.forEach{line->sourceImageOccurrences+=marker.findAll(line).count{it.groupValues[1].isNotBlank()}}}
            File(work,"assets").listFiles()?.filter{it.extension=="json"}?.forEach{file->
                val data=JSONObject(file.readText());expectedDigests+=data.getString("sha256")
            }
            sourceCaptured=true
        }
        val activity=ActivityScenario.launch(ReaderTestActivity::class.java)
        val start=System.currentTimeMillis()
        var finalUri:android.net.Uri?=null
        val report=JSONObject().put("bookId",bookId).put("taskId",task.id)
        try {
            activity.onActivity{NativeDownloadService.start(it,task)}
            val finished=withTimeout(90*60*1000L){container.downloads.state.first{it.task?.id==task.id&&!it.busy}}
            assertEquals(finished.message ?: finished.task?.failure,DownloadPhase.Completed,finished.task?.phase)
            val completed=finished.task!!
            monitor.join()
            assertTrue(sourceCaptured)
            val uri=android.net.Uri.parse(completed.destinationUri);finalUri=uri
            val totalBytes=app.contentResolver.openFileDescriptor(uri,"r")!!.use{it.statSize}
            assertTrue("没有达到大包门槛：$totalBytes",totalBytes>=minimumBytes)
            var chapters=0;var images=0;var references=0;var cover=false
            val paths=hashSetOf<String>();val resources=JSONArray()
            app.contentResolver.openInputStream(uri)!!.use{input->ZipInputStream(input).use{zip->
                val buffer=ByteArray(64*1024)
                while(true) {
                    val entry=zip.nextEntry ?: break
                    assertTrue("重复ZIP路径",paths.add(entry.name))
                    if(entry.name.startsWith("OEBPS/images/")) {
                        val digest=MessageDigest.getInstance("SHA-256");var bytes=0L
                        while(true){val n=zip.read(buffer);if(n<0)break;digest.update(buffer,0,n);bytes+=n}
                        val hash=digest.digest().joinToString(""){"%02x".format(it)}
                        assertTrue("图片字节与原始下载不符：${entry.name}",hash in expectedDigests)
                        resources.put(JSONObject().put("path",entry.name).put("bytes",bytes).put("sha256",hash))
                        if(entry.name.startsWith("OEBPS/images/cover."))cover=true else images++
                    } else if(entry.name.matches(Regex("OEBPS/chapter-\\d+\\.xhtml"))) {
                        chapters++
                        val reader=zip.reader(Charsets.UTF_8)
                        val chars=CharArray(16*1024);var tail=""
                        while(true){val n=reader.read(chars);if(n<0)break;val chunk=tail+String(chars,0,n)
                            references+=Regex("<img[\\s>]").findAll(chunk).count();tail=chunk.takeLast(4).substringAfterLast('>')}
                    } else while(zip.read(buffer)>=0){}
                    zip.closeEntry()
                }
            }}
            report.put("bytes",totalBytes).put("elapsedMs",System.currentTimeMillis()-start)
                .put("chapters",chapters).put("sourceImageOccurrences",sourceImageOccurrences).put("imageFiles",images)
                .put("bodyImageReferences",references).put("resources",resources)
            assertTrue(cover)
            assertEquals(completed.completedChapters,chapters)
            assertEquals(sourceImageOccurrences,references)
            assertEquals(sourceImageOccurrences,images)
            assertEquals(0,completed.failedAssets)
            report.put("passed",true)
            File(evidence,"book-$bookId.json").writeText(report.toString(2))
        }catch(failure:Throwable) {
            report.put("passed",false).put("error",failure.javaClass.simpleName).put("elapsedMs",System.currentTimeMillis()-start)
            File(evidence,"book-$bookId.json").writeText(report.toString(2));throw failure
        }finally {
            monitor.cancelAndJoin()
            finalUri?.let{assertTrue("只清本测试输出",app.contentResolver.delete(it,null,null)>0)}
            inst.runOnMainSync{container.downloads.cancel()}
            activity.close()
        }
    }
}
