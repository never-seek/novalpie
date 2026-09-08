package com.novalpie.nativeapp.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.MainActivity
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

/** Explicitly opt-in, one clearly labeled reply to the signed-in user's own existing nested reply. */
class ForumNestedReplyLiveDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun nativeNestedReplyIsPostedOnceAndVisibleAfterSourceRefresh() = runBlocking {
        require(InstrumentationRegistry.getArguments().getString("allowForumWrite") == "true")
        val context = compose.activity
        val api = AppContainer.from(context).api
        val account = api.currentUser()
        val model = ViewModelProvider(compose.activity)[NovalPieViewModel::class.java]
        compose.runOnIdle { model.openForumPost(1871) }
        compose.waitUntil(45000) { model.forumPostDetailState.comments is LoadResult.Success && model.forumPostDetailState.detail is LoadResult.Success }
        val post = (model.forumPostDetailState.detail as LoadResult.Success).value.post
        assertEquals("Only the user's own App post is in scope", account.id, post.authorId)
        val target = (model.forumPostDetailState.comments as LoadResult.Success).value.firstOrNull { it.authorId == account.id && it.parentCommentId != null }
            ?: error("No existing own nested reply found; nothing sent")
        val marker = "【Beta7原生嵌套回复验收-${UUID.randomUUID().toString().take(8)}】仅验证回复的回复，请忽略。"
        val folder = File(context.getExternalFilesDir(null), "beta7-qa").apply { mkdirs() }
        File(folder, "forum-reply-pending.json").writeText(JSONObject().put("postId", 1871).put("targetId", target.id).put("marker", marker).toString())
        compose.runOnIdle { model.replyToForumComment(target); model.updateForumCommentDraft(marker); model.submitForumComment() }
        compose.waitUntil(45000) { !model.forumPostDetailState.actionLoading }
        assertEquals("发送成功才应清草稿", "", model.forumPostDetailState.commentDraft)
        val refreshed = api.forumPostComments(1871)
        val echoes = refreshed.filter { it.content.contains(marker) }
        assertEquals("同一测试只能发送一次", 1, echoes.size)
        val echo = echoes.single()
        assertNotNull(echo.parentCommentId)
        assertEquals(account.id, echo.authorId)
        File(folder, "forum-reply-live.json").writeText(JSONObject().put("postId", 1871).put("targetReplyId", target.id)
            .put("createdReplyId", echo.id).put("rootCommentId", echo.parentCommentId).put("marker", marker).put("passed", true).toString(2))
        File(folder, "forum-reply-pending.json").delete()
        Unit
    }
}
