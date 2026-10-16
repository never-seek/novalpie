package com.novalpie.nativeapp.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.novalpie.nativeapp.MainActivity
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Current production post/comment GETs only. Does not vote/react/send or capture private content. */
class ForumPostFeatureDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun postBookReferenceReturnAndReplyTargetRemainBoundToTheRightPost() {
        val arguments = androidx.test.platform.app.InstrumentationRegistry.getArguments()
        val postId = arguments.getString("forumPostId")?.toLongOrNull() ?: 1871L
        val otherPostId = arguments.getString("otherForumPostId")?.toLongOrNull() ?: 1934L
        require(postId > 0 && otherPostId > 0 && postId != otherPostId)
        val model = ViewModelProvider(compose.activity)[NovalPieViewModel::class.java]
        compose.runOnIdle { model.openForumPost(postId) }
        compose.waitUntil(45000) { model.forumPostDetailState.detail is LoadResult.Success && model.forumPostDetailState.comments is LoadResult.Success }
        assertEquals(postId, model.forumPostDetailState.postId)
        val comments = (model.forumPostDetailState.comments as LoadResult.Success).value
        assertTrue(comments.isNotEmpty())
        val target = comments.first()
        compose.runOnIdle { model.replyToForumComment(target); model.updateForumCommentDraft("Beta7未发送草稿") }
        compose.runOnIdle { model.openForumPost(otherPostId) }
        compose.waitUntil(45000) { model.forumPostDetailState.detail is LoadResult.Success && model.forumPostDetailState.postId == otherPostId }
        assertEquals("", model.forumPostDetailState.commentDraft)
        compose.runOnIdle { assertTrue(model.goBack()) }
        compose.waitUntil(5000) { model.forumPostDetailState.postId == postId }
        assertEquals("Beta7未发送草稿", model.forumPostDetailState.commentDraft)
        assertEquals(target.authorName, model.forumPostDetailState.replyingToName)
        compose.runOnIdle { model.cancelForumReply(); model.updateForumCommentDraft("") }
        assertEquals("", model.forumPostDetailState.commentDraft)
    }
}
