package com.novalpie.nativeapp.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.data.SearchHistoryStore
import com.novalpie.nativeapp.data.SearchSettingsStore
import com.novalpie.nativeapp.model.LoadResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchSessionStateTest {
    private lateinit var application: Application
    private lateinit var history: SearchHistoryStore
    private lateinit var settings: SearchSettingsStore

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        history = SearchHistoryStore(application)
        settings = SearchSettingsStore(application)
        history.clear()
        settings.clear()
    }

    @After
    fun tearDown() {
        history.clear()
        settings.clear()
    }

    @Test
    fun searchHistoryDoesNotPrepopulateANewSearchField() {
        history.saveKeyword("tag:奇幻 NOT tag:后宫")

        val viewModel = NovalPieViewModel(application)

        assertEquals("", viewModel.searchKeyword)
        assertEquals(listOf("tag:奇幻 NOT tag:后宫"), viewModel.searchHistory)
    }

    @Test
    fun editingTheFieldInvalidatesPriorLocalSearchState() {
        val viewModel = NovalPieViewModel(application)
        viewModel.updateSearchAdvancedSyntaxEnabled(true)
        viewModel.updateSearchKeyword("NOT")
        viewModel.performSearch()
        assertTrue(viewModel.searchResults is LoadResult.Error)

        viewModel.updateSearchKeyword("新的关键词")

        assertTrue(viewModel.searchResults is LoadResult.Idle)
        assertEquals(false, viewModel.searchCanLoadMore)
        assertEquals(false, viewModel.searchLoadingMore)
        assertEquals(null, viewModel.searchLoadMoreError)
    }

    @Test
    fun searchViewModePersistsWithoutInvalidatingTheSearchSession() {
        val viewModel = NovalPieViewModel(application)

        assertEquals(SearchViewMode.Grid, viewModel.searchOptions.viewMode)
        viewModel.toggleSearchViewMode()

        assertEquals(SearchViewMode.List, viewModel.searchOptions.viewMode)
        assertEquals(SearchViewMode.List, NovalPieViewModel(application).searchOptions.viewMode)
    }
}
