package com.novalpie.nativeapp.ui

import android.net.Uri
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.feature.download.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

/** Real Android MediaStore transaction crash boundaries. Only synthetic task files are created/removed. */
class DownloadPublicationDeviceTest {
 private class AbruptDeath:Error("synthetic abrupt termination")
 @Test fun mediaStoreReservationSurvivesEveryPublicationCrashBoundary()=runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
  for(stage in listOf(PublicationMilestone.RowInserted,PublicationMilestone.DestinationBound,PublicationMilestone.BytesWritten,
      PublicationMilestone.DestinationCommitted,PublicationMilestone.ReceiptSaved)) {
   val task=DownloadTask("qa-publication-"+UUID.randomUUID(),1,2,"Beta7发布恢复受控测试",DownloadFormat.Txt)
   val work=File(context.noBackupFilesDir,"download-work/${task.id}").apply{mkdirs()}
   val source=File(work,"result.txt").apply{writeText("Beta7 synthetic bytes ${stage.name}. No source novel or paid request.")}
   val expected=source.readText()
   val journal=DownloadPublicationJournal(context,task.id)
   var uri:Uri?=null
   try {
    val first=runCatching{AndroidDownloadPublisher(context,milestone={if(it==stage)throw AbruptDeath()}).publish(task,source){}}
    assertTrue("Crash milestone must actually be reached: $stage",first.exceptionOrNull() is AbruptDeath)
    val record=journal.read(task)!!
    val rowEvidence=syntheticRows(context,task.id)
    val resumed=AndroidDownloadPublisher(context).publish(task,source){}
    uri=Uri.parse(resumed)
    assertEquals("MediaStore, not a fallback, must be exercised; $rowEvidence; context=${context.packageName}/${context.opPackageName}", "media",uri.authority)
    assertTrue("MediaStore must retain the private complete input",source.isFile)
    assertEquals(expected,context.contentResolver.openInputStream(uri)!!.bufferedReader().use{it.readText()})
    assertEquals(resumed,AndroidDownloadPublisher(context).publish(task,source){})
    val count=context.contentResolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,arrayOf(MediaStore.MediaColumns._ID),
       "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",arrayOf(record.name),null)!!.use{it.count}
    assertEquals("Must be one published copy after $stage",1,count)
    assertTrue(journal.read(task)!!.published)
   } finally {
    uri?.let{assertEquals(1,context.contentResolver.delete(it,null,null))}
    cleanupOnlyPendingRowsFromTask(context,task.id)
    source.delete();work.delete();journal.file.delete();File(journal.file.path+".bak").delete()
   }
  }
 }
 @Test fun existingHumanNamedFileIsNotClaimedByANewTask()=runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
  val task=DownloadTask("qa-publication-"+UUID.randomUUID(),1,2,"Beta7 collision test",DownloadFormat.Txt)
  val work=File(context.noBackupFilesDir,"download-work/${task.id}").apply{mkdirs()}
  val source=File(work,"result.txt").apply{writeText("Own bytes")}
  val oldName="Beta7_collision_test_2_${task.createdAt}.txt"
  val userUri=context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,android.content.ContentValues().apply{
   put(MediaStore.MediaColumns.DISPLAY_NAME,oldName);put(MediaStore.MediaColumns.MIME_TYPE,"text/plain")
   put(MediaStore.MediaColumns.RELATIVE_PATH,android.os.Environment.DIRECTORY_DOWNLOADS)})!!
  context.contentResolver.openOutputStream(userUri)!!.use{it.write("Synthetic unrelated bytes".toByteArray())}
  var ownUri:Uri?=null
  try {
   ownUri=Uri.parse(AndroidDownloadPublisher(context).publish(task,source){})
   assertNotEquals(userUri,ownUri)
   assertEquals("Synthetic unrelated bytes",context.contentResolver.openInputStream(userUri)!!.reader().use{it.readText()})
  } finally {
   ownUri?.let{context.contentResolver.delete(it,null,null)};context.contentResolver.delete(userUri,null,null)
   source.delete();work.delete();val j=DownloadPublicationJournal(context,task.id);j.file.delete();File(j.file.path+".bak").delete()
  }
 }

 @Test fun removeOnlyTheKnownArtifactFromTheFailedFirstQaAttempt() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
  // Exact task IDs from this round's captured synthetic-pending-row-diagnostic.txt, never a user file pattern.
  listOf("6b4a51a5-aa4f-42e4-95b0-6bf5606803e6","9bada430-9ed1-4f87-8cc6-358dfccaaf92",
      "2bad28b9-ad08-4f8a-ad0c-8fcda0d2c016","dbe68a7d-7e8c-4cc9-9596-b9d78635987f",
      "1b944003-9555-4dcb-be57-8a8f2dace4ae","010a395c-b514-4396-807d-c0007aa9b9a1")
      .forEach {cleanupOnlyPendingRowsFromTask(context,"qa-publication-$it")}
 }
 private fun cleanupOnlyPendingRowsFromTask(context:android.content.Context,taskId:String) {
  require(taskId.matches(Regex("qa-publication-[a-f0-9-]{36}")))
  @Suppress("DEPRECATION") val rows=MediaStore.setIncludePending(MediaStore.Downloads.EXTERNAL_CONTENT_URI)
  val ids=mutableListOf<Long>()
  context.contentResolver.query(rows,arrayOf(MediaStore.MediaColumns._ID,MediaStore.MediaColumns.DISPLAY_NAME,MediaStore.MediaColumns.OWNER_PACKAGE_NAME),
      "${MediaStore.MediaColumns.OWNER_PACKAGE_NAME} = ? AND ${MediaStore.MediaColumns.IS_PENDING} = 1 AND (${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? OR ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? OR ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?)",
      arrayOf(context.packageName,".novalpie-$taskId-%","_.novalpie-$taskId-%","novalpie-$taskId-%"),null)?.use {c->while(c.moveToNext()) {
   check(listOf(".novalpie-$taskId-","_.novalpie-$taskId-","novalpie-$taskId-").any(c.getString(1)::startsWith)&&c.getString(2)==context.packageName)
   ids+=c.getLong(0)
  }}
  ids.forEach {id->context.contentResolver.delete(android.content.ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI,id),null,null)}
 }

 private fun syntheticRows(context:android.content.Context,taskId:String):String {
  @Suppress("DEPRECATION") val rows=MediaStore.setIncludePending(MediaStore.Downloads.EXTERNAL_CONTENT_URI)
  val values=mutableListOf<String>()
  context.contentResolver.query(rows,arrayOf(MediaStore.MediaColumns._ID,MediaStore.MediaColumns.DISPLAY_NAME,MediaStore.MediaColumns.OWNER_PACKAGE_NAME,MediaStore.MediaColumns.IS_PENDING),
     "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",arrayOf("%$taskId%"),null)?.use {c->while(c.moveToNext())values+="id=${c.getLong(0)},name=${c.getString(1)},owner=${c.getString(2)},pending=${c.getInt(3)}"}
  return values.toString()
 }
 @Test fun lostSystemCommitAcknowledgementCannotTriggerALocalDuplicate()=runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
  val task=DownloadTask("qa-publication-"+UUID.randomUUID(),1,2,"Beta7commit验证",DownloadFormat.Txt)
  val work=File(context.noBackupFilesDir,"download-work/${task.id}").apply{mkdirs()}
  val source=File(work,"result.txt").apply{writeText("Synthetic commit bytes")};val expected=source.readText()
  val journal=DownloadPublicationJournal(context,task.id);var uri:Uri?=null
  try {
   val first=runCatching{AndroidDownloadPublisher(context,milestone={if(it==PublicationMilestone.DestinationCommitted)throw java.io.IOException("lost commit ack")}).publish(task,source){}}
   uri=first.getOrNull()?.let(Uri::parse)
   assertTrue("A committed row must not be duplicated into local storage",first.isFailure)
   val record=journal.read(task)!!;assertEquals(PublicationKind.MediaStore,record.kind)
   uri=Uri.parse(AndroidDownloadPublisher(context).publish(task,source){})
   assertEquals("media",uri.authority);assertEquals(expected,context.contentResolver.openInputStream(uri)!!.reader().use{it.readText()})
  } finally {
   uri?.let{context.contentResolver.delete(it,null,null)}
   // Includes this task's committed name if the tested crash happens before the caller got its URI.
   journal.read(task)?.uri?.let{u->context.contentResolver.delete(Uri.parse(u),null,null)}
   cleanupOnlyPendingRowsFromTask(context,task.id);source.delete();work.delete();journal.file.delete();File(journal.file.path+".bak").delete()
  }
 }
}
