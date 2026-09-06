package com.novalpie.nativeapp.feature.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.*
import java.util.Calendar
import java.util.Locale

/** Independent account surface. Repository operations never mutate another feature's state. */
internal class ProfileViewModel(
    private val repository: ProfileRepository,
    initial: ProfileState = ProfileState(),
    scope: CoroutineScope? = null,
    private val onProfile: (UserProfile) -> Unit = {},
) : ViewModel() {
    private val parent = scope ?: viewModelScope
    private val work = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
    private var generation = 0L
    private var environment = 0L
    private var editRevision = 0L
    private var draftDirty = false
    private var heroRevision = 0L
    private var inventoryRevision = 0L
    private var request: Job? = null
    private var tokenProfile: UserProfile? = null
    private var hideSpoilers = true
    var state by mutableStateOf(initial)
        private set
    fun present(transform: (ProfileState) -> ProfileState) { state = transform(state) }

    fun load(identity: UserProfile?, hideSpoilers: Boolean) {
        tokenProfile = identity; this.hideSpoilers = hideSpoilers
        val serial = ++generation
        val hero = ++heroRevision
        val inventory = ++inventoryRevision
        request?.cancel()
        state = state.copy(profile = state.profile.takeIf { it is LoadResult.Success } ?: LoadResult.Loading,
            checkinStats = LoadResult.Loading, checkinRecords = LoadResult.Loading, activities = LoadResult.Loading,
            books = LoadResult.Loading, inventory = LoadResult.Loading, shopItems = LoadResult.Loading, quizReward = LoadResult.Loading)
        request = work.launch { supervisorScope {
            val profile = async { attempt { repository.profile() } }
            val year = Calendar.getInstance().get(Calendar.YEAR)
            val today = String.format(Locale.US, "%tF", Calendar.getInstance())
            launch { val result = profile.await(); if (serial == generation && hero == heroRevision) { applyHero(result); publish() } }
            launch { val result = attempt { repository.checkinStats() }; if (serial == generation) state = currentUserProfileWithLoadedCheckinStats(state, result, today) }
            launch { val result = attempt { repository.checkinRecords(year) }; if (serial == generation) state = currentUserProfileWithLoadedCheckinRecords(state, result, today) }
            launch {
                val result = attempt { repository.activities(identity?.id ?: profile.await().getOrThrow().id ?: error("缺少用户ID"), hideSpoilers) }
                if (serial == generation) { state = currentUserProfileWithLoadedActivities(state, result); publish() }
            }
            launch { val result = attempt { repository.books() }; if (serial == generation) { state = currentUserProfileWithLoadedBooks(state, result); publish() } }
            launch { val result = attempt { repository.inventory() }; if (serial == generation && inventory == inventoryRevision) { state = currentUserProfileWithLoadedInventory(state, result); publish() } }
            launch { val result = attempt { repository.shop() }; if (serial == generation) state = state.copy(shopItems = result.asLoad("商店")) }
            launch { val result = attempt { repository.reward() }; if (serial == generation) state = state.copy(quizReward = result.asLoad("奖励")) }
        } }
    }
    fun edit(transform: (ProfileState) -> ProfileState) { editRevision++; draftDirty = true; state = transform(state) }
    private fun applyHero(result: Result<UserProfile>) {
        val old = state
        state = currentUserProfileWithLoadedHero(state, result, tokenProfile)
        if (draftDirty) state = state.copy(nameDraft = old.nameDraft, bioDraft = old.bioDraft, showCheckin = old.showCheckin, autoCheckin = old.autoCheckin)
    }
    fun save() {
        if (state.saving) return
        val profile = (state.profile as? LoadResult.Success)?.value ?: return
        val updated = profile.copy(name = state.nameDraft.trim(), bio = state.bioDraft.trim(), showCheckin = state.showCheckin, autoCheckin = state.autoCheckin)
        if (updated.name.isBlank()) { state = state.copy(actionMessage = "用户名不能为空"); return }
        state = state.copy(saving = true)
        val account = environment
        val edits = editRevision
        work.launch {
            val result = attempt { repository.save(updated) }
            if (account != environment) return@launch
            if (result.isSuccess) {
                draftDirty = edits != editRevision
                heroRevision++
                applyHero(result)
            }
            state = state.copy(saving = false, actionMessage = result.fold(
                { if (draftDirty) "本次资料已保存，后续修改尚未保存" else "资料已保存" }, { apiFailureMessage("保存资料", it) }))
            publish()
        }
    }
    fun equip(item: UserInventoryItem) {
        if (state.inventoryActionInventoryId != null || state.shopPurchaseItemId != null) return
        val account = environment
        state = state.copy(inventoryActionInventoryId = item.inventoryId)
        work.launch {
            val result = attempt { repository.equip(item.itemId, !item.equipped).also { check(it.success) { it.message ?: "装扮操作未完成" } } }
            if (account != environment) return@launch
            state = state.copy(inventoryActionInventoryId = null, actionMessage = result.fold(
                { it.message ?: if (item.equipped) "已卸下" else "装备成功" }, { apiFailureMessage("装扮", it) }))
            if (result.isSuccess) refreshAfterWrite(account, refreshInventory = true)
        }
    }
    fun purchase(item: ShopItem) {
        if (state.shopPurchaseItemId != null || state.inventoryActionInventoryId != null) return
        val account = environment
        state = state.copy(shopPurchaseItemId = item.id, actionMessage = "正在购买…")
        work.launch {
            val result = attempt { repository.purchase(item.id).also { check(it.success) { it.message ?: "购买未完成" } } }
            if (account != environment) return@launch
            state = state.copy(shopPurchaseItemId = null, actionMessage = result.fold({ it.message ?: "购买成功" }, { apiFailureMessage("购买", it) }))
            if (result.isSuccess) refreshAfterWrite(account, refreshInventory = true, refreshShop = true)
        }
    }

    fun checkin() {
        if (state.checkingIn) return
        val account = environment
        state = state.copy(checkingIn = true, actionMessage = "正在签到…")
        work.launch {
            val result = attempt { repository.checkin().also { check(it.success) { it.message ?: "签到未完成" } } }
            if (account != environment) return@launch
            state = state.copy(checkingIn = false, actionMessage = result.fold({ it.message ?: "签到成功" }, { apiFailureMessage("签到", it) }))
            if (result.isSuccess) refreshAfterWrite(account, refreshCheckin = true)
        }
    }

    fun verifyAdult() {
        if (state.verifyingAdult) return
        val year = state.adultBirthYearDraft.toIntOrNull()
        if (year == null || year !in 1900..Calendar.getInstance().get(Calendar.YEAR)) { state = state.copy(actionMessage = "请输入有效出生年份"); return }
        val account = environment
        state = state.copy(verifyingAdult = true, actionMessage = "正在提交验证…")
        work.launch {
            val result = attempt { repository.verifyAdult(year).also { check(it.success) { it.message ?: "验证未通过" } } }
            if (account != environment) return@launch
            state = state.copy(verifyingAdult = false, actionMessage = result.fold({ it.message ?: "验证已提交" }, { apiFailureMessage("成年验证", it) }))
            if (result.isSuccess) refreshAfterWrite(account)
        }
    }

    fun uploadAvatar(upload: suspend () -> UserCheckinAction) {
        if (state.uploadingAvatar) return
        val account = environment
        state = state.copy(uploadingAvatar = true, actionMessage = "正在上传头像…")
        work.launch {
            val result = attempt { upload().also { check(it.success) { it.message ?: "头像上传未完成" } } }
            if (account != environment) return@launch
            state = state.copy(uploadingAvatar = false, actionMessage = result.fold({ it.message ?: "头像已上传" }, { apiFailureMessage("上传头像", it) }))
            if (result.isSuccess) refreshAfterWrite(account)
        }
    }

    /** A read-back error cannot turn an acknowledged purchase into an invitation to buy twice. */
    private suspend fun refreshAfterWrite(account: Long, refreshInventory: Boolean = false, refreshShop: Boolean = false, refreshCheckin: Boolean = false) = supervisorScope {
        val hero = ++heroRevision
        val inventory = if (refreshInventory) ++inventoryRevision else inventoryRevision
        launch { val result = attempt { repository.profile() }; if (account == environment && hero == heroRevision) {
            if (result.isSuccess) { applyHero(result); publish() } else refreshFailed()
        } }
        if (refreshInventory) launch { val result = attempt { repository.inventory() }; if (account == environment && inventory == inventoryRevision) {
            state = currentUserProfileWithLoadedInventory(state, result); if (result.isFailure) refreshFailed(); publish()
        } }
        if (refreshShop) launch { val result = attempt { repository.shop() }; if (account == environment) { state = state.copy(shopItems = result.asLoad("商店")); if (result.isFailure) refreshFailed() } }
        if (refreshCheckin) {
            val year = Calendar.getInstance().get(Calendar.YEAR)
            val today = String.format(Locale.US, "%tF", Calendar.getInstance())
            launch { val result = attempt { repository.checkinStats() }; if (account == environment) { state = currentUserProfileWithLoadedCheckinStats(state, result, today); if (result.isFailure) refreshFailed() } }
            launch { val result = attempt { repository.checkinRecords(year) }; if (account == environment) { state = currentUserProfileWithLoadedCheckinRecords(state, result, today); if (result.isFailure) refreshFailed() } }
            launch { val result = attempt { repository.reward() }; if (account == environment) state = state.copy(quizReward = result.asLoad("奖励状态")) }
        }
    }
    private fun refreshFailed() { state = state.copy(actionMessage = "操作已提交成功，部分资料刷新失败；请刷新查看，不要重复提交") }
    fun environmentChanged() { environment++; generation++; heroRevision++; inventoryRevision++; draftDirty = false; editRevision = 0; tokenProfile = null; work.coroutineContext.cancelChildren(); state = ProfileState(booksGridColumns = state.booksGridColumns, downloadImageConcurrency = state.downloadImageConcurrency) }
    private fun publish() { (state.profile as? LoadResult.Success)?.value?.let(onProfile) }
    private suspend fun <T> attempt(action: suspend () -> T): Result<T> = try { Result.success(action()) } catch (cancelled: CancellationException) { throw cancelled } catch (failure: Exception) { Result.failure(failure) }
    private fun <T> Result<T>.asLoad(label: String): LoadResult<T> = fold({ LoadResult.Success(it) }, { LoadResult.Error(apiFailureMessage(label, it)) })
    fun close() { environment++; generation++; work.cancel() }
}
