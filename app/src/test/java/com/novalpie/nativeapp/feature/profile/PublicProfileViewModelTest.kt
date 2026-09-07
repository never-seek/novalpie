package com.novalpie.nativeapp.feature.profile

import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.PublicUserProfilePanel
import com.novalpie.nativeapp.ui.UserProfileTab
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PublicProfileViewModelTest {
    @Test fun disposingAnOldTabWritesItsOwnScrollNotTheNewTab() = runTest {
        val model = PublicProfileViewModel(Repository(), backgroundScope)
        model.enter(1); runCurrent()
        model.present { it.copy(selectedTab = UserProfileTab.Books) }
        model.saveScroll(1, UserProfileTab.Activities, com.novalpie.nativeapp.ui.ProfileActivityFilter.All, 14, 27)
        assertEquals(0, model.scrollPosition().firstVisibleItemIndex)
        model.present { it.copy(selectedTab = UserProfileTab.Activities) }
        assertEquals(14, model.scrollPosition().firstVisibleItemIndex)
    }
    @Test fun scrollPositionsAreIsolatedByUserAndTab() = runTest {
        val model = PublicProfileViewModel(Repository(), backgroundScope)
        model.enter(1); runCurrent(); model.saveScroll(12, 33)
        model.present { it.copy(selectedTab = UserProfileTab.Books) }
        assertEquals(0, model.scrollPosition().firstVisibleItemIndex)
        model.saveScroll(3, 44); model.enter(2); runCurrent(); model.saveScroll(5, 11)
        model.enter(1); runCurrent()
        assertEquals(3, model.scrollPosition().firstVisibleItemIndex)
        model.present { it.copy(selectedTab = UserProfileTab.Activities) }
        assertEquals(12, model.scrollPosition().firstVisibleItemIndex)
    }
    @Test fun aPartialPageIsRetriedBeforeAdvancingAndDoesNotDuplicatePreviouslyAvailableEntries() = runTest {
        var calls = 0
        val pages = mutableListOf<Int>()
        val model = PublicProfileViewModel(object : Repository() {
            override suspend fun activities(userId: Long, page: Int, hideSpoilers: Boolean): UserContentActivityFeed {
                calls++; pages += page
                return if (calls == 1) UserContentActivityFeed(listOf(UserActivity(1, "post", "一")), partialFailure = true)
                else UserContentActivityFeed(listOf(UserActivity(1, "post", "一"), UserActivity(2, "post", "二")))
            }
        }, backgroundScope)
        model.enter(1); runCurrent(); assertEquals(0, model.state.activityPage)
        model.loadMoreActivities(); runCurrent()
        assertEquals(listOf(1, 1), pages)
        assertEquals(2, (model.state.activities as LoadResult.Success).value.size)
        assertEquals(1, model.state.activityPage)
    }
    @Test fun loadingOlderActivityPagesMergesAndRetryDoesNotLoseAlreadyLoadedPages() = runTest {
        val model = PublicProfileViewModel(object : Repository() {
            override suspend fun activities(userId: Long, page: Int, hideSpoilers: Boolean) = UserContentActivityFeed(
                activities = listOf(UserActivity(page.toLong(), "post", "合成动态$page")), hasMore = page < 2)
        }, backgroundScope)
        model.enter(1); runCurrent(); model.loadMoreActivities(); runCurrent()
        assertEquals(2, (model.state.activities as LoadResult.Success).value.size)
        assertEquals(2, model.state.activityPage)
        model.retry(PublicUserProfilePanel.Activities); runCurrent()
        assertEquals(setOf(1L, 2L), (model.state.activities as LoadResult.Success).value.map { it.id }.toSet())
    }
    private open class Repository : PublicProfileRepository {
        override suspend fun profile(userId: Long) = UserProfile(userId, "用户$userId")
        override suspend fun activities(userId: Long, page: Int, hideSpoilers: Boolean) = UserContentActivityFeed()
        override suspend fun books(userId: Long) = listOf(NovelCard(userId * 10, "作品$userId"))
        override suspend fun checkinStats(userId: Long) = UserCheckinStats()
        override suspend fun checkinRecords(userId: Long, year: Int) = emptyList<UserCheckinRecord>()
        override suspend fun checkinSettings(userId: Long) = UserCheckinSettings()
    }
    @Test fun lateOldProfileCannotReplaceTheCurrentUserAndReturningRestoresTabs() = runTest {
        val old = CompletableDeferred<UserProfile>()
        val model = PublicProfileViewModel(object : Repository() {
            override suspend fun profile(userId: Long) = if (userId == 1L) withContext(NonCancellable) { old.await() } else super.profile(userId)
        }, backgroundScope)
        model.enter(1); runCurrent(); model.present { it.copy(selectedTab = UserProfileTab.Books) }
        model.enter(2); runCurrent(); old.complete(UserProfile(1, "旧用户")); runCurrent()
        assertEquals(2L, model.state.userId)
        assertEquals(2L, (model.state.profile as LoadResult.Success).value.id)
        model.enter(1); runCurrent()
        assertEquals(UserProfileTab.Books, model.state.selectedTab)
        assertTrue(model.state.profile is LoadResult.Success)
    }
    @Test fun retryingActivitiesCannotCancelAnIndependentBookRequest() = runTest {
        val books = CompletableDeferred<List<NovelCard>>()
        val model = PublicProfileViewModel(object : Repository() { override suspend fun books(userId: Long) = books.await() }, backgroundScope)
        model.enter(1); runCurrent(); model.retry(PublicUserProfilePanel.Activities); runCurrent()
        books.complete(listOf(NovelCard(10, "作品"))); runCurrent()
        assertTrue(model.state.books is LoadResult.Success)
    }
    @Test fun accountEnvironmentChangeClearsCachedProfilesIncludingLateReplies() = runTest {
        val late = CompletableDeferred<List<NovelCard>>()
        val model = PublicProfileViewModel(object : Repository() { override suspend fun books(userId: Long) = withContext(NonCancellable) { late.await() } }, backgroundScope)
        model.enter(1); runCurrent(); model.environmentChanged(); late.complete(listOf(NovelCard(1, "旧会话"))); runCurrent()
        assertEquals(0L, model.state.userId)
        assertTrue(model.state.books is LoadResult.Idle)
    }
}
