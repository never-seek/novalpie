package com.novalpie.nativeapp.feature.download

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.data.*
import kotlinx.coroutines.*
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class DownloadIntegrityDecisionTest {
    @Test fun confirmedMissingMetadataIsDisclosedInDurableCompletedTaskAndRestoredReceipt()=runBlocking {
        exercise(DownloadFailureDecision.PackageAnyway,false)
    }
    @Test fun retryingMetadataRestoresItBeforeAuthorizationAndDoesNotDoubleCharge()=runBlocking {
        exercise(DownloadFailureDecision.RetryWithNewNetwork,true)
    }
    private suspend fun exercise(decision:DownloadFailureDecision,recovers:Boolean) {
        val app=ApplicationProvider.getApplicationContext<Application>()
        MockWebServer().use {s->s.start();val authorize=AtomicInteger();val meta=AtomicInteger();val published=AtomicInteger()
            s.dispatcher=object:Dispatcher(){override fun dispatch(r:RecordedRequest)=when(r.requestUrl!!.encodedPath){
                "/api/novels/2/detail"->if(meta.incrementAndGet()>1&&recovers)MockResponse().setBody("""{"id":2,"title":"QA","author":"Author"}""") else MockResponse().setResponseCode(503)
                "/api/downloads"->{authorize.incrementAndGet();MockResponse().setBody("""{"success":true,"file_name":"source.txt"}""")}
                "/api/downloads/source.txt"->MockResponse().setBody("第1章 开始\nQA body")
                else->MockResponse().setResponseCode(404)
            }}
            val root=File(app.cacheDir,"qa-integrity-"+UUID.randomUUID()).apply{mkdirs()}
            val store=DownloadTaskStore(root);val control=NativeDownloadControl();var prompts=0
            val task=DownloadTask("qa-"+UUID.randomUUID(),1,2,"QA",DownloadFormat.Epub)
            val runner=NativeDownloadTaskRunner(app,NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/'))){_,_,_->published.incrementAndGet();"content://qa/result"}
            val checkpoint=object:DownloadCheckpointCallback {
                override val supportsUserDecisions=true
                override suspend fun onCheckpoint(task:DownloadTask,log:String?,awaitingDecision:Boolean) {
                    store.save(task)
                    if(awaitingDecision) {prompts++;assertTrue(task.integrityDecisionPending);control.resolveDecision(decision)}
                }
            }
            try {
                val complete=withTimeout(15_000){runner.run(task,control,checkpoint)}
                assertEquals("Integrity prompt must be shown",1,prompts)
                assertEquals("Authorize only once",1,authorize.get());assertEquals("Publish only once",1,published.get())
                assertEquals(DownloadPhase.Completed,complete.phase);assertFalse(complete.integrityDecisionPending)
                if(recovers)assertTrue(complete.integrityIssues.isEmpty()) else {
                    assertEquals(DownloadIntegrityCode.MetadataUnavailable,complete.integrityIssues.single().code)
                    assertTrue(complete.acknowledgedIntegrityKeys.contains(complete.integrityIssues.single().key))
                }
                val restored=store.recover(1).tasks.single()
                assertEquals(complete.integrityIssues,restored.integrityIssues)
                val replay=runner.run(task,NativeDownloadControl(),checkpoint)
                assertEquals(complete.integrityIssues,replay.integrityIssues)
                assertEquals(1,authorize.get());assertEquals(1,published.get())
            } finally {root.deleteRecursively()}
        }
    }
    @Test fun unknownImageSourceCannotBecomeASuccessfulEmptyImageQuota()=runBlocking {
        val root=File(ApplicationProvider.getApplicationContext<Application>().cacheDir,"qa-image-source-"+UUID.randomUUID()).apply{mkdirs()}
        val issues=mutableListOf<DownloadIntegrityIssue>()
        try {
            val r=TaskExportImageReconciler(root,onIssue=issues::add){throw IOException("offline")}
            val body="[图片https://example.test/a.png]\n[图片https://example.test/a.png]"
            assertEquals(body,r.reconcile(44,body))
            assertEquals(DownloadIntegrityCode.ImageSourceUnavailable,issues.single().code)
            assertFalse(File(root,"44.json").exists())
            assertTrue(File(root,"44-mismatch.json").exists())
            r.retryUnverified();assertFalse(File(root,"44-mismatch.json").exists())
        }finally {root.deleteRecursively()}
    }
}
