package com.novalpie.nativeapp.feature.forum

import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.model.*

internal enum class ForumReaction(val value: String, val label: String) {
    Like("up", "点赞"), Dislike("down", "点踩"), Joy("emoji:heart", "表情"), Award("award", "打赏")
}
internal data class ForumCommentDraft(val content: String, val parentId: Long?, val replyName: String?, val requestId: String)
internal data class ForumCommentsPage(val items: List<ForumComment>, val page: Int, val hasMore: Boolean)
internal interface ForumPostRepository {
    suspend fun detail(id: Long): ForumPostDetail
    suspend fun comments(id: Long): List<ForumComment>
    suspend fun commentPage(id: Long, page: Int): ForumCommentsPage = ForumCommentsPage(comments(id), page, false)
    suspend fun book(id: Long): NovelCard
    suspend fun send(id: Long, draft: ForumCommentDraft): ForumActionResult
    suspend fun vote(id: Long, options: List<Long>): ForumActionResult
    suspend fun react(id: Long, reaction: ForumReaction, parentId: Long? = null, replyId: Long? = null): ForumActionResult
}
internal class WebsiteForumPostRepository(private val api: NovalPieApi) : ForumPostRepository {
    override suspend fun detail(id: Long) = api.forumPostDetail(id)
    override suspend fun comments(id: Long) = api.forumPostComments(id)
    override suspend fun commentPage(id: Long, page: Int) = api.forumPostCommentPage(id, page)
    override suspend fun book(id: Long) = api.bookDetail(id)
    override suspend fun send(id: Long, draft: ForumCommentDraft) = api.createForumComment(id, draft.content, draft.parentId, draft.replyName, draft.requestId)
    override suspend fun vote(id: Long, options: List<Long>) = api.submitForumPoll(id, options)
    override suspend fun react(id: Long, reaction: ForumReaction, parentId: Long?, replyId: Long?): ForumActionResult = when {
        parentId == null && reaction == ForumReaction.Like -> api.toggleForumPostLike(id)
        parentId == null -> api.reactToForumPost(id, reaction.value, if (reaction == ForumReaction.Award) 10 else null)
        reaction == ForumReaction.Like -> api.toggleForumCommentLike(id, parentId, replyId)
        else -> api.reactToForumComment(id, parentId, replyId, reaction.value, if (reaction == ForumReaction.Award) 10 else null)
    }
}
