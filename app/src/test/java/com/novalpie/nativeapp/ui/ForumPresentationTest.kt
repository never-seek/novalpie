package com.novalpie.nativeapp.ui

import androidx.compose.ui.text.buildAnnotatedString
import com.novalpie.nativeapp.model.ForumComment
import com.novalpie.nativeapp.model.ForumPost
import com.novalpie.nativeapp.model.UserBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ForumPresentationTest {
    @Test
    fun trailingSpoilerAnnotationIsStillHitWhenTextLayoutReturnsEndOffset() {
        val annotated = buildAnnotatedString {
            append("前文 ")
            pushStringAnnotation("forum_spoiler", "0")
            append("否決蘿莉")
            pop()
        }

        assertEquals(
            "0",
            forumStringAnnotationAtOffset(
                text = annotated,
                tag = "forum_spoiler",
                offset = annotated.length,
            )
        )
    }

    @Test
    fun forumCategorySlotsFillOneEqualFiveWayAxis() {
        val slots = forumCategorySlots(forumFeedCategories())

        assertEquals(5, slots.size)
        assertEquals(listOf(1f, 1f, 1f, 1f, 1f), slots.map(ForumCategorySlot::weight))
    }

    @Test
    fun sourceBookReviewFeedUsesOneColumnOnPhoneAndTwoOnTablet() {
        assertEquals(1, forumGridColumnCount("review"))
        assertEquals(SOURCE_FORUM_REVIEW_GRID_COLUMNS, forumGridColumnCount("review", 900))
        assertEquals(1, forumGridColumnCount("discussion"))
        assertEquals(1, forumGridColumnCount("feedback"))
    }

    @Test
    fun singleColumnForumUsesTheListRendererInsteadOfGridSubcomposition() {
        assertTrue(forumUsesListLayout("review", 412))
        assertTrue(forumUsesListLayout("discussion", 900))
        assertFalse(forumUsesListLayout("review", 900))
    }

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
        assertEquals(listOf("置顶", "精华", "hot"), forumFeedBadges(item))
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
        assertEquals(emptyList<String>(), forumFeedBadges(item))
    }

    @Test
    fun bookReviewFeedItemsOpenTheirLinkedBookInsteadOfAForumCommentId() {
        val review = forumPostFeedItem(
            ForumPost(
                id = 57028,
                category = "review",
                title = "Review title",
                bookId = 354491,
                bookTitle = "Linked Book",
                bookCoverUrl = "https://images.novelpia.com/imagebox/cover/linked.file",
                isBookReview = true,
                authorName = "Review User"
            )
        )

        assertTrue(review.isBookReview)
        assertEquals(354491L, review.bookId)
        assertEquals(ForumFeedDestination.Book(354491L), forumFeedDestination(review))
    }

    @Test
    fun normalPostWithALinkedBookStillOpensItsForumDetail() {
        val post = forumPostFeedItem(
            ForumPost(
                id = 91,
                category = "discussion",
                title = "Forum topic",
                bookId = 354491,
                bookTitle = "Linked Book"
            )
        )

        assertEquals(ForumFeedDestination.Post(91L), forumFeedDestination(post))
    }

    @Test
    fun malformedBookReviewWithoutABookIdDoesNotOpenAForumCommentRoute() {
        val review = forumPostFeedItem(
            ForumPost(
                id = 57028,
                category = "review",
                title = "Broken review",
                isBookReview = true
            )
        )

        assertEquals(ForumFeedDestination.None, forumFeedDestination(review))
    }

    @Test
    fun bookReviewCardUsesSourceStyleBookLabelAndDoesNotRepeatTheReviewTag() {
        val review = ForumFeedItem(
            id = 57028,
            category = "书评",
            title = "Review title",
            bookId = 354491,
            bookTitle = "Linked Book",
            isBookReview = true,
            authorName = "Review User",
            replyCount = 0,
            lastActiveLabel = "刚刚",
            tags = listOf("review", "Linked Book", "长评", "推荐")
        )

        assertEquals("书评 · 《Linked Book》", forumFeedTitle(review))
        assertEquals(emptyList<String>(), forumFeedTags(review))
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

        assertEquals(listOf("置顶", "精华", "热议", "长评", "补充"), forumFeedBadges(item))
    }

    @Test
    fun forumVisualBadgesKeepSourceArtworkAndOnlyAppendMissingTextFallbacks() {
        val visual = UserBadge(
            id = 12,
            name = "Aurora",
            badgeHtml = "<span class='badge'>Aurora</span>",
            badgeCss = "background: linear-gradient(135deg, #22d3ee, #a855f7);",
        )

        val resolved = forumAuthorBadgeVisuals(
            visuals = listOf(visual),
            labels = listOf("Aurora", "Reader", "Reader"),
        )

        assertEquals(listOf("Aurora", "Reader"), resolved.map(UserBadge::name))
        assertEquals(12L, resolved.first().id)
        assertTrue(resolved.first().badgeCss?.contains("#22d3ee") == true)
        assertEquals(null, resolved.last().badgeCss)
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

        assertEquals(listOf("剧情", "伏笔"), forumFeedBadges(item))
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

        assertEquals("运营记录 · 23分钟前", meta)
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
            listOf("80 条回复", "有价值 81", "欢乐 12", "奖励 7", "7305 次浏览"),
            forumFeedMetricLabels(item)
        )
    }

    @Test
    fun bookReviewFooterKeepsTheSourceHeartCounterAtZero() {
        val review = forumPostFeedItem(
            ForumPost(
                id = 57651,
                category = "书评",
                title = "Review",
                isBookReview = true,
                replyCount = 0,
                likeCount = 0,
                viewCount = 0
            )
        )

        assertEquals(
            listOf("0 条回复", "喜欢 0", "0 次浏览"),
            forumFeedMetricLabels(review)
        )
        assertEquals(
            "书评 22538",
            forumFeedCategoryLabel(forumFeedCategories().single { it.type == "review" }, 22538)
        )
    }

    @Test
    fun forumDateLabelsFollowLiveMobileCards() {
        assertEquals("2026/8/8", forumShortDateLabel("2026-08-08 12:00:00"))
        assertEquals("刚刚", forumShortDateLabel(null))
        assertEquals("12小时前", forumShortDateLabel("12小时前"))
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
    fun forumRichContentKeepsBoldMarkdownAndClickableInternalAndExternalLinks() {
        val paragraphs = forumRichParagraphs(
            """
            <p>欢迎<strong>加粗内容</strong>，<a href="/forum/42">站内链接</a> 和 [外链](https://example.test/path?x=1)</p>
            <p>**Markdown 加粗** https://novalpie.cc/book/354491</p>
            """.trimIndent()
        )

        assertEquals(2, paragraphs.size)
        assertEquals("欢迎加粗内容，站内链接 和 外链", paragraphs[0].plainText)
        assertTrue(paragraphs[0].segments.contains(ForumTextSegment.Bold("加粗内容")))
        assertTrue(paragraphs[1].segments.contains(ForumTextSegment.Bold("Markdown 加粗")))
        assertEquals(
            listOf(
                "https://novalpie.cc/forum/42",
                "https://example.test/path?x=1",
                "https://novalpie.cc/book/354491"
            ),
            paragraphs.flatMap { paragraph ->
                paragraph.segments.mapNotNull { (it as? ForumTextSegment.Link)?.url }
            }
        )
    }

    @Test
    fun forumSpoilerMarkersBecomeMaskedSegmentsInsteadOfLiteralDelimiters() {
        val paragraph = forumRichParagraphs(
            "前文 ||需要隐藏的剧透|| 后文"
        ).single()

        assertEquals(
            listOf(
                ForumTextSegment.Plain("前文 "),
                ForumTextSegment.Spoiler("需要隐藏的剧透"),
                ForumTextSegment.Plain(" 后文"),
            ),
            paragraph.segments,
        )
        assertEquals("前文 需要隐藏的剧透 后文", paragraph.plainText)
    }

    @Test
    fun truncatedSpoilerPreviewStaysMaskedFromItsOpeningMarkerToTheEnd() {
        val paragraph = forumRichParagraphs(
            "Preview before ||the server truncated this spoiler"
        ).single()

        assertEquals(
            listOf(
                ForumTextSegment.Plain("Preview before "),
                ForumTextSegment.Spoiler("the server truncated this spoiler"),
            ),
            paragraph.segments,
        )
    }

    @Test
    fun feedExcerptCapsLongSpoilersButKeepsTheRemainingTextMaskable() {
        val raw = "Preview before ||" + "x".repeat(2_000) + "|| after"

        val excerpt = forumFeedExcerptText(raw, maxCharacters = 96)
        val spoiler = forumRichParagraphs(excerpt)
            .single()
            .segments
            .filterIsInstance<ForumTextSegment.Spoiler>()
            .single()

        assertEquals(97, excerpt.length)
        assertTrue(excerpt.startsWith("Preview before ||"))
        assertTrue(spoiler.value.endsWith("…"))
    }

    @Test
    fun userActivityPreviewCapsLongContentBeforeComposeTextLayout() {
        val content = "Activity preview " + "x".repeat(2_000)

        assertEquals(641, userActivityPreviewText(content).length)
    }

    @Test
    fun forumCardTapRejectsSlowOrMovedGestures() {
        assertTrue(forumCardTapIsEligible(durationMillis = 120, distancePx = 2f, touchSlopPx = 8f))
        assertFalse(forumCardTapIsEligible(durationMillis = 450, distancePx = 2f, touchSlopPx = 8f))
        assertFalse(forumCardTapIsEligible(durationMillis = 120, distancePx = 12f, touchSlopPx = 8f))
        assertFalse(
            forumCardTapIsEligible(
                durationMillis = 120,
                distancePx = 2f,
                touchSlopPx = 8f,
                childConsumed = true,
            )
        )
    }

    @Test
    fun forumRichParagraphCacheReusesParsedContentUntilRawTextChanges() {
        var parseCount = 0
        val cache = ForumRichParagraphCache { raw ->
            parseCount += 1
            forumRichParagraphs(raw)
        }

        val first = cache.get("unchanged ||spoiler||")
        val second = cache.get("unchanged ||spoiler||")

        assertSame(first, second)
        assertEquals(1, parseCount)

        cache.get("new content")
        assertEquals(2, parseCount)
    }

    @Test
    fun hiddenSpoilerStaysMaskedUntilItsOwnSegmentIsRevealed() {
        val revealed = forumRevealSpoiler(
            hideSpoilers = true,
            revealedSpoilerIndexes = emptySet(),
            spoilerIndex = 0,
        )

        assertFalse(forumSpoilerIsVisible(true, spoilerIndex = 0, revealedSpoilerIndexes = emptySet()))
        assertTrue(forumSpoilerIsVisible(true, spoilerIndex = 0, revealedSpoilerIndexes = revealed))
        assertFalse(forumSpoilerIsVisible(true, spoilerIndex = 1, revealedSpoilerIndexes = revealed))
    }

    @Test
    fun showingSpoilersMakesEverySegmentVisibleByDefault() {
        val revealed = forumRevealSpoiler(
            hideSpoilers = false,
            revealedSpoilerIndexes = emptySet(),
            spoilerIndex = 0,
        )

        assertTrue(forumSpoilerIsVisible(false, spoilerIndex = 0, revealedSpoilerIndexes = emptySet()))
        assertTrue(forumSpoilerIsVisible(false, spoilerIndex = 1, revealedSpoilerIndexes = emptySet()))
        assertEquals(emptySet<Int>(), revealed)
    }

    @Test
    fun reviewFeedShowSpoilersDoesNotUnmaskDiscussionOrDetailContent() {
        assertFalse(forumFeedHideSpoilers(type = "review", reviewFeedHideSpoilers = false))
        assertTrue(forumFeedHideSpoilers(type = "discussion", reviewFeedHideSpoilers = false))
        assertTrue(forumContentHideSpoilers())
        assertFalse(
            forumSpoilerIsVisible(
                hideSpoilers = forumContentHideSpoilers(),
                spoilerIndex = 0,
                revealedSpoilerIndexes = emptySet(),
            )
        )
    }

    @Test
    fun forumPostDateLineShowsCreationAndDistinctUpdateTime() {
        assertEquals(
            "发布于 2026/8/8 · 更新于 2026/8/9",
            forumPostDateLine(
                ForumPost(
                    id = 91,
                    category = "交流",
                    title = "Date test",
                    createdAt = "2026-08-08 12:00:00",
                    lastActiveLabel = "2026-08-09 10:00:00"
                )
            )
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
        assertEquals(listOf("置顶", "精华"), forumFeedBadges(item))
        assertEquals(
            listOf("80 条回复", "有价值 81", "欢乐 12", "奖励 7", "7305 次浏览"),
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

    @Test
    fun forumPaginationRetainsSourceFirstAndLastPagesAroundTheCurrentWindow() {
        val firstPage = forumPaginationWindow(page = 1, totalPages = 1141)

        assertEquals(1, firstPage.currentPage)
        assertEquals(1141, firstPage.totalPages)
        assertEquals(
            listOf(
                ForumPaginationToken.Page(1),
                ForumPaginationToken.Page(2),
                ForumPaginationToken.Page(3),
                ForumPaginationToken.Page(4),
                ForumPaginationToken.Page(5),
                ForumPaginationToken.Ellipsis,
                ForumPaginationToken.Page(1141)
            ),
            firstPage.tokens
        )
        assertEquals(2, firstPage.nextPage)

        val middlePage = forumPaginationWindow(page = 570, totalPages = 1141)
        assertEquals(
            listOf(
                ForumPaginationToken.Page(1),
                ForumPaginationToken.Ellipsis,
                ForumPaginationToken.Page(568),
                ForumPaginationToken.Page(569),
                ForumPaginationToken.Page(570),
                ForumPaginationToken.Page(571),
                ForumPaginationToken.Page(572),
                ForumPaginationToken.Ellipsis,
                ForumPaginationToken.Page(1141)
            ),
            middlePage.tokens
        )
        assertEquals(569, middlePage.previousPage)
        assertEquals(571, middlePage.nextPage)
        assertEquals(1141, forumPageJumpTarget("1141", 1141))
        assertEquals(null, forumPageJumpTarget("1142", 1141))
    }
}
