package com.novalpie.nativeapp.feature.download

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.data.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class PublicationReviewRegressionTest {
    @Test fun validLargeIntegrityReceiptMustRemainReadableAfterWrite() {
        val app=ApplicationProvider.getApplicationContext<Application>()
        val id="qa-review-"+UUID.randomUUID()
        val journal=DownloadPublicationJournal(app,id,"runner")
        val issues=(1..400).map { DownloadIntegrityIssue(DownloadIntegrityCode.ImageSourceUnavailable,it,"a".repeat(64)) }
        val data=JSONObject().put("integrityIssues",DownloadIntegrityJson.encode(issues))
            .put("integrityAccepted",JSONArray(issues.map{it.key}))
        assertTrue(data.toString().toByteArray().size>65536)
        try {
            journal.saveJson(data)
            val reloaded=journal.readJson()!!
            assertEquals(issues,DownloadIntegrityJson.decode(reloaded.getJSONArray("integrityIssues")))
            assertEquals(400,reloaded.getJSONArray("integrityAccepted").length())
        }finally {journal.file.delete();File(journal.file.path+".bak").delete()}
    }

    @Test fun recoveringMetadataDoesNotClearWarningOnUnchangedReusedArchive()=runBlocking {
        val app=ApplicationProvider.getApplicationContext<Application>()
        val ready=AtomicBoolean(false);val authorize=AtomicInteger()
        MockWebServer().use{s->s.start();s.dispatcher=object:Dispatcher(){override fun dispatch(r:RecordedRequest)=when(r.requestUrl!!.encodedPath){
            "/api/novels/2/detail"->if(ready.get())MockResponse().setBody("""{"id":2,"title":"Recovered title","author":"Recovered author"}""") else MockResponse().setResponseCode(503)
            "/api/downloads"->{authorize.incrementAndGet();MockResponse().setBody("""{"success":true,"file_name":"source.txt"}""")}
            "/api/downloads/source.txt"->MockResponse().setBody("第1章 开始\nBody")
            else->MockResponse().setResponseCode(404)
        }}
        val control=NativeDownloadControl()
        val task=DownloadTask("qa-review-"+UUID.randomUUID(),1,2,"QA",DownloadFormat.Epub)
        var last=task;var attempts=0;var firstHash=""
        val runner=NativeDownloadTaskRunner(app,NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/'))){_,file,_->
            val hash=java.security.MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString(""){"%02x".format(it)}
            if(++attempts==1){firstHash=hash;throw IOException("save failed")}
            assertEquals("Retained byte-identical package must retain its integrity warning",firstHash,hash)
            "content://qa/review-result"
        }
        val checkpoint=object:DownloadCheckpointCallback {
            override val supportsUserDecisions=true
            override suspend fun onCheckpoint(task:DownloadTask,log:String?,awaitingDecision:Boolean) {
                last=task
                if(awaitingDecision)control.resolveDecision(DownloadFailureDecision.PackageAnyway)
            }
        }
        val work=File(app.noBackupFilesDir,"download-work/${task.id}")
        val journal=DownloadPublicationJournal(app,task.id,"runner")
        try {
            assertTrue(runCatching{runner.run(task,control,checkpoint)}.isFailure)
            assertTrue(last.integrityIssues.any{it.code==DownloadIntegrityCode.MetadataUnavailable})
            ready.set(true)
            val result=runner.run(last.copy(phase=DownloadPhase.NeedsRetry),NativeDownloadControl(),checkpoint)
            assertEquals(1,authorize.get())
            assertTrue("Source metadata recovery did not repair the already-built bytes",result.integrityIssues.any{it.code==DownloadIntegrityCode.MetadataUnavailable})
            assertEquals(DownloadPhase.Completed,result.phase)
        }finally {work.deleteRecursively();journal.file.delete();File(journal.file.path+".bak").delete()}
        }
    }
}
