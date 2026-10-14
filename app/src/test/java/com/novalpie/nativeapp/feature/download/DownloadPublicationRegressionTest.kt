package com.novalpie.nativeapp.feature.download
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class DownloadPublicationRegressionTest {
 private val app=ApplicationProvider.getApplicationContext<Application>()
 private fun task()=DownloadTask("qa-"+UUID.randomUUID(),1,2,"Publication QA",DownloadFormat.Txt)
 private fun file(task:DownloadTask)=File(app.noBackupFilesDir,"download-work/${task.id}/result.txt").also{it.parentFile!!.mkdirs();it.writeText("Owned complete bytes")}
 @Test fun pre29RetryAfterPublicationUsesExactlyTheSameFileAndUri()=runBlocking {
  val task=task();val file=file(task);val first=AndroidDownloadPublisher(app).publish(task,file){}
  val again=AndroidDownloadPublisher(app).publish(task,file){}
  assertEquals(first,again);assertEquals("Owned complete bytes",File(java.net.URI(first)).readText())
 }
 @Test fun twoTaskIdsWithTheSameBookTitleAndTimestampNeverCollide()=runBlocking {
  val a=task();val b=a.copy(id="qa-"+UUID.randomUUID())
  val ua=AndroidDownloadPublisher(app).publish(a,file(a)){}
  val ub=AndroidDownloadPublisher(app).publish(b,file(b)){}
  assertNotEquals(ua,ub);assertTrue(File(java.net.URI(ua)).isFile);assertTrue(File(java.net.URI(ub)).isFile)
 }
 @Test fun existingLegacyNamedUserFileIsNeitherClaimedNorOverwritten()=runBlocking {
  val task=task();val old=File(app.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),"Publication_QA_2_${task.createdAt}.txt")
  old.parentFile!!.mkdirs();old.writeText("USER_FILE")
  val uri=AndroidDownloadPublisher(app).publish(task,file(task)){}
  assertNotEquals(old.toURI().toString(),uri);assertEquals("USER_FILE",old.readText())
 }
 @Test fun reusedTaskIdCannotExposeAnotherAccountOrBookPublication()=runBlocking {
  val task=task();val file=file(task);AndroidDownloadPublisher(app).publish(task,file){}
  assertTrue(runCatching{AndroidDownloadPublisher(app).publish(task.copy(accountId=2),file){}}.isFailure)
  assertTrue(runCatching{AndroidDownloadPublisher(app).publish(task.copy(bookId=3),file){}}.isFailure)
 }
 private fun server(s:MockWebServer,requests:AtomicInteger){
  s.dispatcher=object:Dispatcher(){override fun dispatch(r:RecordedRequest):MockResponse {
   requests.incrementAndGet()
   return when(r.requestUrl!!.encodedPath){
    "/api/novels/2/detail"->MockResponse().setBody("{\"id\":2,\"title\":\"Publication QA\"}")
    "/api/downloads/source.txt"->MockResponse().setBody("第1章\n完整正文")
    else->MockResponse().setResponseCode(404)
   }
  }};s.start()
 }
 @Test fun lostCompletedCheckpointCannotCauseADuplicatePublicationOnRetry()=runBlocking {
  MockWebServer().use {s->val reads=AtomicInteger();server(s,reads);val pubs=AtomicInteger();val task=task().copy(authorizationAttempted=true,authorizationFile="source.txt");var durable=task
   val runner=NativeDownloadTaskRunner(app,NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/'))){_,_,_->"content://qa/${pubs.incrementAndGet()}"}
   assertTrue(runCatching{runner.run(task,NativeDownloadControl()){if(it.phase==DownloadPhase.Completed)error("lost final checkpoint")else durable=it}}.isFailure)
   val requests=reads.get();assertEquals(DownloadPhase.Saving,durable.phase)
   val done=runner.run(durable.copy(phase=DownloadPhase.NeedsRetry),NativeDownloadControl()){}
   assertEquals(1,pubs.get());assertEquals(DownloadPhase.Completed,done.phase);assertEquals("content://qa/1",done.destinationUri)
   assertEquals("Already published retry must not query metadata or re-download",requests,reads.get())
  }
 }
 @Test fun repeatedRunnerInvocationAfterCleanupUsesThePublishedReceipt()=runBlocking {
  MockWebServer().use {s->val reads=AtomicInteger();server(s,reads);val pubs=AtomicInteger();val task=task().copy(authorizationAttempted=true,authorizationFile="source.txt")
   val runner=NativeDownloadTaskRunner(app,NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/'))){_,_,_->"content://qa/${pubs.incrementAndGet()}"}
   val first=runner.run(task,NativeDownloadControl()){};assertEquals(DownloadPhase.Completed,first.phase)
   val second=runner.run(task.copy(phase=DownloadPhase.NeedsRetry),NativeDownloadControl()){}
   assertEquals(first.destinationUri,second.destinationUri);assertEquals(1,pubs.get())
  }
 }

 private class AbruptDeath:Error("synthetic process death")
 @Test fun pre29CommitBeforeReceiptCanRecoverWithoutCreatingAnotherFile()=runBlocking {
  val task=task();val source=file(task)
  val publisher=AndroidDownloadPublisher(app,milestone={if(it==PublicationMilestone.DestinationCommitted)throw AbruptDeath()})
  assertTrue(runCatching{publisher.publish(task,source){}}.exceptionOrNull() is AbruptDeath)
  val record=DownloadPublicationJournal(app,task.id).read(task)!!;assertFalse(record.published)
  val uri=AndroidDownloadPublisher(app).publish(task,source){}
  assertEquals(record.uri,uri);assertTrue(DownloadPublicationJournal(app,task.id).read(task)!!.published)
 }
 @Test fun localMoveBeforeReceiptCanRecoverEvenWhenThePrivateResultNoLongerExists() {
  val task=task();val source=file(task)
  val uriFactory:(File)->android.net.Uri={android.net.Uri.parse("content://qa/local/${it.name}")}
  val publisher=AndroidDownloadPublisher(app,milestone={if(it==PublicationMilestone.DestinationCommitted)throw AbruptDeath()},localUri=uriFactory)
  assertTrue(runCatching{publisher.keepCompletedFileLocally(task,source,"${task.id}.txt")}.exceptionOrNull() is AbruptDeath)
  assertFalse(source.exists());val before=DownloadPublicationJournal(app,task.id).read(task)!!
  val uri=AndroidDownloadPublisher(app,localUri=uriFactory).keepCompletedFileLocally(task,source,"${task.id}.txt")
  assertEquals(before.uri,uri);assertEquals("Owned complete bytes",File(app.filesDir,"native-downloads/${task.id}.txt").readText())
 }
 @Test fun editedPublishedDestinationCannotBeOverwrittenOrPublishedAgain()=runBlocking {
  val task=task();val source=file(task);val uri=AndroidDownloadPublisher(app).publish(task,source){}
  val target=File(java.net.URI(uri));target.writeText("USER_EDITED")
  assertTrue(runCatching{AndroidDownloadPublisher(app).publish(task,source){}}.isFailure)
  assertEquals("USER_EDITED",target.readText())
 }
 @Test fun damagedJournalIsNotSilentlyDiscardedToPublishAgain()=runBlocking {
  val task=task();val source=file(task);AndroidDownloadPublisher(app).publish(task,source){}
  val journal=DownloadPublicationJournal(app,task.id);journal.file.writeText("broken JSON")
  assertTrue(runCatching{AndroidDownloadPublisher(app).publish(task,source){}}.isFailure)
  assertEquals("broken JSON",journal.file.readText())
 }
 @Test fun pausedAndCancelledCopyLeavesOnlyOwnedRecoverableIntentAndCanRetry()=runBlocking {
  val task=task();val source=file(task);val before=File(app.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),"unrelated.txt").apply{parentFile!!.mkdirs();writeText("untouched")}
  var checks=0
  val first=runCatching{AndroidDownloadPublisher(app).publish(task,source){if(++checks>=3)throw kotlinx.coroutines.CancellationException("synthetic cancel")}}
  assertTrue(first.exceptionOrNull() is kotlinx.coroutines.CancellationException)
  val uri=AndroidDownloadPublisher(app).publish(task,source){}
  assertEquals("untouched",before.readText());assertEquals("Owned complete bytes",File(java.net.URI(uri)).readText())
 }
 @Test fun corruptRunnerReceiptDoesNotTriggerAnotherSourceOrPublication()=runBlocking {
  MockWebServer().use {s->val reads=AtomicInteger();server(s,reads);val pubs=AtomicInteger();val task=task().copy(authorizationAttempted=true,authorizationFile="source.txt")
   val runner=NativeDownloadTaskRunner(app,NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/'))){_,_,_->"content://qa/${pubs.incrementAndGet()}"}
   runner.run(task,NativeDownloadControl()){}
   val before=reads.get();DownloadPublicationJournal(app,task.id,"runner").file.writeText("broken JSON")
   assertTrue(runCatching{runner.run(task.copy(phase=DownloadPhase.NeedsRetry),NativeDownloadControl()){}}.isFailure)
   assertEquals(1,pubs.get());assertEquals(before,reads.get())
  }
 }
 @Test fun sourceMayBeGoneAfterPublicationButAccountMismatchStillCannotReadReceipt()=runBlocking {
  val task=task();val source=file(task);val uri=AndroidDownloadPublisher(app).publish(task,source){};source.delete()
  assertEquals(uri,AndroidDownloadPublisher(app).recoverPublished(task){})
  assertTrue(runCatching{AndroidDownloadPublisher(app).recoverPublished(task.copy(accountId=2)){}}.isFailure)
 }
 @Test fun longUnicodeTitlesProduceAValidByteBoundedUniqueFilename()=runBlocking {
  val task=task().copy(title="测试书籍名字".repeat(50));val source=file(task)
  val uri=AndroidDownloadPublisher(app).publish(task,source){};val target=File(java.net.URI(uri))
  assertTrue(target.name.toByteArray().size<=240);assertTrue(target.name.endsWith(".txt"));assertEquals(source.readText(),target.readText())
 }

 @Test @Config(sdk=[23]) fun minimumSdkPublicationAndRetryKeepTheSameOwnedFile()=runBlocking {
  val task=task();val source=file(task);val uri=AndroidDownloadPublisher(app).publish(task,source){}
  assertEquals(uri,AndroidDownloadPublisher(app).publish(task,source){})
  assertEquals(source.readText(),File(java.net.URI(uri)).readText())
 }
 @Test fun concurrentPublicationCallsForOneTaskCannotCreateTwoResults()=runBlocking {
  val task=task();val source=file(task)
  val results=kotlinx.coroutines.coroutineScope {
   listOf(async(kotlinx.coroutines.Dispatchers.IO){AndroidDownloadPublisher(app).publish(task,source){}},
      async(kotlinx.coroutines.Dispatchers.IO){AndroidDownloadPublisher(app).publish(task,source){}}).map{it.await()}
  }
  assertEquals(results[0],results[1])
 }
}
