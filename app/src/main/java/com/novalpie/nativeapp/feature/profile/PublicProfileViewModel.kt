package com.novalpie.nativeapp.feature.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.*
import java.util.Calendar

internal interface PublicProfileRepository {
    suspend fun profile(userId: Long): UserProfile
    suspend fun activities(userId: Long, page: Int, hideSpoilers: Boolean): UserContentActivityFeed
    suspend fun books(userId: Long): List<NovelCard>
    suspend fun checkinStats(userId: Long): UserCheckinStats
    suspend fun checkinRecords(userId: Long, year: Int): List<UserCheckinRecord>
    suspend fun checkinSettings(userId: Long): UserCheckinSettings
}
internal class WebsitePublicProfileRepository(private val api: NovalPieApi, private val activityPageSize: Int = 200) : PublicProfileRepository {
    override suspend fun profile(userId: Long) = api.userProfile(userId)
    override suspend fun activities(userId: Long, page: Int, hideSpoilers: Boolean) = api.userContentActivityFeed(userId, page, activityPageSize, hideSpoilers)
    override suspend fun books(userId: Long) = api.userNovels(userId)
    override suspend fun checkinStats(userId: Long) = api.userCheckinStats(userId)
    override suspend fun checkinRecords(userId: Long, year: Int) = api.userCheckinRecords(userId, "$year-01-01", "$year-12-31")
    override suspend fun checkinSettings(userId: Long) = api.userCheckinSettings(userId)
}

/** Cache entries own requests; returning to a user never inherits another user's section state. */
internal class PublicProfileViewModel(private val repository: PublicProfileRepository, scope: CoroutineScope? = null) : ViewModel() {
    private val parent = scope ?: viewModelScope
    private val work = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
    private class Entry(var state: UserProfileDetailState, var hideSpoilers: Boolean) {
        val jobs = mutableMapOf<String, Job>()
        val revisions = mutableMapOf<String, Long>()
        val scroll = mutableMapOf<String, GridScrollPosition>()
    }
    private val entries = LinkedHashMap<Long, Entry>(8, .75f, true)
    private var environment = 0L
    var state by mutableStateOf(UserProfileDetailState())
        private set

    fun present(transform: (UserProfileDetailState) -> UserProfileDetailState) {
        val entry = entries[state.userId] ?: return
        update(entry, transform)
    }
    fun enter(userId: Long, hideSpoilers: Boolean = true, refresh: Boolean = false) {
        if (userId <= 0) return
        val existed = entries[userId]
        val entry = existed ?: Entry(UserProfileDetailState(userId = userId), hideSpoilers).also { entries[userId] = it }
        state = entry.state
        while (entries.size > 6) {
            val removed = entries.remove(entries.keys.first())
            removed?.jobs?.values?.forEach(Job::cancel)
        }
        val policyChanged = entry.hideSpoilers != hideSpoilers
        entry.hideSpoilers = hideSpoilers
        if (existed == null || refresh) {
            loadHero(entry)
            PublicUserProfilePanel.entries.forEach { request(entry, it) }
        } else if (policyChanged) request(entry, PublicUserProfilePanel.Activities)
    }
    fun retry(panel: PublicUserProfilePanel) { entries[state.userId]?.let { request(it, panel) } }
    fun scrollPosition(): GridScrollPosition = entries[state.userId]?.scroll?.get(scrollKey()) ?: GridScrollPosition()
    fun saveScroll(index: Int, offset: Int) = saveScroll(state.userId, state.selectedTab, state.activityFilter, index, offset)
    fun saveScroll(userId: Long, tab: UserProfileTab, filter: ProfileActivityFilter, index: Int, offset: Int) {
        entries[userId]?.scroll?.set("$tab:$filter", GridScrollPosition.from(index, offset))
    }
    private fun scrollKey(): String = "${state.selectedTab}:${state.activityFilter}"
    fun loadMoreActivities() {
        val entry = entries[state.userId] ?: return
        val feed = entry.state.activityFeed ?: return
        if (entry.state.loadingMoreActivities || entry.state.activities !is LoadResult.Success || (!feed.hasMore && !feed.partialFailure)) return
        val page = entry.state.activityPage + 1
        val policy = entry.hideSpoilers
        update(entry) { it.copy(loadingMoreActivities = true, activityPageMessage = null) }
        launch(entry, PublicUserProfilePanel.Activities.name, { repository.activities(entry.state.userId, page, policy) }) { current, result ->
            result.fold({ next -> userProfileDetailWithLoadedActivities(current, Result.success(mergeActivityPages(current.activityFeed, next))).copy(
                activityPage = if (next.partialFailure) page - 1 else page, loadingMoreActivities = false,
                activityPageMessage = if (next.partialFailure) "部分动态读取失败，点击重试本页" else null)
            }, { current.copy(loadingMoreActivities = false, activityPageMessage = apiFailureMessage("更早动态", it)) })
        }
    }

