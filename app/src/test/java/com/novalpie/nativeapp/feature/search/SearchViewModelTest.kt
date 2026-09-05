package com.novalpie.nativeapp.feature.search

import com.novalpie.nativeapp.data.PersistedSearchSettings
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.NovelCard
import com.novalpie.nativeapp.model.NovelTag
import com.novalpie.nativeapp.model.SearchPage
import com.novalpie.nativeapp.ui.ResolvedSearchRequest
import com.novalpie.nativeapp.ui.SearchViewMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = mutableListOf<SearchViewModel>()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { models.forEach { it.close() }; Dispatchers.resetMain() }

    private fun model(repository: SearchRepository = FakeRepository(), preferences: MemoryPreferences = MemoryPreferences()) =
        SearchViewModel(repository, preferences).also(models::add)

    @Test fun firstEntryLoadsDefaultResultsWithoutReusingHistoryAsTheQuery() = runTest(dispatcher) {
        val repository = FakeRepository()
        val preferences = MemoryPreferences(history = listOf("上次的书"))
        val model = model(repository, preferences)
        model.enter()
        advanceUntilIdle()
        assertEquals("", model.state.keyword)
        assertEquals(listOf("上次的书"), model.state.history)
        assertEquals("", repository.requests.single().first.keyword)
        assertTrue(model.state.results is LoadResult.Success)
    }

    @Test fun switchingPresentationWhileLoadingDoesNotStrandTheSearchRequest() = runTest(dispatcher) {
        val reply = CompletableDeferred<SearchPage>()
        val repository = FakeRepository { _, _ -> reply.await() }
        val model = model(repository)
        model.enter()
        runCurrent()
        model.toggleViewMode()
        model.toggleCache()
        reply.complete(page(1, 11))
        advanceUntilIdle()
        assertEquals(SearchViewMode.List, model.state.options.viewMode)
        assertFalse(model.state.options.cacheEnabled)
        assertEquals(11L, (model.state.results as LoadResult.Success).value.single().id)
    }

    @Test fun lateOldReplyCannotOverwriteTheNewQueryEvenWhenCancellationIsIgnored() = runTest(dispatcher) {
        val old = CompletableDeferred<SearchPage>()
        val repository = FakeRepository { request, _ ->
            if (request.keyword == "old") withContext(NonCancellable) { old.await() } else page(1, 22)
        }
        val model = model(repository)
        model.updateKeyword("old")
        model.submit()
        runCurrent()
        model.updateKeyword("new")
        model.submit()
        runCurrent()
        old.complete(page(1, 11))
        advanceUntilIdle()
        assertEquals("new", model.state.keyword)
        assertEquals(22L, (model.state.results as LoadResult.Success).value.single().id)
    }

    @Test fun accountOrProxyChangeInvalidatesAnInFlightResultAndRestartsVisibleSearch() = runTest(dispatcher) {
        val old = CompletableDeferred<SearchPage>()
        var requests = 0
        val repository = FakeRepository { _, _ ->
            if (++requests == 1) withContext(NonCancellable) { old.await() } else page(1, 22)
        }
        val model = model(repository)
        model.enter()
        runCurrent()
        model.environmentChanged()
        runCurrent()
        old.complete(page(1, 11))
        advanceUntilIdle()
        assertEquals(22L, (model.state.results as LoadResult.Success).value.single().id)
    }

    @Test fun failedExplicitPageRetainsItsTargetForRetry() = runTest(dispatcher) {
        var fail = true
        val repository = FakeRepository { _, requested ->
            if (requested == 2 && fail) error("network down") else page(requested, requested.toLong())
        }
        val model = model(repository)
        model.enter()
        advanceUntilIdle()
        model.goToPage(2)
        advanceUntilIdle()
        assertTrue(model.state.results is LoadResult.Error)
        assertEquals(2, model.state.page)
        assertFalse(model.state.loadingPage)
        fail = false
        model.retry()
        advanceUntilIdle()
        assertEquals(2, model.state.page)
        assertEquals(listOf(1, 2, 2), repository.requests.map { it.second })
    }

    @Test fun blankingTheFieldAndClearingOptionsDoNotLeaveAnEmptySearchRoute() = runTest(dispatcher) {
        val repository = FakeRepository()
        val model = model(repository)
        model.enter()
        advanceUntilIdle()
        model.updateKeyword("something")
        model.submit()
        advanceUntilIdle()
        model.updateKeyword("")
        advanceUntilIdle()
        assertTrue(model.state.results is LoadResult.Success)
        model.clearOptions()
        advanceUntilIdle()
        assertTrue(model.state.results is LoadResult.Success)
        assertEquals("", repository.requests.last().first.keyword)
    }

    @Test fun leavingAndReturningPreservesResultPageAndViewportWithoutAnotherRequest() = runTest(dispatcher) {
        val repository = FakeRepository()
        val model = model(repository)
        model.enter()
        advanceUntilIdle()
        model.goToPage(3)
        advanceUntilIdle()
        model.saveScroll(7, 31)
        model.leave()
        model.enter()
        advanceUntilIdle()
        assertEquals(3, model.state.page)
        assertEquals(7, model.state.scroll.firstVisibleItemIndex)
        assertEquals(31, model.state.scroll.firstVisibleItemScrollOffset)
        assertEquals(2, repository.requests.size)
    }

    private class MemoryPreferences(var settings: PersistedSearchSettings = PersistedSearchSettings(), var history: List<String> = emptyList()) : SearchPreferences {
        override fun settings() = settings
        override fun save(settings: PersistedSearchSettings) { this.settings = settings }
        override fun history() = history
        override fun remember(keyword: String) { history = (listOf(keyword) + history).distinct().take(10) }
        override fun clearHistory() { history = emptyList() }
        override fun clearOptions() { settings = PersistedSearchSettings(cacheEnabled = settings.cacheEnabled) }
    }
    private class FakeRepository(val response: suspend (ResolvedSearchRequest, Int) -> SearchPage = { _, n -> page(n, n.toLong()) }) : SearchRepository {
        val requests = mutableListOf<Pair<ResolvedSearchRequest,Int>>()
        override suspend fun page(request: ResolvedSearchRequest, page: Int): SearchPage {
            requests += request to page
            return response(request, page)
        }
        override suspend fun tags(): List<NovelTag> = emptyList()
    }
    companion object {
        private fun page(number: Int, bookId: Long) = SearchPage(items = listOf(NovelCard(id=bookId,title="Book $bookId")),page=number,pageSize=60,total=180,totalPages=3)
    }
}
