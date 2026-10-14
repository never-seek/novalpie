package com.novalpie.nativeapp.feature.download
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.data.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class DownloadIntegrityDisclosureRegressionTest {
 @Test fun failedMetadataDiscoveryCannotPublishWithoutAUserDecisionChannel()=runBlocking {
  MockWebServer().use{s->s.start();s.dispatcher=object:Dispatcher(){override fun dispatch(r:RecordedRequest)=when(r.requestUrl!!.encodedPath){
   "/api/novels/2/detail","/api/novels/2/photo"->MockResponse().setResponseCode(503)
   "/api/downloads/source.txt"->MockResponse().setBody("第1章\nBody")
   else->MockResponse().setResponseCode(404)
  }}
   val published=AtomicInteger();val runner=NativeDownloadTaskRunner(ApplicationProvider.getApplicationContext<Application>(),NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/'))){_,_,_->published.incrementAndGet();"content://qa/result"}
   val result=runCatching{runner.run(DownloadTask("qa-"+UUID.randomUUID(),1,2,"QA",DownloadFormat.Epub,authorizationAttempted=true,authorizationFile="source.txt"),NativeDownloadControl()){}}
   assertTrue("Missing metadata must not become unqualified published success",result.isFailure);assertEquals(0,published.get())
  }
 }
 @Test fun completedTxtDialogMustNotClaimItWasAnEpub() {
  val state=com.novalpie.nativeapp.ui.NativeEpubDownloadState(format=com.novalpie.nativeapp.ui.NativeBookDownloadFormat.Txt,completedUri="content://qa/result")
  // Current UI uses a hardcoded EPUB status; drive the status contract after extraction.
  val source=java.io.File("src/main/java/com/novalpie/nativeapp/feature/download/NativeDownloadProgressDialog.kt").readText()
  assertFalse(source.contains("downloadState.completedUri != null -> \"EPUB 生成完成！\""))
 }
}
