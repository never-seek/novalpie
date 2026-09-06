package com.novalpie.nativeapp.feature.profile

import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.model.*

internal interface ProfileRepository {
    suspend fun profile(): UserProfile
    suspend fun checkinStats(): UserCheckinStats
    suspend fun checkinRecords(year: Int): List<UserCheckinRecord>
    suspend fun activities(userId: Long, hideSpoilers: Boolean): UserContentActivityFeed
    suspend fun books(): List<NovelCard>
    suspend fun inventory(): UserInventory
    suspend fun shop(): List<ShopItem>
    suspend fun reward(): UserQuizRewardStatus
    suspend fun save(profile: UserProfile): UserProfile
    suspend fun checkin(): UserCheckinAction
    suspend fun verifyAdult(year: Int): UserCheckinAction
    suspend fun equip(itemId: Long, equip: Boolean): UserCheckinAction
    suspend fun purchase(itemId: Long): ShopPurchaseResult
}

internal class WebsiteProfileRepository(private val api: NovalPieApi) : ProfileRepository {
    override suspend fun profile() = api.currentUser()
    override suspend fun checkinStats() = api.currentUserCheckinStats()
    override suspend fun checkinRecords(year: Int) = api.userCheckinRecords(startDate = "$year-01-01", endDate = "$year-12-31")
    override suspend fun activities(userId: Long, hideSpoilers: Boolean) = api.userContentActivityFeed(userId, limit = 200, hideSpoilers = hideSpoilers)
    override suspend fun books() = api.currentUserUploadedBooks()
    override suspend fun inventory() = api.currentUserInventory()
    override suspend fun shop() = api.shopItems()
    override suspend fun reward() = api.currentUserQuizRewardStatus()
    override suspend fun save(profile: UserProfile) = api.updateCurrentUser(profile)
    override suspend fun checkin() = api.checkinCurrentUser()
    override suspend fun verifyAdult(year: Int) = api.verifyCurrentUserAdult(year)
    override suspend fun equip(itemId: Long, equip: Boolean) = api.setCurrentUserEquipment(itemId, if (equip) "equip" else "unequip")
    override suspend fun purchase(itemId: Long) = api.purchaseShopItem(itemId)
}
