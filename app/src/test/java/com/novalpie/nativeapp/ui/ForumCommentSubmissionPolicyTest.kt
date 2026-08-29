package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ForumActionResult
import com.novalpie.nativeapp.model.ForumComment
import com.novalpie.nativeapp.model.LoadResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForumCommentSubmissionPolicyTest {
    @Test
    fun successfulNestedReplyKeepsConfirmationWhileCommentsRefresh() {
        val root = ForumComment(
            id = 501,
            postId = 91,
            authorName = "reader",
            content = "parent",
        )
        val state = ForumPostDetailState(
            postId = 91,
            comments = LoadResult.Success(listOf(root)),
            commentDraft = "@reader reply",
            replyingToCommentId = 501,
            replyingToName = "reader",
            expandedCommentIds = setOf(501),
            actionLoading = true,
            commentClientRequestId = "request-501",
        )

        val next = forumPostDetailAfterCommentSubmission(
            state,
            Result.success(
                ForumActionResult(
                    success = true,
                    message = "回复已提交",
                    reply = ForumComment(
                        id = 502,
                        postId = 91,
                        parentCommentId = 501,
                        authorName = "seeking",
                        replyToName = "reader",
                        content = "child",
                    ),
                ),
            ),
        )

        assertEquals("回复已提交", next.actionMessage)
        assertFalse(next.actionLoading)
        assertEquals("", next.commentDraft)
        assertNull(next.replyingToCommentId)
        assertNull(next.replyingToName)
        assertNull(next.commentClientRequestId)
        assertTrue(501L in next.expandedCommentIds)
        assertEquals(
            listOf(501L, 502L),
            (next.comments as LoadResult.Success).value.map { it.id },
        )
    }

    @Test
    fun failedNestedReplyRetainsDraftAndTargetForRetry() {
        val state = ForumPostDetailState(
            postId = 91,
            commentDraft = "@reader retry",
            replyingToCommentId = 501,
            replyingToName = "reader",
            actionLoading = true,
            commentClientRequestId = "request-501",
        )

        val next = forumPostDetailAfterCommentSubmission(
            state,
            Result.failure(IllegalStateException("network")),
        )

        assertFalse(next.actionLoading)
        assertEquals("@reader retry", next.commentDraft)
        assertEquals(501L, next.replyingToCommentId)
        assertEquals("reader", next.replyingToName)
        assertEquals("request-501", next.commentClientRequestId)
        assertEquals("评论提交失败：network", next.actionMessage)
    }

    @Test
    fun serverRejectedNestedReplyRetainsDraftTargetAndRequestIdForRetry() {
        val state = ForumPostDetailState(
            postId = 91,
            commentDraft = "@reader retry after rejection",
            replyingToCommentId = 501,
            replyingToName = "reader",
            actionLoading = true,
            commentClientRequestId = "request-rejected-501",
        )

        val next = forumPostDetailAfterCommentSubmission(
            state,
            Result.success(
                ForumActionResult(
                    success = false,
                    message = "服务器暂时拒绝，请稍后重试",
                ),
            ),
        )

        assertFalse(next.actionLoading)
        assertEquals("@reader retry after rejection", next.commentDraft)
        assertEquals(501L, next.replyingToCommentId)
        assertEquals("reader", next.replyingToName)
        assertEquals("request-rejected-501", next.commentClientRequestId)
        assertEquals("服务器暂时拒绝，请稍后重试", next.actionMessage)
    }

    @Test
    fun successfulReplyForAnotherPostDoesNotClearTheCurrentPostRetryState() {
        val currentPost = ForumPostDetailState(
            postId = 92,
            commentDraft = "@reader keep this draft",
            replyingToCommentId = 502,
            replyingToName = "reader",
            actionLoading = true,
            commentClientRequestId = "request-current-post",
        )

        val staleResult = Result.success(
            ForumActionResult(
                success = true,
                message = "旧帖回复成功",
                reply = ForumComment(
                    id = 503,
                    postId = 91,
                    parentCommentId = 501,
                    authorName = "seeking",
                    content = "old post reply",
                ),
            ),
        )

        val next = forumPostDetailAfterCommentSubmission(currentPost, staleResult)

        assertEquals(
            "A completion from another post must never clear the current post's retry state.",
            currentPost,
            next,
        )
    }
}
