package com.novalpie.nativeapp.feature.forum

import com.novalpie.nativeapp.model.ForumPost
import com.novalpie.nativeapp.model.ForumPostPage
import com.novalpie.nativeapp.model.LoadResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForumFeedViewModelTest {
    private fun page(category:String,index:Int)=ForumPostPage(listOf(ForumPost(index.toLong(),category,"第$index 页")),total=60,page=index,totalPages=3)

    @Test fun categoriesKeepIndependentPageQueryAndScrollWhenReturning()=runTest {
        val queries=mutableListOf<ForumFeedQuery>()
        val model=ForumFeedViewModel(ForumFeedRepository{queries+=it;page(it.category,it.page)},backgroundScope)
        model.enter();runCurrent();model.goToPage(2);runCurrent();model.saveScroll(9,17)
        model.selectCategory("review");runCurrent();model.goToPage(3);runCurrent()
        model.selectCategory("discussion");runCurrent()
        assertEquals(2,model.state.feed.page)
        assertEquals(9,model.state.scroll.firstVisibleItemIndex)
        assertEquals(17,model.state.scroll.firstVisibleItemScrollOffset)
        assertEquals(4,queries.size)
    }

    @Test fun changingDraftWhileARequestIsRunningNeverLeavesPermanentLoading()=runTest {
        val pending=CompletableDeferred<ForumPostPage>()
        val model=ForumFeedViewModel(orumRepository(pending),backgroundScope)
        model.enter();runCurrent();model.updateQuery("新关键词")
        pending.complete(page("discussion",1));runCurrent()
        assertFalse(model.state.feed.posts is LoadResult.Loading)
        assertEquals("新关键词",model.state.feed.searchQuery)
    }
    private fun orumRepository(pending:CompletableDeferred<ForumPostPage>)=ForumFeedRepository{withContext(NonCancellable){pending.await()}}

    @Test fun failedPageRetainsTargetForRetryWithoutWrongFirstPageResponse()=runTest {
        var fail=true
        val requests=mutableListOf<Int>()
        val model=ForumFeedViewModel(ForumFeedRepository{requests+=it.page;if(it.page==2&&fail)error("offline")else page(it.category,it.page)},backgroundScope)
        model.enter();runCurrent();model.goToPage(2);runCurrent()
        assertEquals(2,model.state.feed.page)
        assertTrue(model.state.feed.posts is LoadResult.Error)
        fail=false;model.refresh();runCurrent()
        assertEquals(listOf(1,2,2),requests)
        assertEquals(2L,(model.state.feed.posts as LoadResult.Success).value.single().id)
    }

    @Test fun sessionChangeDropsCachedCategoriesAndLateResults()=runTest {
        val old=CompletableDeferred<ForumPostPage>()
        var calls=0
        val model=ForumFeedViewModel(ForumFeedRepository{if(++calls==1)withContext(NonCancellable){old.await()}else page(it.category,2)},backgroundScope)
        model.enter();runCurrent();model.environmentChanged();runCurrent()
        old.complete(page("discussion",1));runCurrent()
        assertEquals(2L,(model.state.feed.posts as LoadResult.Success).value.single().id)
    }
}
