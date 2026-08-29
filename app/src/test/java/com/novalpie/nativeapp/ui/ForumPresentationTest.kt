package com.novalpie.nativeapp.ui

import androidx.compose.ui.text.buildAnnotatedString
import com.novalpie.nativeapp.model.ForumComment
import com.novalpie.nativeapp.model.ForumPost
import com.novalpie.nativeapp.model.UserBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun markdownHeadingPrefixesAreRenderedAsTextInsteadOfRawMarkup() {
        val paragraphs = forumRichParagraphs(
            "##### <u>[#1173](https://novalpie.cc/forum/1173)</u>\n\n" +
                "#### 推薦個站點：Arcalive"
        )

        assertEquals("#1173", paragraphs[0].plainText)
        assertEquals("推薦個站點：Arcalive", paragraphs[1].plainText)
        assertTrue(paragraphs.none { it.plainText.contains("#####") || it.plainText.contains("####") })
        assertEquals(
            "https://novalpie.cc/forum/1173",
            paragraphs[0].segments.filterIsInstance<ForumTextSegment.Link>().single().url,
        )
    }

    @Test
    fun sourceFoldBlocksRenderAsClosedSummariesInsteadOfRawMarkup() {
        val paragraphs = forumRichParagraphs(
            """
            普通前言

            [fold:这里添加的是所有人都会看见的全局规则]
            ![规则说明](https://i.imgs.ovh/2026/08/20/f473c4fe6b6cb6d75e94bd179a760f9c.jpg)
            [下载页面](https://github.com/never-seek/novalpie/releases/tag/v2.0.0-native-beta4)
            [/fold]

            普通结尾
            """.trimIndent()
        )

        assertEquals(
            listOf("普通前言", "折叠：这里添加的是所有人都会看见的全局规则", "普通结尾"),
            paragraphs.map(ForumRichParagraph::plainText),
        )
        assertFalse(
            paragraphs.any { paragraph ->
                paragraph.plainText.contains("[fold:", ignoreCase = true) ||
                    paragraph.plainText.contains("[/fold]", ignoreCase = true)
            }
        )
    }

    @Test
    fun foldBodyKeepsBlankSeparatedRichParagraphsInsideOneExpandableBlock() {
        val paragraphs = forumRichParagraphs(
            """
            [fold:多段折叠]
            第一段说明

            [外链](https://example.test/folded)
            [/fold]
            """.trimIndent()
        )

        val fold = paragraphs.singleOrNull()?.fold
            ?: throw AssertionError("expected one fold block")
        assertEquals("多段折叠", fold.title)
        assertEquals(
            listOf("第一段说明", "外链"),
            forumRichParagraphs(fold.content).map(ForumRichParagraph::plainText),
        )
    }

    @Test
    fun separateLinesBetweenFoldLinksRemainReadableAndClickable() {
        val fold = forumRichParagraphs(
            "[fold:链接列表]\nhttps://example.test/one\nhttps://example.test/two\n[/fold]"
        ).single().fold ?: throw AssertionError("expected fold")
        val body = forumRichParagraphs(fold.content).single()

        assertEquals(
            "https://example.test/one\nhttps://example.test/two",
            body.plainText,
        )
        assertEquals(
            listOf(
                ForumTextSegment.Link("https://example.test/one", "https://example.test/one"),
                ForumTextSegment.Plain("\n"),
                ForumTextSegment.Link("https://example.test/two", "https://example.test/two"),
            ),
            body.segments,
        )
    }

    @Test
    fun relativeMarkdownLinksBecomeClickableLinksInsteadOfRawMarkup() {
        val paragraphs = forumRichParagraphs(
            "站内入口 [打开帖子](/forum/42) 和 [打开书籍](/book/354491)"
        )

        assertEquals("站内入口 打开帖子 和 打开书籍", paragraphs.single().plainText)
        assertEquals(
            listOf(
                ForumTextSegment.Link("打开帖子", "https://novalpie.cc/forum/42"),
                ForumTextSegment.Link("打开书籍", "https://novalpie.cc/book/354491"),
            ),
            paragraphs.single().segments.filterIsInstance<ForumTextSegment.Link>(),
        )
    }

    @Test
    fun protocolRelativeMarkdownLinksUseHttpsWithoutChangingTheirHost() {
        val paragraph = forumRichParagraphs("[项目主页](//github.com/never-seek/novalpie)").single()

        assertEquals("项目主页", paragraph.plainText)
        assertEquals(
            ForumTextSegment.Link(
                label = "项目主页",
                url = "https://github.com/never-seek/novalpie",
            ),
            paragraph.segments.single(),
        )
    }

    @Test
    fun liveCommentMarkdownImageBecomesADedicatedRenderableParagraph() {
        val imageUrl = "https://p.sda1.dev/34/d8f8ba919c228eddf5f9bf1630748042/7196e54684e89fcf3d971058a75bd5b7.jpg"
        val paragraphs = forumRichParagraphs(
            """
            ![]($imageUrl)
            不是很想修，反正皇書站也沒啥人看和尚文。
            """.trimIndent()
        )

        assertEquals(2, paragraphs.size)
        assertEquals(
            "Image(url=$imageUrl, alt=)",
            paragraphs.first().segments.single().toString(),
        )
        assertEquals("不是很想修，反正皇書站也沒啥人看和尚文。", paragraphs.last().plainText)
        assertFalse(paragraphs.any { it.plainText.contains("![](") })
    }

    @Test
    fun forumHtmlImageSupportsLazySourceButNeverRendersUnsafeScheme() {
        val imageUrl = "https://p.sda1.dev/34/d8f8ba919c228eddf5f9bf1630748042/7196e54684e89fcf3d971058a75bd5b7.jpg"
        val paragraphs = forumRichParagraphs(
            "<p>图片前</p><img data-src=\"$imageUrl\" alt=\"动态插图\"><p>图片后</p>"
        )

        assertEquals(listOf("图片前", "图片后"), paragraphs.map(ForumRichParagraph::plainText).filter(String::isNotBlank))
        assertEquals(
            "Image(url=$imageUrl, alt=动态插图)",
            paragraphs.single { it.plainText.isBlank() }.segments.single().toString(),
        )
        assertTrue(
            forumRichParagraphs("![](javascript:alert(1))")
                .flatMap(ForumRichParagraph::segments)
                .none { it::class.simpleName == "Image" },
        )
    }

    @Test
    fun standaloneBookReferenceSyntaxIsNotRenderedAsLiteralCommentText() {
        val paragraphs = forumRichParagraphs(
            """
            厉害，我甚至把这本上万张插图的书给下载下来了
            [bookid:350192]
            不过实际看书的时候还是有点难受
            """.trimIndent()
        )

        assertFalse(
            paragraphs.any { paragraph ->
                paragraph.plainText.contains("[bookid:", ignoreCase = true)
            }
        )
    }

    @Test
    fun bookReferenceSyntaxSeparatesSurroundingTextAndRetainsItsPositiveId() {
        val raw = "前文[bookid:350192]后文"
        val paragraphs = forumRichParagraphs(raw)

        assertEquals(listOf("前文", "后文"), paragraphs.map(ForumRichParagraph::plainText).filter(String::isNotBlank))
        assertEquals(listOf(350192L), paragraphs.mapNotNull(ForumRichParagraph::bookReferenceId))
        assertEquals(listOf(350192L), forumBookReferenceIds(raw))
        assertEquals(
            ForumTextSegment.BookReference(350192L),
            paragraphs.single { it.bookReferenceId != null }.segments.single()
        )
    }

    @Test
    fun invalidBookReferenceSyntaxRemainsPlainTextAndDoesNotCreateANetworkId() {
        val raw = "[bookid:0]\n[bookid:]\n[bookid:not-a-number]"

        assertEquals(emptyList<Long>(), forumBookReferenceIds(raw))
        assertTrue(forumRichParagraphs(raw).single().plainText.contains("[bookid:0]"))
    }

    @Test
    fun forumPostCommentsAndRepliesShareOneDeduplicatedBookReferenceParser() {
        val rootComment = ForumComment(id = 1, content = "根评论 [bookid:350192]")
        val reply = ForumComment(id = 2, parentCommentId = 1, content = "回复 [bookid:354491]")

        assertEquals(
            listOf(350192L, 354491L),
            forumBookReferenceIds(
                listOf(
                    "帖子正文 [bookid:350192]",
                    rootComment.content,
                    reply.content,
                )
            )
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
    fun truncatedFoldPreviewBecomesAClosedSummaryInsteadOfLeakingFoldSyntax() {
        // The review feed can truncate a server excerpt before its closing [/fold] marker.
        // A compact card must not render the source control syntax or its hidden body as plain text.
        val raw = "前言\n[fold:点击展开]\n" + "折叠正文".repeat(200)

        val excerpt = forumFeedExcerptText(raw, maxCharacters = 96)
        val paragraphs = forumRichParagraphs(excerpt)

        assertEquals(listOf("点击展开"), paragraphs.mapNotNull { it.fold?.title })
        assertTrue(paragraphs.none { it.plainText.contains("[fold:", ignoreCase = true) })
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
    fun forumCardNavigationPolicyRejectsTheReproducedLongSwipeBeforeOpeningABook() {
        assertFalse(
            forumCardNavigationAllowed(
                durationMillis = 1_200,
                distancePx = 1_190f,
                touchSlopPx = 24f,
                childConsumed = false,
            )
        )
        assertTrue(
            forumCardNavigationAllowed(
                durationMillis = 120,
                distancePx = 2f,
                touchSlopPx = 24f,
                childConsumed = false,
            )
        )
    }

    @Test
    fun forumNavigationTapIsDisabledWhileTheDetailListIsScrolling() {
        assertFalse(
            forumNavigationTapEnabled(
                destinationAvailable = true,
                isScrollInProgress = true,
            )
        )
        assertTrue(
            forumNavigationTapEnabled(
                destinationAvailable = true,
                isScrollInProgress = false,
            )
        )
        assertFalse(
            forumNavigationTapEnabled(
                destinationAvailable = false,
                isScrollInProgress = false,
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
    fun sharedShowSpoilersPreferenceUnmasksDiscussionAndDetailContent() {
        assertFalse(forumFeedHideSpoilers(type = "review", reviewFeedHideSpoilers = false))
        assertFalse(forumFeedHideSpoilers(type = "discussion", reviewFeedHideSpoilers = false))
        assertTrue(forumContentHideSpoilers())
        assertFalse(forumContentHideSpoilers(preference = false))
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
    fun forumNestedReplySubmissionUsesThreadRootIdAndKeepsDirectReplyTargetSeparate() {
        val root = ForumComment(id = 700, authorName = "root", content = "root comment")
        val reply = ForumComment(
            id = 701,
            parentCommentId = root.id,
            authorName = "reply",
            replyToName = root.authorName,
            content = "first reply",
        )
        val nestedReply = ForumComment(
            id = 702,
            parentCommentId = reply.id,
            authorName = "nested",
            replyToName = reply.authorName,
            content = "reply to reply",
        )

        assertEquals(
            root.id,
            forumReplySubmissionCommentId(nestedReply, listOf(root, reply, nestedReply)),
        )
    }

    @Test
    fun forumReplySubmissionFallsBackToTheKnownParentWhenAncestorsAreOutsidePage() {
        val nestedReply = ForumComment(
            id = 702,
            parentCommentId = 701,
            authorName = "nested",
            content = "reply to reply",
        )

        assertEquals(
            nestedReply.parentCommentId,
            forumReplySubmissionCommentId(nestedReply),
        )
    }

    @Test
    fun selectingReplyTargetKeepsComposerBodyEmptyUntilUserTypes() {
        assertEquals(
            "",
            replyComposerDraftForTarget(
                currentDraft = "@旧目标 ",
                previousTargetName = "旧目标",
                nextTargetName = "新目标",
            ),
        )
        assertEquals(
            "我已经写好的内容",
            replyComposerDraftForTarget(
                currentDraft = "我已经写好的内容",
                previousTargetName = "旧目标",
                nextTargetName = "新目标",
            ),
        )
        assertEquals(
            "",
            replyComposerDraftForTarget(
                currentDraft = "",
                previousTargetName = null,
                nextTargetName = "新目标",
            ),
        )
    }

    @Test
    fun forumNestedReplyInteractionsUseThreadRootAndReplyIds() {
        val root = ForumComment(id = 700, content = "root")
        val reply = ForumComment(id = 701, parentCommentId = 700, content = "reply")
        val nested = ForumComment(id = 702, parentCommentId = 701, content = "nested")

        assertEquals(
            ForumCommentActionTarget(parentCommentId = 700, replyId = 702),
            forumCommentActionTarget(nested, listOf(root, reply, nested)),
        )
        assertEquals(
            ForumCommentActionTarget(parentCommentId = 701, replyId = 702),
            forumCommentActionTarget(nested),
        )
        assertEquals(
            ForumCommentActionTarget(parentCommentId = 700),
            forumCommentActionTarget(root),
        )
    }

    @Test
    fun selectingAnyReplyRequestsTheComposerItemToBecomeVisible() {
        assertEquals(3, forumPostDetailComposerScrollIndex(2284L))
        assertNull(forumPostDetailComposerScrollIndex(null))
        assertNull(forumPostDetailComposerScrollIndex(0L))
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

    @Test
    fun sourceInlineMarkupDoesNotLeakRawMarkdownOrHtmlControlCharacters() {
        val paragraph = forumRichParagraphs(
            "*斜体*、<u>下划线</u>、~~删除线~~、`行内代码`"
        ).single()

        assertEquals("斜体、下划线、删除线、行内代码", paragraph.plainText)
        assertFalse(paragraph.plainText.contains('*'))
        assertFalse(paragraph.plainText.contains("~~"))
        assertFalse(paragraph.plainText.contains('`'))
        assertFalse(paragraph.plainText.contains("<u>", ignoreCase = true))
    }

    @Test
    fun markdownInsideAnHtmlStyleWrapperDoesNotLeakControlCharacters() {
        val paragraph = forumRichParagraphs(
            "<u>**建议在看书页面的设置里打开文本替换-全部规则**</u>"
        ).single()

        assertEquals("建议在看书页面的设置里打开文本替换-全部规则", paragraph.plainText)
        assertTrue(paragraph.segments.any { it is ForumTextSegment.Bold })
        assertFalse(paragraph.plainText.contains("**"))
    }

    @Test
    fun sourceBookReferenceOptionsStillResolveTheNativeBookCardId() {
        val raw = "[bookid:354491|tags,bio]"

        assertEquals(listOf(354491L), forumBookReferenceIds(raw))
        assertEquals(
            ForumTextSegment.BookReference(
                bookId = 354491L,
                showTags = true,
                showBio = true,
            ),
            forumRichParagraphs(raw).single().bookReference,
        )
    }

    @Test
    fun websiteBookReferenceWhitespaceAroundOptionsStillBecomesANativeCard() {
        val raw = "[bookid:353813 | tag,bio]"

        assertEquals(listOf(353813L), forumBookReferenceIds(raw))
        assertEquals(
            ForumTextSegment.BookReference(
                bookId = 353813L,
                showTags = true,
                showBio = true,
            ),
            forumRichParagraphs(raw).single().bookReference,
        )
    }

    @Test
    fun liveCommentBookReferenceAfterOrdinaryTextBecomesItsOwnNativeCard() {
        val raw = """
            厉害，我甚至把这本上万张插图的书给下载下来了
            [bookid:350192]
            不过实际看书的时候还是有点难受
        """.trimIndent()
        val paragraphs = forumRichParagraphs(raw)

        assertEquals(
            listOf(
                "厉害，我甚至把这本上万张插图的书给下载下来了",
                "",
                "不过实际看书的时候还是有点难受",
            ),
            paragraphs.map(ForumRichParagraph::plainText),
        )
        assertEquals(350192L, paragraphs[1].bookReferenceId)
        assertEquals(listOf(350192L), forumBookReferenceIds(raw))
    }

    @Test
    fun commentComposerTemplatesMatchTheWebsiteMarkdownEditorSyntax() {
        assertEquals(
            "||黑幕||",
            forumCommentMarkupEdit(
                text = "黑幕",
                selectionStart = 0,
                selectionEnd = 2,
                action = ForumCommentMarkupAction.Spoiler,
            ).text,
        )
        assertEquals(
            "[链接](url)",
            forumCommentMarkupEdit(
                text = "链接",
                selectionStart = 0,
                selectionEnd = 2,
                action = ForumCommentMarkupAction.Link,
            ).text,
        )
        assertEquals(
            "[fold:标题]\n标题\n[/fold]",
            forumCommentMarkupEdit(
                text = "标题",
                selectionStart = 0,
                selectionEnd = 2,
                action = ForumCommentMarkupAction.Fold,
            ).text,
        )
        assertEquals(
            "*斜体文字*",
            forumCommentMarkupEdit(
                text = "",
                selectionStart = 0,
                selectionEnd = 0,
                action = ForumCommentMarkupAction.Italic,
            ).text,
        )
    }

    @Test
    fun htmlEntityEncodedLinksBecomeNativeLinksInsteadOfRawMarkup() {
        val paragraph = forumRichParagraphs(
            "&lt;a href=\"/forum/1173\"&gt;打开帖子&lt;/a&gt;"
        ).single()

        assertEquals(
            ForumTextSegment.Link(
                label = "打开帖子",
                url = "https://novalpie.cc/forum/1173",
            ),
            paragraph.segments.single(),
        )
        assertEquals("打开帖子", paragraph.plainText)
    }

    @Test
    fun entityEncodedLinksInsideFoldRemainActionableAfterExpansion() {
        val fold = forumRichParagraphs(
            "[fold:链接]\n&lt;a href=\"https://example.com/a?b=1&amp;c=2\"&gt;站外链接&lt;/a&gt;\n[/fold]"
        ).single().fold

        requireNotNull(fold)
        val expanded = forumRichParagraphs(fold.content).single()
        assertEquals(
            ForumTextSegment.Link(
                label = "站外链接",
                url = "https://example.com/a?b=1&c=2",
            ),
            expanded.segments.single(),
        )
    }

    @Test
    fun htmlDetailsAndSummaryBecomeTheSameExpandableFoldContractAsTheWebsite() {
        val fold = forumRichParagraphs(
            "<details><summary>网页折叠</summary><p><a href=\"/forum/1173\">打开帖子</a></p></details>"
        ).single().fold

        requireNotNull(fold)
        assertEquals("网页折叠", fold.title)
        assertEquals(
            ForumTextSegment.Link(
                label = "打开帖子",
                url = "https://novalpie.cc/forum/1173",
            ),
            forumRichParagraphs(fold.content).single().segments.single(),
        )
    }

    @Test
    fun livePost1833FoldPayloadKeepsBothFoldsAndTheirImages() {
        val raw = """
            我已经看到四五本书被添加了影响阅读的全局规则了，请各位坛友使用的时候注意一下。
            在"文本替换→全局规则"里，你添加的规则**会对所有读者生效**，且其他读者**目前无法屏蔽你的规则**。
            如果阅读时有特殊需求请点击"仅我的规则"。
            [fold:这里添加的是所有人都会看见的全局规则]
            ![谁把主角给换成的丰川祥子](https://i.imgs.ovh/2026/08/20/f473c4fe6b6cb6d75e94bd179a760f9c.jpg)
            [/fold]
            [fold:这里添加仅自己可见的本地规则]
            ![你在这里替换没人管你是换成祥子还是孙笑川](https://i.imgs.ovh/2026/08/20/7ccb487d273c9470ec9e9a0d0ebb9613.jpg)
            [/fold]
        """.trimIndent()

        val paragraphs = forumRichParagraphs(raw)
        val folds = paragraphs.mapNotNull { it.fold }

        assertEquals(2, folds.size)
        assertEquals(
            listOf("这里添加的是所有人都会看见的全局规则", "这里添加仅自己可见的本地规则"),
            folds.map(ForumTextSegment.Fold::title),
        )
        assertTrue(folds.all { fold ->
            forumRichParagraphs(fold.content).singleOrNull()?.image?.url?.startsWith("https://") == true
        })
        assertTrue(paragraphs.none { paragraph -> paragraph.plainText.contains("[fold:") })
    }

    @Test
    fun livePost1796FoldUrlsRemainIndividualClickableSegments() {
        val raw = """
            ##### <u>[#1173](https://novalpie.cc/forum/1173)</u>

            #### 推荐站点

            [fold:AI 聊天频道]
            https://arca.live/b/characterai?category=19
            https://arca.live/b/characterai?category=review19
            [/fold]

            [fold:AI 约会频道]
            https://arca.live/b/ailove?category=bot3
            https://arca.live/b/ailove?category=collabo
            [/fold]
        """.trimIndent()

        val folds = forumRichParagraphs(raw).mapNotNull { it.fold }
        assertEquals(2, folds.size)
        assertEquals(
            listOf(
                "https://arca.live/b/characterai?category=19",
                "https://arca.live/b/characterai?category=review19",
            ),
            forumRichParagraphs(folds[0].content).flatMap { paragraph ->
                paragraph.segments.filterIsInstance<ForumTextSegment.Link>().map { it.url }
            },
        )
        assertEquals(
            listOf(
                "https://arca.live/b/ailove?category=bot3",
                "https://arca.live/b/ailove?category=collabo",
            ),
            forumRichParagraphs(folds[1].content).flatMap { paragraph ->
                paragraph.segments.filterIsInstance<ForumTextSegment.Link>().map { it.url }
            },
        )
    }

    @Test
    fun adjacentFoldBlocksDoNotLeakTheSecondMarkerOrUrlTemplate() {
        val raw = "[fold:第一个]\n第一段\n[/fold][fold:第二个]\n[链接文字](https://example.com/a)\n[/fold]"

        val paragraphs = forumRichParagraphs(raw)
        assertEquals(listOf("第一个", "第二个"), paragraphs.mapNotNull { it.fold?.title })
        val second = paragraphs.mapNotNull { it.fold }.last()
        assertEquals(
            ForumTextSegment.Link("链接文字", "https://example.com/a"),
            forumRichParagraphs(second.content).single().segments.single(),
        )
        assertTrue(paragraphs.none { it.plainText.contains("[/fold]") })
    }

    @Test
    fun markdownLinkPlaceholderDoesNotExposeBracketSyntaxInsideAFold() {
        val fold = forumRichParagraphs(
            """
            [fold:链接]
            [链接文字](url)
            [/fold]
            """.trimIndent()
        ).single().fold ?: throw AssertionError("expected fold")

        val paragraph = forumRichParagraphs(fold.content).single()
        assertEquals("链接文字", paragraph.plainText)
        assertFalse(paragraph.plainText.contains("[链接文字]"))
    }

    @Test
    fun bbcodeUrlVariantsBecomeActionableLinksInsteadOfRawMarkup() {
        assertEquals(
            ForumTextSegment.Link(
                label = "打开帖子",
                url = "https://novalpie.cc/forum/1173",
            ),
            forumRichParagraphs("[url=https://novalpie.cc/forum/1173]打开帖子[/url]")
                .single().segments.single(),
        )
        assertEquals(
            ForumTextSegment.Link(
                label = "https://example.com/long?id=1",
                url = "https://example.com/long?id=1",
            ),
            forumRichParagraphs("[url]https://example.com/long?id=1[/url]")
                .single().segments.single(),
        )
    }

    @Test
    fun fencedCodeDoesNotLeakMarkdownFenceMarkersIntoCommentText() {
        val paragraph = forumRichParagraphs(
            "```kotlin\nval answer = 42\nprintln(answer)\n```"
        ).single()

        assertEquals("val answer = 42\nprintln(answer)", paragraph.plainText)
        assertFalse(paragraph.plainText.contains("```"))
        assertEquals(
            ForumTextSegment.CodeBlock(
                value = "val answer = 42\nprintln(answer)",
                language = "kotlin",
            ),
            paragraph.codeBlock,
        )
    }
}
