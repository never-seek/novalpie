package com.novalpie.nativeapp.feature.forum

import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ForumPostViewModelTest {
    private open class Repository : ForumPostRepository {
        val sends = mutableListOf<Pair<Long, ForumCommentDraft>>()
        override suspend fun detail(id: Long) = ForumPostDetail(ForumPost(id = id, category = "discussion", title = "帖子$id"))
        override suspend fun comments(id: Long) = listOf(ForumComment(10, id, authorName = "甲", content = "父评论"))
        override suspend fun book(id: Long) = NovelCard(id, "书$id")
        override suspend fun send(id: Long, draft: ForumCommentDraft): ForumActionResult { sends += id to draft; return ForumActionResult(true) }
        override suspend fun vote(id: Long, options: List<Long>) = ForumActionResult(true)
        override suspend fun react(id: Long, reaction: ForumReaction, parentId: Long?, replyId: Long?) = ForumActionResult(true)
    }
    @Test fun replyTargetChangedWhileSendingKeepsNewDraftAndAlwaysReleasesBusyState() = runBlocking {
        val answer = CompletableDeferred<ForumActionResult>()
        val repo = object : Repository() { override suspend fun send(id: Long, draft: ForumCommentDraft): ForumActionResult {
            sends += id to draft; return answer.await()
        } }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = ForumPostViewModel(repo, scope)
        try {
            model.load(1); model.reply(ForumComment(10, 1, authorName = "甲", content = "父")); model.draft("给甲的回复"); model.send()
            assertTrue(model.state.actionLoading)
            model.reply(ForumComment(20, 1, authorName = "乙", content = "另一父")); model.draft("给乙的新草稿")
            answer.complete(ForumActionResult(true, reply = ForumComment(11, 1, parentCommentId = 10, content = "给甲的回复")))
            assertFalse(model.state.actionLoading)
            assertEquals("给乙的新草稿", model.state.commentDraft)
            assertEquals("乙", model.state.replyingToName)
            assertEquals(10L, repo.sends.single().second.parentId)
            assertEquals("给甲的回复", repo.sends.single().second.content)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun latePostResponseAndWriteCannotChangeAnotherPostsIdentityOrDraft() = runBlocking {
        val answer = CompletableDeferred<ForumActionResult>()
        val repo = object : Repository() { override suspend fun send(id: Long, draft: ForumCommentDraft): ForumActionResult {
            sends += id to draft; return answer.await()
        } }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = ForumPostViewModel(repo, scope)
        try {
            model.load(1); model.draft("旧帖回复"); model.send()
            model.load(2); model.draft("新帖草稿")
            answer.complete(ForumActionResult(true))
            assertEquals(2L, model.state.postId); assertEquals("新帖草稿", model.state.commentDraft)
            model.load(1)
            assertFalse(model.state.actionLoading)
            assertEquals("", model.state.commentDraft)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun commentsNetworkDelayNeverBlocksPostBodyAndRetryKeepsTheFailedDraftId() = runBlocking {
        val comments = CompletableDeferred<List<ForumComment>>()
        val repo = object : Repository() {
            override suspend fun comments(id: Long) = comments.await()
            override suspend fun send(id: Long, draft: ForumCommentDraft): ForumActionResult {
                sends += id to draft; throw java.io.IOException("fixture network")
            }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = ForumPostViewModel(repo, scope)
        try {
            model.load(1)
            assertTrue(model.state.detail is LoadResult.Success)
            assertEquals(LoadResult.Loading, model.state.comments)
            model.draft("保留这条"); model.send()
            assertFalse(model.state.actionLoading)
            assertEquals("保留这条", model.state.commentDraft)
            model.send()
            assertEquals(repo.sends[0].second.requestId, repo.sends[1].second.requestId)
            model.environmentChanged()
            comments.complete(emptyList())
            assertEquals(0L, model.state.postId)
            assertEquals("", model.state.commentDraft)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun paginationRetainsAllPagesAndFailureRetriesSamePageWithoutSkipping() = runBlocking {
        var failSecond = true
        val pages = mutableListOf<Int>()
        val repo = object : Repository() {
            override suspend fun commentPage(id: Long, page: Int): ForumCommentsPage {
                pages += page
                if (page == 2 && failSecond) error("offline")
                return ForumCommentsPage(listOf(ForumComment(page.toLong(), id, content = "第$page 页")), page, page < 2)
            }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = ForumPostViewModel(repo, scope)
        try {
            model.load(1); assertTrue(model.state.commentsHasMore)
            model.loadMore(); assertEquals(1, model.state.commentsPage); assertFalse(model.state.commentsLoadingMore)
            assertEquals(listOf(1L), (model.state.comments as LoadResult.Success).value.map { it.id })
            failSecond = false; model.loadMore()
            assertEquals(2, model.state.commentsPage); assertFalse(model.state.commentsHasMore)
            assertEquals(listOf(1L, 2L), (model.state.comments as LoadResult.Success).value.map { it.id })
            model.retryComments()
            assertEquals(setOf(1L, 2L), (model.state.comments as LoadResult.Success).value.map { it.id }.toSet())
            assertEquals(listOf(1, 2, 2, 1, 2), pages)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun confirmedSendEchoSurvivesAStaleCommentsReadButDisappearsAfterAcknowledgedDeletion() = runBlocking {
        val echo = ForumComment(11, 1, parentCommentId = 10, content = "已发送")
        var sourceHasReply = false
        val repo = object : Repository() {
            override suspend fun comments(id: Long) = super.comments(id) + if (sourceHasReply) listOf(echo) else emptyList()
            override suspend fun send(id: Long, draft: ForumCommentDraft) = ForumActionResult(true, reply = echo)
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = ForumPostViewModel(repo, scope)
        try {
            model.load(1); model.draft("已发送"); model.send()
            assertTrue((model.state.comments as LoadResult.Success).value.any { it.id == 11L })
            sourceHasReply = true; model.retryComments()
            assertEquals(1, (model.state.comments as LoadResult.Success).value.count { it.id == 11L })
            sourceHasReply = false; model.retryComments()
            assertFalse((model.state.comments as LoadResult.Success).value.any { it.id == 11L })
        } finally { model.close(); scope.cancel() }
    }
}
