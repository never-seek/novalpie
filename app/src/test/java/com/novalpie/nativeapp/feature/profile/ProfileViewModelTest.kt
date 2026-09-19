package com.novalpie.nativeapp.feature.profile

import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @Test fun failedEquipmentMutationStillAllowsTheOriginalHeroAndInventoryToFinishLoading() = runTest {
        val hero = CompletableDeferred<UserProfile>()
        val inventory = CompletableDeferred<UserInventory>()
        val model = ProfileViewModel(object : Repository() {
            override suspend fun profile() = hero.await()
            override suspend fun inventory() = inventory.await()
            override suspend fun equip(itemId: Long, equip: Boolean) = UserCheckinAction(false, "测试拒绝")
        }, scope = backgroundScope)
        model.load(UserProfile(1, "本人"), true); runCurrent()
        model.equip(UserInventoryItem(3, "装扮")); runCurrent()
        hero.complete(UserProfile(1, "本人")); inventory.complete(UserInventory()); runCurrent()
        assertTrue(model.state.profile is LoadResult.Success)
        assertTrue(model.state.inventory is LoadResult.Success)
    }

    @Test fun acknowledgedPurchaseWithFailedReadbackIsNotReportedAsFailedOrSentTwice() = runTest {
        var bought = false
        var calls = 0
        val model = ProfileViewModel(object : Repository() {
            override suspend fun profile(): UserProfile { if (bought) error("readback offline"); return super.profile() }
            override suspend fun inventory(): UserInventory { if (bought) error("readback offline"); return super.inventory() }
            override suspend fun purchase(itemId: Long): ShopPurchaseResult { calls++; bought = true; return ShopPurchaseResult(true) }
        }, scope = backgroundScope)
        model.load(UserProfile(1, "本人"), true); runCurrent()
        model.purchase(ShopItem(3, "合成装扮")); runCurrent()
        assertEquals(1, calls)
        assertTrue(model.state.actionMessage.orEmpty().contains("操作已提交成功"))
        assertTrue(model.state.profile is LoadResult.Success)
        assertNull(model.state.shopPurchaseItemId)
    }
    private open class Repository : ProfileRepository {
        override suspend fun profile() = UserProfile(1, "原名字")
        override suspend fun checkinStats() = UserCheckinStats()
        override suspend fun checkinRecords(year: Int) = emptyList<UserCheckinRecord>()
        override suspend fun activities(userId: Long, hideSpoilers: Boolean) = UserContentActivityFeed()
        override suspend fun books() = emptyList<NovelCard>()
        override suspend fun inventory() = UserInventory()
        override suspend fun shop() = emptyList<ShopItem>()
        override suspend fun reward() = UserQuizRewardStatus()
        override suspend fun save(profile: UserProfile) = profile
        override suspend fun checkin() = UserCheckinAction(true)
        override suspend fun verifyAdult(year: Int) = UserCheckinAction(true)
        override suspend fun equip(itemId: Long, equip: Boolean) = UserCheckinAction(true)
        override suspend fun purchase(itemId: Long) = ShopPurchaseResult()
    }
    @Test fun slowRefreshCannotEraseAProfileDraftTypedWhileItLoads() = runTest {
        val delayed = CompletableDeferred<UserProfile>()
        val model = ProfileViewModel(object : Repository() { override suspend fun profile() = delayed.await() }, scope = backgroundScope)
        model.load(UserProfile(1, "旧"), true); runCurrent()
        model.edit { it.copy(nameDraft = "正在输入的新名字", bioDraft = "新简介") }
        delayed.complete(UserProfile(1, "原名字")); runCurrent()
        assertEquals("正在输入的新名字", model.state.nameDraft)
        assertEquals("新简介", model.state.bioDraft)
    }
    @Test fun equipmentWriteCannotInvalidateAnotherPanelsInFlightRead() = runTest {
        val books = CompletableDeferred<List<NovelCard>>()
        val model = ProfileViewModel(object : Repository() { override suspend fun books() = books.await() }, scope = backgroundScope)
        model.load(UserProfile(1, "本人"), true); runCurrent()
        model.equip(UserInventoryItem(3, "测试装扮")); runCurrent()
        books.complete(listOf(NovelCard(5, "本人的书"))); runCurrent()
        assertTrue(model.state.books is LoadResult.Success)
        assertEquals(5L, (model.state.books as LoadResult.Success).value.single().id)
    }
    @Test fun accountSwitchCannotReceiveAnOldSaveAcknowledgement() = runTest {
        val save = CompletableDeferred<UserProfile>()
        val model = ProfileViewModel(object : Repository() { override suspend fun save(profile: UserProfile) = withContext(NonCancellable) { save.await() } }, scope = backgroundScope)
        model.load(UserProfile(1, "本人"), true); runCurrent(); model.edit { it.copy(nameDraft = "旧账号新名字") }; model.save(); runCurrent()
        model.environmentChanged()
        save.complete(UserProfile(1, "旧账号新名字")); runCurrent()
        assertTrue(model.state.profile is LoadResult.Idle)
    }

    @Test fun autoCheckinTriggersAutomaticallyWhenConfiguredAndNotCheckedInToday() = runTest {
        var checkinCalled = false
        val model = ProfileViewModel(object : Repository() {
            override suspend fun profile() = UserProfile(1, "本人", autoCheckin = true)
            override suspend fun checkinRecords(year: Int) = emptyList<UserCheckinRecord>()
            override suspend fun checkin(): UserCheckinAction {
                checkinCalled = true
                return UserCheckinAction(true, "自动签到成功", points = 10)
            }
        }, scope = backgroundScope)
        model.load(UserProfile(1, "本人", autoCheckin = true), true); runCurrent()
        assertTrue(checkinCalled)
        assertEquals("自动签到成功", model.state.actionMessage)
    }

    @Test fun autoCheckinDoesNotTriggerIfAlreadyCheckedInToday() = runTest {
        var checkinCalled = false
        val today = String.format(java.util.Locale.US, "%tF", java.util.Calendar.getInstance())
        val model = ProfileViewModel(object : Repository() {
            override suspend fun profile() = UserProfile(1, "本人", autoCheckin = true)
            override suspend fun checkinRecords(year: Int) = listOf(UserCheckinRecord(today, 10))
            override suspend fun checkin(): UserCheckinAction {
                checkinCalled = true
                return UserCheckinAction(true)
            }
        }, scope = backgroundScope)
        model.load(UserProfile(1, "本人", autoCheckin = true), true); runCurrent()
        assertFalse(checkinCalled)
    }

    @Test fun checkinHandledGracefullyWhenAlreadyCheckedInOnServer() = runTest {
        val model = ProfileViewModel(object : Repository() {
            override suspend fun checkin() = UserCheckinAction(false, "今日已签到")
        }, scope = backgroundScope)
        model.checkin()
        runCurrent()
        assertEquals("今日已签到", model.state.actionMessage)
        assertFalse(model.state.checkingIn)
    }

    @Test fun checkinHandledGracefullyWhenExceptionThrownWithAlreadyCheckedIn() = runTest {
        val model = ProfileViewModel(object : Repository() {
            override suspend fun checkin(): UserCheckinAction {
                throw java.io.IOException("NovalPie API 400: /api/users/me/checkins - 今日已签到")
            }
        }, scope = backgroundScope)
        model.checkin()
        runCurrent()
        assertEquals("今日已签到", model.state.actionMessage)
        assertFalse(model.state.checkingIn)
    }

    @Test fun autoCheckinTriggersWhenProfileAutoCheckinNullButCheckinSettingsAutoCheckinTrue() = runTest {
        var checkinCalled = false
        val model = ProfileViewModel(object : Repository() {
            override suspend fun profile() = UserProfile(1, "本人", autoCheckin = null)
            override suspend fun checkinSettings() = UserCheckinSettings(autoCheckin = true)
            override suspend fun checkinRecords(year: Int) = emptyList<UserCheckinRecord>()
            override suspend fun checkin(): UserCheckinAction {
                checkinCalled = true
                return UserCheckinAction(true, "自动签到成功", points = 10)
            }
        }, scope = backgroundScope)
        model.load(UserProfile(1, "本人"), true); runCurrent()
        assertTrue(checkinCalled)
        assertEquals("自动签到成功", model.state.actionMessage)
        assertTrue(model.state.autoCheckin)
    }
}
