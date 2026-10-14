package com.novalpie.nativeapp.feature.profile

import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.model.*

internal class ProfileSavePartialFailure(val confirmedProfile:UserProfile,message:String,cause:Throwable?=null):java.io.IOException(message,cause)

internal interface ProfileRepository {
    suspend fun profile(): UserProfile
    suspend fun checkinStats(): UserCheckinStats
    suspend fun checkinRecords(year: Int): List<UserCheckinRecord>
    suspend fun activities(userId: Long, hideSpoilers: Boolean): UserContentActivityFeed
    suspend fun activityPage(userId: Long, page: Int, hideSpoilers: Boolean): UserContentActivityFeed = activities(userId, hideSpoilers)
    suspend fun books(): List<NovelCard>
    suspend fun inventory(): UserInventory
    suspend fun shop(): List<ShopItem>
    suspend fun reward(): UserQuizRewardStatus
    suspend fun save(profile: UserProfile): UserProfile
    suspend fun checkin(): UserCheckinAction
    suspend fun checkinSettings(): UserCheckinSettings = UserCheckinSettings()
    suspend fun verifyAdult(year: Int): UserCheckinAction
    suspend fun equip(itemId: Long, equip: Boolean): UserCheckinAction
    suspend fun purchase(itemId: Long): ShopPurchaseResult
}

internal class WebsiteProfileRepository(private val api: NovalPieApi) : ProfileRepository {
    override suspend fun profile() = api.currentUser()
    override suspend fun checkinStats() = api.currentUserCheckinStats()
    override suspend fun checkinRecords(year: Int) = api.userCheckinRecords(startDate = "$year-01-01", endDate = "$year-12-31")
    override suspend fun activities(userId: Long, hideSpoilers: Boolean) = api.userContentActivityFeed(userId, limit = 200, hideSpoilers = hideSpoilers)
    override suspend fun activityPage(userId: Long, page: Int, hideSpoilers: Boolean) = api.userContentActivityFeed(userId, page, 200, hideSpoilers)
    override suspend fun books() = api.currentUserUploadedBooks()
    override suspend fun inventory() = api.currentUserInventory()
    override suspend fun shop() = api.shopItems()
    override suspend fun reward() = api.currentUserQuizRewardStatus()
    override suspend fun save(profile: UserProfile): UserProfile {
        val updated = api.updateCurrentUser(profile)
        if (profile.showCheckin != null || profile.autoCheckin != null) {
            try {
                val action=api.updateCurrentUserCheckinSettings(showCheckin=profile.showCheckin,autoCheckin=profile.autoCheckin)
                if(!action.success)throw ProfileSavePartialFailure(updated.copy(showCheckin=null,autoCheckin=null),
                    "资料已保存，但签到设置未保存：${action.message ?: "服务器拒绝"}。签到草稿保留，请核对后重试。")
            }catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled}
            catch(partial:ProfileSavePartialFailure){throw partial}
            catch(error:Exception){throw ProfileSavePartialFailure(updated.copy(showCheckin=null,autoCheckin=null),
                "资料已保存，但签到设置结果未确认；草稿保留，请刷新核对，不要重复提交其他资料。",error)}
        }
        return updated
    }
    override suspend fun checkin() = api.checkinCurrentUser()
    override suspend fun checkinSettings() = api.userCheckinSettings()
    override suspend fun verifyAdult(year: Int) = api.verifyCurrentUserAdult(year)
    override suspend fun equip(itemId: Long, equip: Boolean) = api.setCurrentUserEquipment(itemId, if (equip) "equip" else "unequip")
    override suspend fun purchase(itemId: Long) = api.purchaseShopItem(itemId)
}
