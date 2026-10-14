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
import java.util.TimeZone

/** Independent account surface. Repository operations never mutate another feature's state. */
internal class ProfileViewModel(
    private val repository: ProfileRepository,
    initial: ProfileState = ProfileState(),
    scope: CoroutineScope? = null,
    private val onProfile: (UserProfile) -> Unit = {},
    private val checkinReceipts:CheckinReceiptStore=MemoryCheckinReceiptStore(),
    private val nowMillis:()->Long=System::currentTimeMillis,
) : ViewModel() {
    private val parent = scope ?: viewModelScope
    private val work = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
    private var generation = 0L
    private var environment = 0L
    private var editRevision = 0L
    private var heroRevision = 0L
    private var inventoryRevision = 0L
    private var activityRevision = 0L
    private var draftDirty = false
    private var request: Job? = null
    private var tokenProfile: UserProfile? = null
    private var hideSpoilers = true
    private val localCheckinReceipts=mutableMapOf<Pair<Long,String>,CheckinReceipt>()
    private fun receipt(accountId:Long,date:String)=localCheckinReceipts[accountId to date] ?: checkinReceipts.get(accountId,date)
    private fun saveReceipt(accountId:Long,date:String,value:CheckinReceipt) {
        localCheckinReceipts[accountId to date]=value
        checkinReceipts.put(accountId,date,value)
    }
    private fun ownerId():Long?=tokenProfile?.id ?: (state.profile as? LoadResult.Success)?.value?.id
    var state by mutableStateOf(initial)
        private set
    fun present(transform: (ProfileState) -> ProfileState) { state = transform(state) }

    fun load(identity: UserProfile?, hideSpoilers: Boolean) {
        tokenProfile = identity; this.hideSpoilers = hideSpoilers
        val serial = ++generation
        val hero = ++heroRevision
        val inventory = ++inventoryRevision
        val activity = ++activityRevision
        val activityPages = state.activityPage.coerceAtLeast(1)
        request?.cancel()
        state = state.copy(profile = state.profile.takeIf { it is LoadResult.Success } ?: LoadResult.Loading,
            checkinStats = LoadResult.Loading, checkinRecords = LoadResult.Loading, activities = LoadResult.Loading,
            books = LoadResult.Loading, inventory = LoadResult.Loading, shopItems = LoadResult.Loading, quizReward = LoadResult.Loading,
            loadingMoreActivities = false, activityPageMessage = null)
        request = work.launch { supervisorScope {
            val profile = async { attempt { repository.profile() } }
            val today=checkinSiteDay(nowMillis())
            val year=today.take(4).toInt()
            launch {
                val profResult = profile.await()
                if (serial == generation && hero == heroRevision) {
                    val enriched = if (profResult.isSuccess) {
                        Result.success(enrichProfileWithCheckinSettings(profResult.getOrThrow()))
                    } else profResult
                    applyHero(enriched)
                    publish()
                }
            }
            launch { val result = attempt { repository.checkinStats() }; if (serial == generation) state = currentUserProfileWithLoadedCheckinStats(state, result, today) }
            launch {
                val result = attempt { repository.checkinRecords(year) }
                if (serial == generation) {
                    state = currentUserProfileWithLoadedCheckinRecords(state, result, today)
                    val prof = profile.await().getOrNull()?.let { enrichProfileWithCheckinSettings(it) }
                    val records = result.getOrNull().orEmpty()
                    if(serial!=generation)return@launch
                    val id=prof?.id ?: identity?.id
                    if(id!=null&&records.any{it.date==today})runCatching{saveReceipt(id,today,CheckinReceipt.Confirmed)}
                    if(prof?.autoCheckin==true&&id!=null&&records.none{it.date==today})checkin(isAuto=true,expectedOwner=id)
                }
            }
            launch {
                val result = attempt {
                    val id = identity?.id ?: profile.await().getOrThrow().id ?: error("缺少用户ID")
                    reloadActivityPages(activityPages) { page -> repository.activityPage(id, page, hideSpoilers) }
                }
                if (serial == generation && activity == activityRevision) {
                    state = currentUserProfileWithLoadedActivities(state, result.map { it.first }).copy(
                        activityPage = result.getOrNull()?.second ?: state.activityPage,
                        activityPageMessage = if (result.getOrNull()?.first?.partialFailure == true) "部分动态读取失败，点击重试本页" else null)
                    publish()
                }
            }
            launch { val result = attempt { repository.books() }; if (serial == generation) { state = currentUserProfileWithLoadedBooks(state, result); publish() } }
            launch { val result = attempt { repository.inventory() }; if (serial == generation && inventory == inventoryRevision) { state = currentUserProfileWithLoadedInventory(state, result); publish() } }
            launch { val result = attempt { repository.shop() }; if (serial == generation) state = state.copy(shopItems = result.asLoad("商店")) }
            launch { val result = attempt { repository.reward() }; if (serial == generation) state = state.copy(quizReward = result.asLoad("奖励")) }
        } }
    }
    fun loadMoreActivities() {
        val feed = state.activityFeed ?: return
        val id = (state.profile as? LoadResult.Success)?.value?.id ?: return
        if (state.loadingMoreActivities || state.activities !is LoadResult.Success || (!feed.hasMore && !feed.partialFailure)) return
        val serial = ++activityRevision
        val account = environment
        val page = state.activityPage + 1
        state = state.copy(loadingMoreActivities = true, activityPageMessage = null)
        work.launch {
            val result = attempt { repository.activityPage(id, page, hideSpoilers) }
            if (serial != activityRevision || account != environment) return@launch
            result.onSuccess { next ->
                state = currentUserProfileWithLoadedActivities(state, Result.success(mergeActivityPages(state.activityFeed, next))).copy(
                    loadingMoreActivities = false, activityPage = if (next.partialFailure) page - 1 else page,
                    activityPageMessage = if (next.partialFailure) "部分动态读取失败，点击重试本页" else null)
                publish()
            }.onFailure { state = state.copy(loadingMoreActivities = false, activityPageMessage = apiFailureMessage("更早动态", it)) }
        }
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
            val partial=result.exceptionOrNull() as? ProfileSavePartialFailure
            if(partial!=null) {
                heroRevision++
                draftDirty=true
                val current=(state.profile as? LoadResult.Success)?.value
                val confirmed=partial.confirmedProfile.copy(showCheckin=current?.showCheckin,autoCheckin=current?.autoCheckin)
                state=state.copy(profile=LoadResult.Success(confirmed))
            }
            state = state.copy(saving = false, actionMessage = result.fold(
                { if (draftDirty) "本次资料已保存，后续修改尚未保存" else "资料已保存" }, { if(it is ProfileSavePartialFailure)it.message else apiFailureMessage("保存资料",it) }))
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

    fun checkin(isAuto:Boolean=false,expectedOwner:Long?=ownerId()) {
        if(state.checkingIn)return
        val knownId=expectedOwner?.takeIf{it>0}
        val date=checkinSiteDay(nowMillis())
        val existing=knownId?.let {receipt(it,date)}
        if(existing==CheckinReceipt.Confirmed) {
            if(!isAuto)state=state.copy(actionMessage="今日已签到")
            return
        }
        if(existing!=null&&existing!=CheckinReceipt.Confirmed&&isAuto) {
            state=state.copy(actionMessage=if(existing==CheckinReceipt.Rejected)"上次自动签到未成功，未自动重发；可手动重试"else "签到请求结果未确认，未自动重发；请刷新记录后核对")
            return
        }
        val account=environment
        state=state.copy(checkingIn=true,actionMessage=if(isAuto)"正在自动签到…"else "正在签到…")
        work.launch {
            val userId=knownId ?: attempt {repository.profile().id?.takeIf{it>0} ?: error("缺少签到账号ID，请刷新资料后重试")}.getOrElse {
                if(account==environment)state=state.copy(checkingIn=false,actionMessage=apiFailureMessage("签到账号",it))
                return@launch
            }
            if(account!=environment)return@launch
            val previous=receipt(userId,date)
            if(previous==CheckinReceipt.Confirmed || previous!=null&&previous!=CheckinReceipt.Confirmed&&isAuto) {
                state=state.copy(checkingIn=false,actionMessage=if(previous==CheckinReceipt.Confirmed)"今日已签到"else "签到请求结果未确认，未自动重发；请刷新记录后核对")
                return@launch
            }
            // Persist an attempted write before POST. A refresh/process restart cannot blindly replay an unknown write.
            val persisted=attempt {saveReceipt(userId,date,CheckinReceipt.Attempted)}
            if(persisted.isFailure){state=state.copy(checkingIn=false,actionMessage=persisted.exceptionOrNull()?.message);return@launch}
            val result=attempt {repository.checkin()}
            if(account!=environment||ownerId()!=null&&ownerId()!=userId)return@launch
            val already=checkinAlreadyCompletedMessage(result.getOrNull()?.message ?: result.exceptionOrNull()?.message)
            val confirmed=result.getOrNull()?.success==true||already
            if(confirmed)runCatching {saveReceipt(userId,date,CheckinReceipt.Confirmed)}
            else if(result.isSuccess || (result.exceptionOrNull() as? com.novalpie.nativeapp.data.NovalPieApiException)?.statusCode in listOf(400,401,403,404,422)) {
                runCatching {saveReceipt(userId,date,CheckinReceipt.Rejected)}
            }
            state=state.copy(checkingIn=false,actionMessage=when {
                already->result.getOrNull()?.message ?: "今日已签到"
                result.getOrNull()?.success==true->result.getOrNull()?.message ?: if(isAuto)"自动签到成功"else "签到成功"
                result.isSuccess->result.getOrNull()?.message ?: "签到未完成"
                else->if(isAuto)"签到请求结果未确认，未自动重发；请刷新记录后核对"else apiFailureMessage("签到",result.exceptionOrNull()!!)
            })
            if(confirmed)refreshAfterWrite(account,refreshCheckin=true)
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
        launch {
            val result = attempt { repository.profile() }
            if (account == environment && hero == heroRevision) {
                if (result.isSuccess) {
                    val enriched = Result.success(enrichProfileWithCheckinSettings(result.getOrThrow()))
                    applyHero(enriched)
                    publish()
                } else refreshFailed()
            }
        }
        if (refreshInventory) launch { val result = attempt { repository.inventory() }; if (account == environment && inventory == inventoryRevision) {
            state = currentUserProfileWithLoadedInventory(state, result); if (result.isFailure) refreshFailed(); publish()
        } }
        if (refreshShop) launch { val result = attempt { repository.shop() }; if (account == environment) { state = state.copy(shopItems = result.asLoad("商店")); if (result.isFailure) refreshFailed() } }
        if (refreshCheckin) {
            val today=checkinSiteDay(nowMillis())
            val year=today.take(4).toInt()
            launch { val result = attempt { repository.checkinStats() }; if (account == environment) { state = currentUserProfileWithLoadedCheckinStats(state, result, today); if (result.isFailure) refreshFailed() } }
            launch { val result = attempt { repository.checkinRecords(year) }; if (account == environment) { state = currentUserProfileWithLoadedCheckinRecords(state, result, today); if (result.isFailure) refreshFailed() } }
            launch { val result = attempt { repository.reward() }; if (account == environment) state = state.copy(quizReward = result.asLoad("奖励状态")) }
        }
    }
    private suspend fun enrichProfileWithCheckinSettings(profile: UserProfile): UserProfile {
        if (profile.autoCheckin != null) return profile
        val settings = attempt { repository.checkinSettings() }.getOrNull() ?: return profile
        return profile.copy(
            autoCheckin = settings.autoCheckin,
            showCheckin = profile.showCheckin ?: settings.showCheckin
        )
    }
    private fun refreshFailed() { state = state.copy(actionMessage = "操作已提交成功，部分资料刷新失败；请刷新查看，不要重复提交") }
    fun environmentChanged() { environment++; generation++; heroRevision++; inventoryRevision++; draftDirty = false; editRevision = 0; tokenProfile = null; work.coroutineContext.cancelChildren(); state = ProfileState(booksGridColumns = state.booksGridColumns, downloadImageConcurrency = state.downloadImageConcurrency, downloadCompressImages = state.downloadCompressImages, downloadImageQuality = state.downloadImageQuality, downloadZipCompressionLevel = state.downloadZipCompressionLevel) }
    private fun publish() { (state.profile as? LoadResult.Success)?.value?.let(onProfile) }
    private suspend fun <T> attempt(action: suspend () -> T): Result<T> = try { Result.success(action()) } catch (cancelled: CancellationException) { throw cancelled } catch (failure: Exception) { Result.failure(failure) }
    private fun <T> Result<T>.asLoad(label: String): LoadResult<T> = fold({ LoadResult.Success(it) }, { LoadResult.Error(apiFailureMessage(label, it)) })
    fun close() { environment++; generation++; work.cancel() }
}
