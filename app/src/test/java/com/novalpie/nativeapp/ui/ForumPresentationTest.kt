package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ForumComment
import com.novalpie.nativeapp.model.ForumPost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ForumPresentationTest {
    @Test
    fun forumPostFeedItemUsesWebsitePostDataWithoutDebugCopy() {
        val item = forumPostFeedItem(
            ForumPost(
                id = 91,
                category = "书评",
                title = "Native forum topic",
                authorName = "Forum User",
                bookTitle = "Linked Novel",
                replyCount = 7,
                likeCount = 81,
                reactionCount = 12,
                awardPoints = 7,
                viewCount = 7305,
                lastActiveLabel = "2026-07-07T10:00:00Z",
                tags = listOf("书评", "hot"),
                pinned = true,
                featured = true
            )
        )

        assertEquals("书评", item.category)
        assertEquals("Native forum topic", item.title)
        assertEquals("Linked Novel", item.bookTitle)
        assertEquals("Forum User", item.authorName)
        assertEquals(7, item.replyCount)
        assertEquals(81, item.likeCount)
        assertEquals(12, item.reactionCount)
        assertEquals(7, item.awardPoints)
        assertEquals(7305, item.viewCount)
        assertEquals(listOf("置顶", "精华", "书评", "hot"), forumFeedBadges(item))
    }

    @Test
    fun forumPostFeedItemUsesCleanFallbackCopy() {
        val item = forumPostFeedItem(
            ForumPost(
                id = 12,
                category = "动态",
                title = "站务提示",
                authorName = null,
                bookTitle = null,
                replyCount = null,
                lastActiveLabel = null,
                tags = emptyList(),
                pinned = false
            )
        )

        assertEquals("站内讨论", item.bookTitle)
        assertEquals("匿名用户", item.authorName)
        assertEquals("刚刚", item.lastActiveLabel)
        assertEquals(listOf("动态"), forumFeedBadges(item))
    }

    @Test
    fun forumFeedBadgesStayCompactAndPrioritizePinnedThenCategory() {
        val item = ForumFeedItem(
            category = "书评",
            title = "角色弧光讨论",
            bookTitle = "热门作品",
            authorName = "北港读者",
            replyCount = 42,
            lastActiveLabel = "刚刚",
            tags = listOf("热议", "长评", "补充"),
            pinned = true,
            featured = true
        )

        assertEquals(listOf("置顶", "精华", "书评", "热议", "长评"), forumFeedBadges(item))
    }

    @Test
    fun forumFeedBadgesSkipPinnedWhenTopicIsNormal() {
        val item = ForumFeedItem(
            category = "章节",
            title = "最新章节伏笔整理",
            bookTitle = "连载专区",
            authorName = "栗子校对",
            replyCount = 28,
            lastActiveLabel = "8分钟前",
            tags = listOf("剧情", "伏笔"),
            pinned = false
        )

        assertEquals(listOf("章节", "剧情", "伏笔"), forumFeedBadges(item))
    }

    @Test
    fun forumFeedMetaIsOneLineAndDoesNotExposeDebugCopy() {
        val item = ForumFeedItem(
            category = "动态",
            title = "作者更新说明",
            bookTitle = "站内公告",
            authorName = "运营记录",
            replyCount = 17,
            lastActiveLabel = "23分钟前",
            tags = listOf("公告")
        )

        val meta = forumFeedMetaLine(item)

        assertEquals("运营记录 · 站内公告 · 23分钟前", meta)
        assertFalse(meta.contains("API", ignoreCase = true))
        assertFalse(meta.contains("fallback", ignoreCase = true))
    }

    @Test
    fun forumFeedMetricLabelsMirrorWebsitePostFooter() {
        val item = ForumFeedItem(
            category = "公告",
            title = "翻译功能已恢复",
            bookTitle = "站内公告",
            authorName = "诺亚方舟",
            replyCount = 80,
            likeCount = 81,
            reactionCount = 12,
            awardPoints = 7,
            viewCount = 7305,
            lastActiveLabel = "2026/6/2",
            tags = listOf("公告")
        )

        assertEquals(
            listOf("80 条回复", "赞 81", "表情 12", "打赏 7", "7305 次浏览"),
            forumFeedMetricLabels(item)
        )
    }

    @Test
    fun forumActionBarUsesForumClientLabelsInsteadOfChipDump() {
        assertEquals(listOf("赞", "踩", "表情", "打赏", "网页"), forumActionBarLabels())
    }

    @Test
    fun forumContentLinksAreExtractedForPreviewRows() {
        val links = forumContentLinks(
            listOf(
                "正文第一段",
                "https://novalpie.cc/book/354491?from=forum",
                "也可以看看 https://example.test/very/long/path?query=1#anchor。"
            )
        )

        assertEquals(
            listOf(
                "https://novalpie.cc/book/354491?from=forum",
                "https://example.test/very/long/path?query=1#anchor"
            ),
            links
        )
    }

    @Test
    fun forumCommentLinkPreviewsUseParsedCommentParagraphs() {
        val previews = forumCommentLinkPreviews(
            ForumComment(
                id = 501,
                content = "<p>我看的是 https://novalpie.cc/book/354491?from=comment</p><p>另一个很长 https://example.test/a/b/c?query=long#anchor。</p>"
            )
        )

        assertEquals(
            listOf(
                "https://novalpie.cc/book/354491?from=comment",
                "https://example.test/a/b/c?query=long#anchor"
            ),
            previews
        )
    }

    @Test
    fun forumCommentThreadsGroupRepliesWithoutDroppingOrphans() {
        val threads = forumCommentThreads(
            listOf(
                ForumComment(id = 1, authorName = "主楼", content = "第一条"),
                ForumComment(id = 2, parentCommentId = 1, authorName = "回复 A", replyToName = "主楼", content = "回复主楼"),
                ForumComment(id = 3, parentCommentId = 99, authorName = "孤立回复", replyToName = "失效用户", content = "父评论不在当前页"),
                ForumComment(id = 4, authorName = "第二主楼", content = "第二条"),
                ForumComment(id = 5, parentCommentId = 1, authorName = "回复 B", replyToName = "主楼", content = "继续回复")
            )
        )

        assertEquals(listOf(1L, 3L, 4L), threads.map { it.comment.id })
        assertEquals(listOf(2L, 5L), threads.first().replies.map { it.id })
        assertEquals(emptyList<ForumComment>(), threads[1].replies)
        assertEquals("3 条评论 · 2 条回复", forumCommentThreadSummary(threads))
    }

    @Test
    fun forumPresentationUsesReadableChineseLabels() {
        val item = forumPostFeedItem(
            ForumPost(
                id = 91,
                category = "讨论",
                title = "翻译功能已恢复",
                authorName = null,
                bookTitle = null,
                replyCount = 80,
                likeCount = 81,
                reactionCount = 12,
                awardPoints = 7,
                viewCount = 7305,
                lastActiveLabel = null,
                pinned = true,
                featured = true
            )
        )

        assertEquals("匿名用户", item.authorName)
        assertEquals("站内讨论", item.bookTitle)
        assertEquals("刚刚", item.lastActiveLabel)
        assertEquals(listOf("置顶", "精华", "讨论"), forumFeedBadges(item))
        assertEquals(
            listOf("80 条回复", "赞 81", "表情 12", "打赏 7", "7305 次浏览"),
            forumFeedMetricLabels(item)
        )
        assertEquals(listOf("赞", "踩", "表情", "打赏", "网页"), forumActionBarLabels())
        assertEquals(
            "3 条评论 · 2 条回复",
            forumCommentThreadSummary(
                listOf(
                    ForumCommentThread(ForumComment(id = 1, content = "root"), listOf(ForumComment(id = 2, content = "reply"))),
                    ForumCommentThread(ForumComment(id = 3, content = "root"), listOf(ForumComment(id = 4, content = "reply"))),
                    ForumCommentThread(ForumComment(id = 5, content = "root"))
                )
            )
        )
    }
}
