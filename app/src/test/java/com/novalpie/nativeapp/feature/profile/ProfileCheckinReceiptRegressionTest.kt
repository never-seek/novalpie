package com.novalpie.nativeapp.feature.profile

import com.novalpie.nativeapp.feature.profile.*
import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileCheckinReceiptRegressionTest {
    private open class FakeRepository : ProfileRepository {
        var calls = 0
        override suspend fun profile() = UserProfile(1, "synthetic", autoCheckin = true)
        override suspend fun checkinStats() = UserCheckinStats()
        override suspend fun checkinRecords(year: Int) = emptyList<UserCheckinRecord>()
        override suspend fun activities(userId: Long, hideSpoilers: Boolean) = UserContentActivityFeed()
        override suspend fun books() = emptyList<NovelCard>()
        override suspend fun inventory() = UserInventory()
        override suspend fun shop() = emptyList<ShopItem>()
        override suspend fun reward() = UserQuizRewardStatus()
        override suspend fun save(profile: UserProfile) = profile
        override suspend fun checkin(): UserCheckinAction { calls++; return UserCheckinAction(false, "今日已签到") }
        override suspend fun verifyAdult(year: Int) = UserCheckinAction(true)
        override suspend fun equip(itemId: Long, equip: Boolean) = UserCheckinAction(true)
        override suspend fun purchase(itemId: Long) = ShopPurchaseResult()
    }
    @Test fun enabledAutoCheckinPostsWhenSuccessfulReadReturnsNoTodayRecord() = runTest {
        val repo = FakeRepository()
        val vm = ProfileViewModel(repo, scope = backgroundScope)
        vm.load(UserProfile(1, "synthetic"), true); runCurrent()
        assertEquals(1, repo.calls)
        assertEquals("今日已签到", vm.state.actionMessage)
        vm.close()
    }
    @Test fun disabledAutoCheckinDoesNotPostWhenRecordsReadFails() = runTest {
        val repo = object : FakeRepository() {
            override suspend fun profile() = UserProfile(1, "synthetic", autoCheckin = false)
            override suspend fun checkinRecords(year: Int): List<UserCheckinRecord> = error("synthetic GET offline")
        }
        val vm = ProfileViewModel(repo, scope = backgroundScope)
        vm.load(UserProfile(1, "synthetic"), true); runCurrent()
        assertEquals(0, repo.calls)
        vm.close()
    }
    @Test fun todayRecordSuppressesAutoCheckin() = runTest {
        val today = String.format(Locale.US, "%tF", Calendar.getInstance())
        val repo = object : FakeRepository() { override suspend fun checkinRecords(year: Int) = listOf(UserCheckinRecord(today, 10)) }
        val vm = ProfileViewModel(repo, scope = backgroundScope)
        vm.load(UserProfile(1, "synthetic"), true); runCurrent()
        assertEquals(0, repo.calls)
        vm.close()
    }
    @Test fun alreadySignedAcknowledgementShouldSuppressRepeatedPostOnRefreshWhenGetFails() = runTest {
        val repo = object : FakeRepository() {
            override suspend fun checkinRecords(year: Int): List<UserCheckinRecord> = error("synthetic GET offline")
        }
        val vm = ProfileViewModel(repo, scope = backgroundScope)
        vm.load(UserProfile(1, "synthetic"), true); runCurrent()
        assertEquals(1, repo.calls)
        println("message after failed record refresh=${vm.state.actionMessage}")
        vm.load(UserProfile(1, "synthetic"), true); runCurrent()
        println("checkin reconfirm: failed GET, already-signed response, POST calls after two loads=${repo.calls}")
        vm.close()
        assertEquals("Already-signed same-day result should prevent repeated writes on refresh", 1, repo.calls)
    }

    @Test fun acknowledgedSameDayCheckinSurvivesFeatureRecreationAndNextDayCanCheckAgain()=runTest {
        val receipts=MemoryCheckinReceiptStore();var now=1791129600000L // 2026-10-05 UTC midnight
        val repo=FakeRepository()
        val first=ProfileViewModel(repo,scope=backgroundScope,checkinReceipts=receipts,nowMillis={now})
        first.load(UserProfile(1,"synthetic"),true);runCurrent();assertEquals(1,repo.calls)
        first.close()
        val second=ProfileViewModel(repo,scope=backgroundScope,checkinReceipts=receipts,nowMillis={now})
        second.load(UserProfile(1,"synthetic"),true);runCurrent();assertEquals(1,repo.calls)
        now+=24L*60*60*1000;second.load(UserProfile(1,"synthetic"),true);runCurrent();assertEquals(2,repo.calls)
    }
    @Test fun oneAccountConfirmedReceiptCannotSuppressAnotherAccount()=runTest {
        val receipt=MemoryCheckinReceiptStore();val now=1791129600000L;receipt.put(1,checkinSiteDay(now),CheckinReceipt.Confirmed)
        val repo=object:FakeRepository(){override suspend fun profile()=UserProfile(2,"other",autoCheckin=true)}
        val vm=ProfileViewModel(repo,scope=backgroundScope,checkinReceipts=receipt,nowMillis={now})
        vm.load(UserProfile(2,"other"),true);runCurrent();assertEquals(1,repo.calls)
    }
    @Test fun unknownWriteIsNeverAutomaticallyReplayedByProfileRefresh()=runTest {
        val repo=object:FakeRepository(){override suspend fun checkin():UserCheckinAction{calls++;throw java.io.IOException("timeout; result unknown")}}
        val vm=ProfileViewModel(repo,scope=backgroundScope)
        vm.load(UserProfile(1,"QA"),true);runCurrent();vm.load(UserProfile(1,"QA"),true);runCurrent()
        assertEquals(1,repo.calls);assertTrue(vm.state.actionMessage!!.contains("未自动重发"))
    }
    @Test fun explicitRejectedWriteDoesNotBecomeARepeatedAutomaticPost()=runTest {
        val repo=object:FakeRepository(){override suspend fun checkin():UserCheckinAction{calls++;return UserCheckinAction(false,"permission rejected")}}
        val vm=ProfileViewModel(repo,scope=backgroundScope)
        vm.load(UserProfile(1,"QA"),true);runCurrent();vm.load(UserProfile(1,"QA"),true);runCurrent();assertEquals(1,repo.calls)
        vm.checkin();runCurrent();assertEquals(2,repo.calls) // A deliberate manual retry remains available.
    }
    @Test fun anotherAlreadyErrorIsNotMistakenForASignedReceipt() {
        assertFalse(checkinAlreadyCompletedMessage("already rate limited"));assertFalse(checkinAlreadyCompletedMessage("本月已签到3天"))
        assertTrue(checkinAlreadyCompletedMessage("今日已签到"));assertTrue(checkinAlreadyCompletedMessage("Already checked in today"))
    }

    @Test fun partialSaveShowsConfirmedBasicProfileButKeepsUnconfirmedSettingsAndDraft()=runTest {
        val repo=object:FakeRepository(){
            override suspend fun profile()=UserProfile(1,"old",autoCheckin=false,showCheckin=true)
            override suspend fun save(profile:UserProfile):UserProfile=throw ProfileSavePartialFailure(profile.copy(showCheckin=null,autoCheckin=null),"资料已保存，但签到设置未保存")
        }
        val vm=ProfileViewModel(repo,scope=backgroundScope)
        vm.load(UserProfile(1,"old"),true);runCurrent();vm.edit{it.copy(nameDraft="new",bioDraft="edited bio",autoCheckin=true,showCheckin=false)}
        vm.save();runCurrent()
        assertFalse(vm.state.saving);assertEquals("new",(vm.state.profile as LoadResult.Success).value.name)
        assertFalse((vm.state.profile as LoadResult.Success).value.autoCheckin!!)
        assertTrue(vm.state.autoCheckin);assertFalse(vm.state.showCheckin);assertEquals("edited bio",vm.state.bioDraft)
        assertTrue(vm.state.actionMessage!!.contains("签到设置未保存"))
        vm.load(UserProfile(1,"new"),true);runCurrent();assertTrue(vm.state.autoCheckin);assertEquals("new",vm.state.nameDraft)
    }
    @Test fun settingsEditedWhilePartialSaveIsPendingMustNotBeErasedByItsAck()=runTest {
        val ack=CompletableDeferred<UserProfile>()
        val repo=object:FakeRepository(){override suspend fun profile()=UserProfile(1,"old",autoCheckin=false)
            override suspend fun save(profile:UserProfile)=ack.await()}
        val vm=ProfileViewModel(repo,scope=backgroundScope);vm.load(UserProfile(1,"old"),true);runCurrent()
        vm.edit{it.copy(nameDraft="submitted",autoCheckin=true)};vm.save();runCurrent();vm.edit{it.copy(nameDraft="new draft",autoCheckin=false)}
        ack.completeExceptionally(ProfileSavePartialFailure(UserProfile(1,"submitted"),"资料已保存，但签到设置未保存"));runCurrent()
        assertEquals("new draft",vm.state.nameDraft);assertFalse(vm.state.autoCheckin);assertFalse(vm.state.saving)
    }
    @Test fun failedReceiptPersistencePreventsTheAutoWriteAndDoesNotCrash()=runTest {
        val repo=FakeRepository();val broken=object:CheckinReceiptStore {
            override fun get(accountId:Long,date:String):CheckinReceipt?=null
            override fun put(accountId:Long,date:String,value:CheckinReceipt){error("synthetic write failure")}
            override fun clear(accountId:Long,date:String)=Unit
        }
        val vm=ProfileViewModel(repo,scope=backgroundScope,checkinReceipts=broken);vm.load(UserProfile(1,"QA"),true);runCurrent()
        assertEquals(0,repo.calls);assertFalse(vm.state.checkingIn)
    }
}
