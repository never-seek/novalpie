package com.novalpie.nativeapp.feature.forum

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.model.ForumPost
import com.novalpie.nativeapp.model.ForumPostPage
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.ui.ForumState
import com.novalpie.nativeapp.ui.GridScrollPosition
import com.novalpie.nativeapp.ui.apiFailureMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal data class ForumFeedQuery(val category:String,val keyword:String,val page:Int,val hideSpoilers:Boolean)
internal fun interface ForumFeedRepository {suspend fun fetch(query:ForumFeedQuery):ForumPostPage}
internal class WebsiteForumFeedRepository(private val api:NovalPieApi):ForumFeedRepository {
    override suspend fun fetch(query:ForumFeedQuery)=api.forumPostsPage(query.page,20,query.category,query.keyword,query.hideSpoilers)
}
internal data class ForumFeedUiState(val feed:ForumState=ForumState(),val scroll:GridScrollPosition=GridScrollPosition())

/** Each forum rail owns its query/page/scroll; details and other tabs never reset this state. */
internal class ForumFeedViewModel(private val repository:ForumFeedRepository,scope:CoroutineScope?=null):ViewModel() {
    private val work=scope ?: viewModelScope
    var state by mutableStateOf(ForumFeedUiState())
        private set
    private val saved=mutableMapOf<String,ForumFeedUiState>()
    private var generation=0L
    private var request:Job?=null
    private var visible=false
    fun enter(){visible=true;if(state.feed.posts is LoadResult.Idle)refresh()}
    fun leave(){visible=false}
    fun updateQuery(value:String) {
        if(value==state.feed.searchQuery)return
        invalidate()
        state=state.copy(feed=state.feed.copy(searchQuery=value,posts=LoadResult.Idle,page=1,totalPages=null,reviewTotal=null,canLoadMore=false,loadingMore=false,loadMoreError=null),scroll=GridScrollPosition())
        if(value.isBlank())refresh()
    }
    fun selectCategory(value:String) {
        val category=value.trim().ifBlank{"discussion"}
        if(category==state.feed.selectedType)return
        val hide=state.feed.hideSpoilers
        saved[state.feed.selectedType]=if(state.feed.posts is LoadResult.Loading)state.copy(feed=state.feed.copy(posts=LoadResult.Idle))else state
        invalidate()
        state=(saved[category] ?: ForumFeedUiState(ForumState(selectedType=category))).let{it.copy(feed=it.feed.copy(hideSpoilers=hide,loadingMore=false))}
        if(state.feed.posts is LoadResult.Idle)refresh()
    }
    fun setHideSpoilers(hide:Boolean) {
        if(state.feed.hideSpoilers==hide)return
        state=state.copy(feed=state.feed.copy(hideSpoilers=hide))
        saved.remove("review")
        if(state.feed.selectedType=="review")load(1)
    }
    fun saveScroll(index:Int,offset:Int){state=state.copy(scroll=GridScrollPosition.from(index,offset))}
    fun resetScroll(){state=state.copy(scroll=GridScrollPosition())}
    fun refresh()=load(state.feed.page)
    fun goToPage(page:Int) {
        val total=state.feed.totalPages ?: return
        val target=page.coerceIn(1,total.coerceAtLeast(1))
        if(target!=state.feed.page&&state.feed.posts !is LoadResult.Loading)load(target)
    }
    fun loadMore() {
        val current=state.feed
        current.totalPages?.let{if(current.page<it)goToPage(current.page+1);return}
        if(current.canLoadMore&&!current.loadingMore&&current.posts is LoadResult.Success)load(current.page+1,append=true)
    }
    private fun load(page:Int,append:Boolean=false) {
        invalidate()
        val serial=generation
        val before=state.feed
        val previous=(before.posts as? LoadResult.Success)?.value.orEmpty()
        val query=ForumFeedQuery(before.selectedType,before.searchQuery,page.coerceAtLeast(1),before.hideSpoilers)
        state=state.copy(feed=before.copy(posts=if(append)before.posts else LoadResult.Loading,page=if(append)before.page else query.page,
            loadingMore=append,loadMoreError=null),scroll=if(append)state.scroll else GridScrollPosition())
        request=work.launch {
            try {
                val result=repository.fetch(query)
                if(serial!=generation)return@launch
                state=state.copy(feed=state.feed.copy(posts=LoadResult.Success(if(append)(previous+result.posts).distinctBy(ForumPost::id)else result.posts),
                    page=result.page.coerceAtLeast(1),reviewTotal=if(query.category=="review")result.total else null,totalPages=result.totalPages,
                    canLoadMore=result.totalPages?.let{result.page<it} ?: (result.posts.size==20),loadingMore=false,loadMoreError=null))
            }catch(cancelled:CancellationException){if(serial==generation)state=state.copy(feed=state.feed.copy(posts=LoadResult.Idle,loadingMore=false));throw cancelled}
            catch(failure:Exception){if(serial==generation)state=state.copy(feed=state.feed.copy(
                posts=if(append)before.posts else LoadResult.Error(apiFailureMessage("论坛",failure)),loadingMore=false,
                loadMoreError=if(append)apiFailureMessage("论坛",failure)else null))}
        }
    }
    fun environmentChanged() {
        invalidate();saved.clear()
        state=ForumFeedUiState(ForumState(selectedType=state.feed.selectedType,searchQuery=state.feed.searchQuery,hideSpoilers=state.feed.hideSpoilers))
        if(visible)refresh()
    }
    private fun invalidate(){generation++;request?.cancel();request=null}
    fun close(){invalidate();work.cancel()}
}
