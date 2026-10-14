package com.novalpie.nativeapp.feature.search

import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.data.*
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.ResolvedSearchRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SearchConditionRegressionTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = mutableListOf<SearchViewModel>()
    private lateinit var store: SearchSettingsStore
    private lateinit var preferences: StoredSearchPreferences
    private val chosen = PersistedSearchSettings(source="novelPia", adultFilter="adult_only",
        sortBy="updated_at", sortOrder="asc", scope="title", matchType="exact",
        wordCountRange="100000..500000", requiredTags=listOf("奇幻"), blockedTags=listOf("后宫"),
        advancedSyntaxEnabled=true, viewMode="list")
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        store=SearchSettingsStore(context).also { it.clear(); it.save(chosen) }
        preferences=StoredSearchPreferences(store, SearchHistoryStore(context))
    }
    @After fun cleanup() { models.forEach { it.close() }; Dispatchers.resetMain() }
    private class Repo: SearchRepository {
        val calls=mutableListOf<ResolvedSearchRequest>()
        override suspend fun page(request: ResolvedSearchRequest,page:Int):SearchPage {
            calls+=request; return SearchPage(emptyList(),page=page,pageSize=60)
        }
        override suspend fun tags()=emptyList<NovelTag>()
    }
    private fun model(repo:Repo=Repo())=SearchViewModel(repo,preferences).also(models::add)

    @Test fun clearingSavedConditionsDoesNotResetLiveFiltersOrTags() = runTest(dispatcher) {
        val vm=model();vm.enter();advanceUntilIdle();vm.saveScroll(5,27)
        val before=vm.state
        vm.clearOptions();advanceUntilIdle()
        assertEquals("Clearing a saved snapshot must not delete current selections",before,vm.state)
        assertEquals(PersistedSearchSettings(),store.load())
    }
    @Test fun clearingSavedConditionsDoesNotCancelOrRerunTheQuery() = runTest(dispatcher) {
        val repo=Repo();val vm=model(repo);vm.enter();advanceUntilIdle()
        vm.clearOptions();advanceUntilIdle()
        assertEquals("Only the persisted snapshot is cleared",1,repo.calls.size)
        assertEquals(listOf("奇幻"),vm.state.options.requiredTags)
        assertEquals(listOf("后宫"),vm.state.options.blockedTags)
    }
    @Test fun clearingTagSelectionPreservesAllNonTagConditions() = runTest(dispatcher) {
        val vm=model();vm.clearTags();advanceUntilIdle()
        assertEquals(chosen.copy(requiredTags=emptyList(),blockedTags=emptyList()).toSearchOptions(),vm.state.options)
        assertEquals(chosen.copy(requiredTags=emptyList(),blockedTags=emptyList()),store.load())
    }
    @Test fun enablingConditionRetentionSavesTheWholeActiveSnapshotForRestart() = runTest(dispatcher) {
        val vm=model();vm.toggleCache()
        assertEquals(chosen.toSearchOptions().copy(cacheEnabled=false),vm.state.options)
        vm.toggleCache()
        assertEquals(chosen,store.load())
        assertEquals(vm.state.options,model().state.options)
    }
    @Test fun explicitSaveWorksWhenCachingWasDisabledAndAfterClearingSavedSnapshot() = runTest(dispatcher) {
        val vm=model();vm.toggleCache();vm.saveConditions()
        assertEquals(chosen,store.load())
        vm.clearOptions();assertEquals(chosen.toSearchOptions(),vm.state.options)
        vm.saveConditions();assertEquals(chosen,store.load())
        assertEquals(vm.state.options,model().state.options)
    }
}
