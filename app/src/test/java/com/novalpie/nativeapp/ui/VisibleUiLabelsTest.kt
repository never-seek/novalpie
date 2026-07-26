package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VisibleUiLabelsTest {
    @Test
    fun loadAreaLabelsUseReadableChinese() {
        val labels = listOf(
            VisibleUiLabels.ForumPostDetail,
            VisibleUiLabels.Comments,
            VisibleUiLabels.CommentSubmit,
            VisibleUiLabels.FavoriteGroups,
            VisibleUiLabels.Bookshelf,
            VisibleUiLabels.Search,
            VisibleUiLabels.BookDetail,
            VisibleUiLabels.ChapterCatalog,
            VisibleUiLabels.ChapterComments
        )

        assertEquals(
            listOf("帖子详情", "评论", "评论提交", "收藏分组", "书架", "搜索", "书籍详情", "章节目录", "章节评论"),
            labels
        )
        labels.forEach(::assertNoMojibake)
    }

    @Test
    fun forumActionFeedbackLabelsUseCleanWebsiteLanguage() {
        assertEquals("点赞", forumPostActionLabel(ForumPostAction.Like))
        assertEquals("点踩", forumPostActionLabel(ForumPostAction.Dislike))
        assertEquals("表情", forumPostActionLabel(ForumPostAction.Emoji))
        assertEquals("打赏", forumPostActionLabel(ForumPostAction.Award))

        assertEquals("评论点赞", forumCommentActionLabel(ForumPostAction.Like))
        assertEquals("评论点踩", forumCommentActionLabel(ForumPostAction.Dislike))
        assertEquals("评论表情", forumCommentActionLabel(ForumPostAction.Emoji))
        assertEquals("评论打赏", forumCommentActionLabel(ForumPostAction.Award))
    }

    private fun assertNoMojibake(value: String) {
        val fragments = listOf("甯", "璇", "鐐", "鎼", "涔", "鏀", "琛", "鎵", "绔")
        fragments.forEach { fragment ->
            assertFalse("Visible label contains mojibake '$fragment': $value", value.contains(fragment))
        }
    }
}
