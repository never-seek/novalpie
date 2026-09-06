package com.novalpie.nativeapp.feature.books

import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookDetailViewModelTest {
    private open class Repository:BookDetailRepository {
        override suspend fun book(id:Long)=NovelCard(id,"书$id")
        override suspend fun cover(id:Long):String?=null
        override suspend fun chapters(id:Long)=listOf(Chapter(id*10,"第一章",1))
        override suspend fun comments(id:Long)=emptyList<ChapterComment>()
        override suspend fun favorite(id:Long)=FavoriteStatus(false)
        override suspend fun permissions(id:Long)=BookEditPermissions()
    }
    @Test fun aSlowCommentListDoesNotBlockBookAndCatalogAndOldCoverCannotLeak()=runTest {
        val comments=CompletableDeferred<List<ChapterComment>>()
        val oldCover=CompletableDeferred<String?>()
        val repository=object:Repository(){
            override suspend fun comments(id:Long)=if(id==1L)withContext(NonCancellable){comments.await()}else emptyList()
            override suspend fun cover(id:Long)=if(id==1L)withContext(NonCancellable){oldCover.await()}else "https://book.test/two.png"
        }
        val model=BookDetailViewModel(repository,backgroundScope)
        model.load(1,true);runCurrent()
        assertTrue(model.state.book is LoadResult.Success)
        assertTrue(model.state.chapters is LoadResult.Success)
        assertTrue(model.state.comments is LoadResult.Loading)
        model.load(2,true);runCurrent()
        comments.complete(emptyList());oldCover.complete("https://book.test/one.png");runCurrent()
        val book=(model.state.book as LoadResult.Success).value
        assertEquals(2L,book.id)
        assertEquals("https://book.test/two.png",book.fullCoverUrl)
    }
    @Test fun failedReviewRefreshPreservesTheCurrentDraftAndReplyTarget()=runTest {
        var fail=false
        val repository=object:Repository(){override suspend fun comments(id:Long):List<ChapterComment>{if(fail)error("offline");return emptyList()}}
        val model=BookDetailViewModel(repository,backgroundScope)
        model.load(1,true);runCurrent()
        model.present{it.copy(commentDraft="保留的回复",replyingToCommentId=22,replyingToName="读者")}
        fail=true;model.load(1,true,retainComposer=true);runCurrent()
        assertEquals("保留的回复",model.state.commentDraft)
        assertEquals(22L,model.state.replyingToCommentId)
        assertTrue(model.state.comments is LoadResult.Error)
    }
    @Test fun guestDoesNotQueryManagementPermissionsOrReusePriorPrivilegedState()=runTest {
        var queried=0
        val model=BookDetailViewModel(object:Repository(){override suspend fun permissions(id:Long):BookEditPermissions{queried++;return BookEditPermissions(title=true)}},backgroundScope)
        model.load(1,true);runCurrent();model.environmentChanged();model.load(2,false);runCurrent()
        assertEquals(1,queried)
        assertFalse((model.state.managementPermissions as LoadResult.Success).value.title)
    }
}
