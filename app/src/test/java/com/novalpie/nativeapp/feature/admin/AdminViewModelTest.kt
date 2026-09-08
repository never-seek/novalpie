package com.novalpie.nativeapp.feature.admin

import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AdminViewModelTest {
    private open class Repository : AdminRepository {
        val writes = mutableListOf<AdminCommand>()
        override suspend fun overview(days: Int) = AdminOverviewStats(todayUsers = days)
        override suspend fun reviewSettings() = AdminReviewSettings()
        override suspend fun reviews(query: AdminReviewQuery) = emptyList<AdminReviewRequest>()
        override suspend fun keys() = emptyList<AdminKeyItem>()
        override suspend fun rules() = emptyList<AdminBaseUrlRule>()
        override suspend fun logs(query: AdminOperationLogQuery) = AdminOperationLogPage(totalPages = query.page)
        override suspend fun cookies() = emptyList<AdminCookieConfig>()
        override suspend fun scheduler(lines: Int) = AdminSchedulerLogs(totalLines = lines)
        override suspend fun shop(query: AdminShopQuery) = emptyList<AdminShopItem>()
        override suspend fun mutate(command: AdminCommand): UserCheckinAction { writes += command; return UserCheckinAction(true) }
    }
    @Test fun slowSiblingReadDoesNotHideTheReadyReviewList() = runBlocking {
        val slow = CompletableDeferred<AdminReviewSettings>()
        val repository = object : Repository() { override suspend fun reviewSettings() = slow.await() }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = AdminViewModel(repository, { true }, scope)
        try {
            model.load(AdminSection.Review)
            assertEquals(LoadResult.Loading, model.state.reviewSettings)
            assertTrue(model.state.reviewRequests is LoadResult.Success)
            slow.complete(AdminReviewSettings(true, false))
            assertTrue((model.state.reviewSettings as LoadResult.Success).value.autoApproveUpload)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun refreshAndSectionSwitchCannotReleaseOrDuplicateAnInFlightWrite() = runBlocking {
        val result = CompletableDeferred<UserCheckinAction>()
        val repository = object : Repository() { override suspend fun mutate(command: AdminCommand): UserCheckinAction { writes += command; return result.await() } }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = AdminViewModel(repository, { true }, scope)
        try {
            model.load(AdminSection.Review)
            model.mutate(AdminCommand.Review(1, "approve"))
            model.load(AdminSection.Shop)
            assertTrue(model.state.actionLoading)
            model.mutate(AdminCommand.Review(1, "approve"))
            assertEquals(1, repository.writes.size)
            result.complete(UserCheckinAction(true))
            assertEquals(AdminSection.Shop, model.state.section)
            assertNull(model.state.actionMessage)
            assertFalse(model.state.actionLoading)
            model.enter(AdminSection.Review)
            assertEquals("审核请求已处理", model.state.actionMessage)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun rejectedActionKeepsEditableDraftAndNeverShowsSuccessOrRefreshesItAway() = runBlocking {
        val repository = object : Repository() { override suspend fun mutate(command: AdminCommand): UserCheckinAction { writes += command; return UserCheckinAction(false, "明确拒绝") } }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = AdminViewModel(repository, { true }, scope)
        try {
            val item = AdminShopItem(10, "我的未保存名称", type = "frame", imageUrl = "https://fixture.invalid/a.png")
            model.load(AdminSection.Shop); model.mutate(AdminCommand.SaveShop(item))
            assertTrue(model.state.actionError); assertFalse(model.state.actionUncertain)
            assertEquals("明确拒绝", model.state.actionMessage)
            assertEquals(item, (model.state.failedEditDraft as AdminEditDraft.Shop).item)
            model.load(AdminSection.Keys); model.enter(AdminSection.Shop)
            assertEquals(item, (model.state.failedEditDraft as AdminEditDraft.Shop).item)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun roleAndAccountChangeInvalidateEvenAnUncancellableLateResponse() = runBlocking {
        val result = CompletableDeferred<AdminOverviewStats>()
        val repository = object : Repository() { override suspend fun overview(days: Int) = withContext(NonCancellable) { result.await() } }
        var allowed = true
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = AdminViewModel(repository, { allowed }, scope)
        try {
            model.load(AdminSection.Overview); allowed = false; model.environmentChanged()
            result.complete(AdminOverviewStats(todayUsers = 999))
            model.mutate(AdminCommand.DeleteKey(1)); model.load(AdminSection.Keys)
            assertEquals(AdminState(accessRevision = model.state.accessRevision), model.state)
            assertTrue(repository.writes.isEmpty())
        } finally { model.close(); scope.cancel() }
    }
    @Test fun newerFilterResponseWinsAndServerRequestsUseFrozenQuery() = runBlocking {
        val old = CompletableDeferred<List<AdminReviewRequest>>()
        val queries = mutableListOf<AdminReviewQuery>()
        val repository = object : Repository() { override suspend fun reviews(query: AdminReviewQuery): List<AdminReviewRequest> {
            queries += query
            return if (query.keyword == "old") withContext(NonCancellable) { old.await() } else listOf(AdminReviewRequest(2, "upload", "pending"))
        } }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = AdminViewModel(repository, { true }, scope)
        try {
            model.present { it.copy(reviewQuery = AdminReviewQuery(keyword = "old")) }; model.load(AdminSection.Review)
            model.present { it.copy(reviewQuery = AdminReviewQuery(keyword = "new")) }; model.load(AdminSection.Review)
            old.complete(listOf(AdminReviewRequest(1, "upload", "pending")))
            assertEquals(listOf(2L), (model.state.reviewRequests as LoadResult.Success).value.map { it.id })
            assertEquals(listOf("old", "new"), queries.map { it.keyword })
        } finally { model.close(); scope.cancel() }
    }
    @Test fun uncertainCookieWriteKeepsRawOnlyInMemoryAndDoesNotLeakItIntoDiagnostics() = runBlocking {
        val raw = "synthetic-secret-value"
        val repository = object : Repository() { override suspend fun mutate(command: AdminCommand): UserCheckinAction { writes += command; throw java.io.IOException(raw) } }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = AdminViewModel(repository, { true }, scope)
        try {
            model.load(AdminSection.Scraper)
            model.mutate(AdminCommand.SaveCookie(AdminCookieConfig(2, "synthetic"), raw))
            assertTrue(model.state.actionUncertain); assertEquals(1, repository.writes.size)
            assertEquals(raw, (model.state.failedEditDraft as AdminEditDraft.Cookie).raw)
            assertFalse(model.state.toString().contains(raw)); assertFalse(repository.writes.single().toString().contains(raw))
            model.environmentChanged(); assertNull(model.state.failedEditDraft)
        } finally { model.close(); scope.cancel() }
    }
}
