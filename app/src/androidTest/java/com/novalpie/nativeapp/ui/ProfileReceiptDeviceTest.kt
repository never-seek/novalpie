package com.novalpie.nativeapp.ui
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.feature.profile.*
import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/** Synthetic repositories, real Android preferences/main lifecycle. No real checkin or settings writes. */
class ProfileReceiptDeviceTest {
 private open class Repo:ProfileRepository {
  val calls=AtomicInteger()
  override suspend fun profile()=UserProfile(999999990,"Synthetic profile",autoCheckin=true,showCheckin=true)
  override suspend fun checkinStats()=UserCheckinStats()
  override suspend fun checkinRecords(year:Int):List<UserCheckinRecord> = throw java.io.IOException("synthetic GET failure")
  override suspend fun activities(userId:Long,hideSpoilers:Boolean)=UserContentActivityFeed()
  override suspend fun books()=emptyList<NovelCard>();override suspend fun inventory()=UserInventory()
  override suspend fun shop()=emptyList<ShopItem>();override suspend fun reward()=UserQuizRewardStatus()
  override suspend fun save(profile:UserProfile)=profile
  override suspend fun checkin():UserCheckinAction{calls.incrementAndGet();return UserCheckinAction(false,"今日已签到")}
  override suspend fun verifyAdult(year:Int)=UserCheckinAction(true)
  override suspend fun equip(itemId:Long,equip:Boolean)=UserCheckinAction(true)
  override suspend fun purchase(itemId:Long)=ShopPurchaseResult(true)
 }
 @Test fun actualPersistentReceiptSuppressesRefreshAndRecreatedFeaturePostsForThatAccountDay()=runBlocking {
  val app=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
  val now=4099680000000L;val date=checkinSiteDay(now);val store=StoredCheckinReceiptStore(app);val repo=Repo()
  var first:ProfileViewModel?=null;var second:ProfileViewModel?=null
  try {
   store.clear(999999990,date)
   withContext(Dispatchers.Main){first=ProfileViewModel(repo,checkinReceipts=store,nowMillis={now});first!!.load(UserProfile(999999990,"QA"),true)}
   withTimeout(10000){while(withContext(Dispatchers.Main){first!!.state.checkingIn||repo.calls.get()!=1})delay(30)}
   withContext(Dispatchers.Main){first!!.load(UserProfile(999999990,"QA"),true)};delay(300)
   assertEquals(1,repo.calls.get());assertEquals(CheckinReceipt.Confirmed,store.get(999999990,date))
   withContext(Dispatchers.Main){first!!.close();second=ProfileViewModel(repo,checkinReceipts=StoredCheckinReceiptStore(app),nowMillis={now});second!!.load(UserProfile(999999990,"QA"),true)}
   delay(300);assertEquals(1,repo.calls.get())
  }finally{withContext(Dispatchers.Main){first?.close();second?.close()};store.clear(999999990,date)}
 }
 @Test fun partialAcknowledgementKeepsSettingsDraftAndReportsTheSavedPart()=runBlocking {
  val repo=object:Repo(){override suspend fun profile()=UserProfile(999999990,"old",autoCheckin=false,showCheckin=true)
   override suspend fun save(profile:UserProfile):UserProfile=throw ProfileSavePartialFailure(profile.copy(autoCheckin=null,showCheckin=null),"资料已保存，但签到设置未保存")}
  lateinit var vm:ProfileViewModel
  try {
   withContext(Dispatchers.Main){vm=ProfileViewModel(repo);vm.load(UserProfile(999999990,"QA"),true)}
   withTimeout(10000){while(withContext(Dispatchers.Main){vm.state.profile !is LoadResult.Success})delay(30)}
   withContext(Dispatchers.Main){vm.edit{it.copy(nameDraft="edited",autoCheckin=true,showCheckin=false)};vm.save()}
   withTimeout(10000){while(withContext(Dispatchers.Main){vm.state.saving})delay(30)}
   withContext(Dispatchers.Main){assertEquals("edited",vm.state.nameDraft);assertTrue(vm.state.autoCheckin);assertFalse(vm.state.showCheckin)
    assertTrue(vm.state.actionMessage!!.contains("签到设置未保存"));assertEquals("edited",(vm.state.profile as LoadResult.Success).value.name)}
  }finally{withContext(Dispatchers.Main){runCatching{vm.close()}}}
 }
}
