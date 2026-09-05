package com.novalpie.nativeapp.feature.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.NovelCard
import com.novalpie.nativeapp.model.NovelTag
import com.novalpie.nativeapp.model.SearchPage
import com.novalpie.nativeapp.ui.BookDetailSearchTarget
import com.novalpie.nativeapp.ui.GridScrollPosition
import com.novalpie.nativeapp.ui.SearchOptions
import com.novalpie.nativeapp.ui.SearchTagFilterMode
import com.novalpie.nativeapp.ui.SearchViewMode
import com.novalpie.nativeapp.ui.apiFailureMessage
import com.novalpie.nativeapp.ui.bookDetailSearchOptions
import com.novalpie.nativeapp.ui.normalizeSearchTagList
import com.novalpie.nativeapp.ui.removeSearchTagFilter
import com.novalpie.nativeapp.ui.resolveSearchRequest
import com.novalpie.nativeapp.ui.searchKeywordForSubmission
import com.novalpie.nativeapp.ui.toggleSearchTagFilters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal data class SearchUiState(
    val keyword: String = "",
    val history: List<String> = emptyList(),
    val options: SearchOptions = SearchOptions(),
    val results: LoadResult<List<NovelCard>> = LoadResult.Idle,
    val tags: LoadResult<List<NovelTag>> = LoadResult.Idle,
    val page: Int = 1,
    val envelope: SearchPage? = null,
    val canLoadMore: Boolean = false,
    val loadingPage: Boolean = false,
    val pageError: String? = null,
    val scroll: GridScrollPosition = GridScrollPosition(),
)