    private fun update(entry: Entry, transform: (UserProfileDetailState) -> UserProfileDetailState) {
        entry.state = transform(entry.state)
        if (state.userId == entry.state.userId) state = entry.state
    }
    private fun <T> launch(entry: Entry, key: String, load: suspend () -> T, publish: (UserProfileDetailState, Result<T>) -> UserProfileDetailState) {
        val id = entry.state.userId
        val revision = (entry.revisions[key] ?: 0) + 1
        val account = environment
        entry.revisions[key] = revision
        entry.jobs[key]?.cancel()
        entry.jobs[key] = work.launch {
            val result = try { Result.success(load()) } catch (cancelled: CancellationException) { throw cancelled } catch (failure: Exception) { Result.failure(failure) }
            if (account == environment && entries[id] === entry && entry.revisions[key] == revision) update(entry) { publish(it, result) }
        }
    }
    private fun loadHero(entry: Entry) {
        update(entry) { it.copy(profile = LoadResult.Loading) }
        launch(entry, "hero", { repository.profile(entry.state.userId) }, ::userProfileDetailWithLoadedProfile)
    }
    private fun request(entry: Entry, panel: PublicUserProfilePanel) {
        val id = entry.state.userId
        update(entry) { userProfileDetailForPanelRetry(it, panel) }
        when (panel) {
            PublicUserProfilePanel.Activities -> {
                val pages = entry.state.activityPage.coerceAtLeast(1)
                val policy = entry.hideSpoilers
                update(entry) { it.copy(loadingMoreActivities = false, activityPageMessage = null) }
                launch(entry, panel.name, { reloadActivityPages(pages) { page -> repository.activities(id, page, policy) } }) { state, result ->
                    userProfileDetailWithLoadedActivities(state, result.map { it.first }).copy(
                        activityPage = result.getOrNull()?.second ?: state.activityPage,
                        activityPageMessage = if (result.getOrNull()?.first?.partialFailure == true) "部分动态读取失败，点击重试本页" else null)
                }
            }
            PublicUserProfilePanel.Books -> launch(entry, panel.name, { repository.books(id) }, ::userProfileDetailWithLoadedBooks)
            PublicUserProfilePanel.CheckinStats -> launch(entry, panel.name, { repository.checkinStats(id) }) { state, result -> state.copy(checkinStats = result.loadResult("签到统计")) }
            PublicUserProfilePanel.CheckinRecords -> launch(entry, panel.name, { repository.checkinRecords(id, Calendar.getInstance().get(Calendar.YEAR)) }) { state, result -> state.copy(checkinRecords = result.loadResult("签到记录")) }
            PublicUserProfilePanel.CheckinSettings -> launch(entry, panel.name, { repository.checkinSettings(id) }) { state, result -> state.copy(checkinSettings = result.loadResult("签到设置")) }
        }
    }
    private fun <T> Result<T>.loadResult(label: String): LoadResult<T> = fold({ LoadResult.Success(it) }, { LoadResult.Error(apiFailureMessage(label, it)) })
    fun environmentChanged() { environment++; work.coroutineContext.cancelChildren(); entries.clear(); state = UserProfileDetailState() }
    fun close() { environment++; work.cancel(); entries.clear() }
}
