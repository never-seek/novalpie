package com.novalpie.nativeapp.data
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.model.ReaderContent
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.UUID
@RunWith(RobolectricTestRunner::class)
class ReaderCacheRevocationIoRegressionTest {
 private val app=ApplicationProvider.getApplicationContext<Application>()
 @Test fun unreadableDenialMarkerCannotEscapeAndRemainsDeniedAcrossStoreInstances(){
  val scope="qa-"+UUID.randomUUID();val store=ReaderChapterCacheStore(app){scope}
  assertTrue(store.save(21,31,"india",true,null,ReaderContent("A","OWNED_BODY","test")))
  val dir=File(app.filesDir,"novalpie_reader_chapter_cache/accounts/$scope")
  val entry=dir.listFiles()!!.single{it.extension=="json"};val marker=File(entry.path+".denied");assertTrue(marker.mkdir())
  try {
   assertTrue("I/O must not escape revoke",runCatching{store.revoke(21,31,"india",true)}.isSuccess)
   assertNull(store.load(21,31,"india",true));assertNull(ReaderChapterCacheStore(app){scope}.load(21,31,"india",true))
   assertTrue("Recovery bytes must not be deleted",entry.isFile)
  } finally {marker.delete();store.clearBook(21)}
 }
 @Test fun deniedRegistrySurvivesFailedMarkerAndParentRestorationWithoutDeletingChapter(){
  val scope="qa-"+UUID.randomUUID();val store=ReaderChapterCacheStore(app){scope}
  assertTrue(store.save(22,32,"india",true,null,ReaderContent("A","OLD_BYTES","test")))
  val dir=File(app.filesDir,"novalpie_reader_chapter_cache/accounts/$scope");val parked=File(dir.parentFile,scope+"-parked")
  assertTrue(dir.renameTo(parked));dir.writeText("Synthetic blocked directory")
  try {assertTrue("Marker failure must not throw",runCatching{store.revoke(22,32,"india",true)}.isSuccess)}
  finally {assertTrue(dir.delete());assertTrue(parked.renameTo(dir))}
  try {
   assertNull("Persistent denial must survive re-opening the normal cache parent",ReaderChapterCacheStore(app){scope}.load(22,32,"india",true))
   assertTrue(dir.listFiles()!!.any{it.extension=="json"&&it.readText().contains("OLD_BYTES")})
   assertTrue(store.save(22,32,"india",true,null,ReaderContent("A","AUTHORIZED_NEW","test")))
   assertEquals("AUTHORIZED_NEW",ReaderChapterCacheStore(app){scope}.load(22,32,"india",true)?.content?.content)
  } finally {store.clearBook(22)}
 }

 @Test fun corruptPersistentDenialIndexFailsClosedWithoutLosingRecoveryBytes(){
  val scope="qa-"+UUID.randomUUID();val store=ReaderChapterCacheStore(app){scope}
  assertTrue(store.save(23,33,"india",true,null,ReaderContent("A","OLD","test")))
  val key="revoked:$scope:23-33-india-images.json"
  app.getSharedPreferences("novalpie_reader_cache_ownership",0).edit().putString(key,"corrupt bool").commit()
  try {
   val result=runCatching{store.load(23,33,"india",true)};assertTrue(result.isSuccess);assertNull(result.getOrThrow())
   assertTrue(store.save(23,33,"india",true,null,ReaderContent("A","NEW","test")))
   assertEquals("NEW",store.load(23,33,"india",true)?.content?.content)
  }finally{store.clearBook(23)}
 }
}