/** Route-scoped search state. Unrelated app screens cannot write into its request/session state. */
internal class SearchViewModel(
    private val repository: SearchRepository,
    private val preferences: SearchPreferences,
) : ViewModel() {
    var state by mutableStateOf(SearchUiState(history=preferences.history(), options=preferences.settings().toSearchOptions()))
        private set
    private var generation = 0L
    private var tagGeneration = 0L
    private var requestJob: Job? = null
    private var tagJob: Job? = null
    private var visible = false

    fun setVisible(value: Boolean) { visible = value }
    fun enter() {
        visible = true
        if (state.tags is LoadResult.Idle) loadTags()
        if (state.results is LoadResult.Idle) submit()
    }
    fun leave() { visible = false }
    fun updateKeyword(keyword: String) {
        if (state.keyword == keyword) return
        invalidate()
        state = state.copy(keyword=keyword,results=LoadResult.Idle,page=1,envelope=null,scroll=GridScrollPosition(),pageError=null)
        if (visible && keyword.isBlank()) submit("")
    }
    fun updateOptions(next: SearchOptions, refresh: Boolean = visible) {
        if (next == state.options) return
        val queryChanged = resolveSearchRequest(state.keyword,state.options) != resolveSearchRequest(state.keyword,next)
        state = state.copy(options=next)
        preferences.save(next.persisted())
        if (queryChanged) {
            invalidate()
            state=state.copy(results=LoadResult.Idle,page=1,envelope=null,pageError=null,scroll=GridScrollPosition())
            if (refresh) submit()
        }
    }
    fun toggleViewMode() = updateOptions(state.options.copy(
        viewMode=if(state.options.viewMode==SearchViewMode.Grid) SearchViewMode.List else SearchViewMode.Grid,
    )).also { saveScroll(0,0) }
    fun toggleCache() = updateOptions(state.options.copy(cacheEnabled=!state.options.cacheEnabled))
    fun clearOptions() {
        invalidate()
        preferences.clearOptions()
        state=state.copy(options=SearchOptions(cacheEnabled=state.options.cacheEnabled),results=LoadResult.Idle,page=1,envelope=null,pageError=null,scroll=GridScrollPosition())
        if (visible) submit()
    }
    fun clearHistory() { preferences.clearHistory(); state=state.copy(history=emptyList()) }
    fun openTarget(target: BookDetailSearchTarget) {
        invalidate()
        val options=bookDetailSearchOptions(state.options,target)
        preferences.save(options.persisted())
        state=state.copy(keyword=target.keyword,options=options,scroll=GridScrollPosition())
        visible=true
        submit(target.keyword)
    }
    fun tag(name: String, mode: SearchTagFilterMode) {
        val (required,blocked)=toggleSearchTagFilters(state.options.requiredTags,state.options.blockedTags,name,mode)
        setTags(required,blocked)
    }
    fun removeTag(name: String) {
        val (required,blocked)=removeSearchTagFilter(state.options.requiredTags,state.options.blockedTags,name)
        setTags(required,blocked)
    }
    fun clearTags() = setTags(emptyList(),emptyList())
    private fun setTags(required: List<String>, blocked: List<String>) = updateOptions(
        state.options.copy(requiredTags=normalizeSearchTagList(required),blockedTags=normalizeSearchTagList(blocked)),refresh=true,
    )
    fun saveScroll(index: Int, offset: Int) {
        val next=GridScrollPosition.from(index,offset)
        if(next!=state.scroll) state=state.copy(scroll=next)
    }
    fun submit(submittedKeyword: String? = null) {
        val keyword=searchKeywordForSubmission(state.keyword,submittedKeyword)
        state=state.copy(keyword=keyword)
        if(keyword.isNotBlank()) { preferences.remember(keyword); state=state.copy(history=preferences.history()) }
        requestPage(1)
    }
    fun goToPage(requested: Int) {
        if(state.loadingPage) return
        val envelope=state.envelope ?: return
        val totalPages=envelope.totalPages ?: envelope.total?.let{(it.toLong()+envelope.pageSize.coerceAtLeast(1)-1)/envelope.pageSize.coerceAtLeast(1)}?.toInt()
        val target=totalPages?.let{requested.coerceIn(1,it.coerceAtLeast(1))} ?: requested.coerceAtLeast(1)
        if(target==state.page && state.results is LoadResult.Success) return
        requestPage(target)
    }
    fun retry() = requestPage(state.page)
    private fun requestPage(page: Int) {
        invalidate()
        val id=generation
        val resolved=resolveSearchRequest(state.keyword,state.options)
        state=state.copy(results=LoadResult.Loading,page=page,envelope=state.envelope?.copy(page=page),loadingPage=true,pageError=null,scroll=GridScrollPosition())
        if(resolved.errors.isNotEmpty()) {
            state=state.copy(results=LoadResult.Error("高级语法：${resolved.errors.joinToString("；")}"),loadingPage=false)
            return
        }
        requestJob=viewModelScope.launch {
            try {
                val response=repository.page(resolved,page)
                // Presentation-only switches never invalidate the actual request snapshot.
                if(id!=generation || resolved!=resolveSearchRequest(state.keyword,state.options)) return@launch
                val canLoadMore=response.totalPages?.let{response.page<it}
                    ?: response.total?.let{response.page.toLong()*response.pageSize<it}
                    ?: (response.items.size>=response.pageSize)
                state=state.copy(results=LoadResult.Success(response.items),page=response.page,envelope=response,canLoadMore=canLoadMore,loadingPage=false,pageError=null)
            } catch(cancelled: CancellationException) { throw cancelled }
            catch(failure: Exception) {
                if(id!=generation) return@launch
                val message=apiFailureMessage("搜索",failure)
                state=state.copy(results=LoadResult.Error(message),loadingPage=false,pageError=message,canLoadMore=false)
            }
        }
    }
    fun loadTags() {
        if(state.tags is LoadResult.Loading) return
        val id=++tagGeneration
        state=state.copy(tags=LoadResult.Loading)
        tagJob=viewModelScope.launch {
            try {
                val tags=repository.tags()
                if(id==tagGeneration) state=state.copy(tags=LoadResult.Success(tags))
            } catch(cancelled: CancellationException) { throw cancelled }
            catch(failure: Exception) {
                if(id==tagGeneration) state=state.copy(tags=LoadResult.Error(apiFailureMessage("标签",failure)))
            }
        }
    }
    fun environmentChanged() {
        invalidate()
        tagGeneration++
        tagJob?.cancel()
        state=state.copy(results=LoadResult.Idle,tags=LoadResult.Idle,envelope=null,page=1,pageError=null,scroll=GridScrollPosition())
        if(visible) enter()
    }
    private fun invalidate() {
        generation++
        requestJob?.cancel()
        requestJob=null
        state=state.copy(canLoadMore=false,loadingPage=false)
    }
    fun close() { invalidate(); tagGeneration++; tagJob?.cancel(); viewModelScope.cancel() }
}
