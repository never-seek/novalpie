package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ChapterComment
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.ReaderChapterCacheState
import com.novalpie.nativeapp.model.ReaderChapterContent
import com.novalpie.nativeapp.model.ReaderContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPresentationTest {
    @Test
    fun coreReaderBodyPublishesWithoutOverwritingAuxiliaryPanels() {
        val initial = ReaderState(
            bookId = 7,
            chapterId = 9,
            content = LoadResult.Loading,
            chapters = LoadResult.Loading,
            comments = LoadResult.Loading,
            favoriteStatus = LoadResult.Loading,
        )
        val content = ReaderContent(title = "第一章", content = "正文", source = "api")
        val window = listOf(ReaderChapterContent(9, "第一章", content))

        val updated = readerStateAfterCoreContent(
            current = initial,
            content = LoadResult.Success(content),
            chapterContents = window,
            contentFromCache = false,
        )

        assertEquals(LoadResult.Success(content), updated.content)
        assertEquals(window, updated.chapterContents)
        assertEquals(LoadResult.Loading, updated.chapters)
        assertEquals(LoadResult.Loading, updated.comments)
        assertEquals(LoadResult.Loading, updated.favoriteStatus)
    }
    @Test
    fun readerTextLayoutMatchesWebsiteDefaultsAndKeepsUserSelectedSize() {
        assertEquals(
            ReaderTextLayout(
                fontSizeSp = 16,
                lineHeightSp = 25.6f,
                firstLineIndentSp = 32f,
                paragraphSpacingDp = 20f,
                horizontalPaddingDp = 16f,
                titleFontSizeSp = 20f,
                titleBottomSpacingDp = 24f,
            ),
            readerTextLayout(16)
        )
        assertEquals(
            ReaderTextLayout(
                fontSizeSp = 20,
                lineHeightSp = 32f,
                firstLineIndentSp = 40f,
                paragraphSpacingDp = 20f,
                horizontalPaddingDp = 16f,
                titleFontSizeSp = 20f,
                titleBottomSpacingDp = 24f,
            ),
            readerTextLayout(20)
        )
    }

    @Test
    fun readerTextLayoutBoundsAnInvalidSizeWithoutChangingValidPreferences() {
        assertEquals(12, readerTextLayout(1).fontSizeSp)
        assertEquals(48, readerTextLayout(100).fontSizeSp)
    }

    @Test
    fun readerContentUsesTheWebsiteWideArticleCapWithoutConstrictingMobile() {
        assertNull(readerContentMaxWidthDp(360f))
        assertNull(readerContentMaxWidthDp(800f))
        assertEquals(800f, readerContentMaxWidthDp(1280f))
    }

    @Test
    fun readerChromeMatchesTheWebsiteMobileInformationRails() {
        assertEquals(
            ReaderChromeLayout(headerHeightDp = 32f, statusHeightDp = 26f, sidePaddingDp = 12f),
            readerChromeLayout(),
        )
        assertEquals(64f, readerActionRailWidthDp())
        assertEquals(0.84f, readerSidePanelWidthFraction())
        assertEquals(440f, readerSidePanelMaxWidthDp())
    }

    @Test
    fun readerTapAreasResolveConfiguredSourceZones() {
        val areas = defaultReaderTapAreas()
        assertEquals("pagePrev", readerTapAreaAt(areas, 0.05f)?.action)
        assertEquals("sidebar", readerTapAreaAt(areas, 0.5f)?.action)
        assertEquals("pageNext", readerTapAreaAt(areas, 0.95f)?.action)
        assertEquals("sidebar", readerTapActionAt(areas, 0.5f))
    }

    @Test
    fun readerGestureTreatsDragAsScrollAndKeepsChromeDismissalResponsive() {
        assertFalse(readerTapExceedsTouchSlop(distancePx = 8f, touchSlopPx = 8f))
        assertEquals(true, readerTapExceedsTouchSlop(distancePx = 8.1f, touchSlopPx = 8f))
        assertEquals(true, readerShouldToggleChrome(lastToggleUptimeMillis = 0L, currentUptimeMillis = 100L))
        assertFalse(readerShouldToggleChrome(lastToggleUptimeMillis = 100L, currentUptimeMillis = 400L))
        assertEquals(
            true,
            readerShouldToggleChrome(
                lastToggleUptimeMillis = 100L,
                currentUptimeMillis = 400L,
                sameTapTarget = false,
            ),
        )
        assertEquals(true, readerShouldToggleChrome(lastToggleUptimeMillis = 100L, currentUptimeMillis = 701L))
        assertEquals(true, readerChromeTapTargetsOverlap(0.50f, 0.50f, 0.55f, 0.56f))
        assertFalse(readerChromeTapTargetsOverlap(0.50f, 0.50f, 0.70f, 0.50f))
        assertEquals(
            true,
            readerChromeVisibilityAfterBodyTap(
                controlsVisible = false,
                lastToggleUptimeMillis = 0L,
                currentUptimeMillis = 100L,
                sameTapTarget = true,
            ),
        )
        assertEquals(
            true,
            readerChromeVisibilityAfterBodyTap(
                controlsVisible = true,
                lastToggleUptimeMillis = 100L,
                currentUptimeMillis = 400L,
                sameTapTarget = true,
            ),
        )
        assertEquals(
            false,
            readerChromeVisibilityAfterBodyTap(
                controlsVisible = true,
                lastToggleUptimeMillis = 100L,
                currentUptimeMillis = 400L,
                sameTapTarget = false,
            ),
        )
        assertEquals(
            ReaderBodyTapResolution.ApplyConfiguredTapArea,
            readerBodyTapResolution(
                controlsVisible = false,
                lastToggleUptimeMillis = 100L,
                currentUptimeMillis = 400L,
                sameTapTarget = false,
            ),
        )
        assertEquals(
            ReaderBodyTapResolution.KeepChromeVisible,
            readerBodyTapResolution(
                controlsVisible = true,
                lastToggleUptimeMillis = 100L,
                currentUptimeMillis = 400L,
                sameTapTarget = true,
            ),
        )
        assertEquals(
            ReaderBodyTapResolution.DismissChrome,
            readerBodyTapResolution(
                controlsVisible = true,
                lastToggleUptimeMillis = 100L,
                currentUptimeMillis = 400L,
                sameTapTarget = false,
            ),
        )

        val misconfiguredCenter = defaultReaderTapAreas().map { area ->
            if (area.position == "center") area.copy(action = "catalog") else area
        }
        assertEquals("sidebar", readerTapActionAt(misconfiguredCenter, 0.5f))
    }

    @Test
    fun readerBodyTapRemainsEligibleWhenSelectableTextConsumesTheFinalPointerPass() {
        assertTrue(
            readerBodyTapIsEligible(
                moved = false,
                durationMillis = 120L,
                longPressTimeoutMillis = 500L,
            ),
        )
        assertFalse(
            readerBodyTapIsEligible(
                moved = true,
                durationMillis = 120L,
                longPressTimeoutMillis = 500L,
            ),
        )
        assertFalse(
            readerBodyTapIsEligible(
                moved = false,
                durationMillis = 500L,
                longPressTimeoutMillis = 500L,
            ),
        )
    }

    @Test
    fun readerPageEffectsHaveDistinctStableDurations() {
        assertEquals(160, readerPageAnimationDurationMs("fade"))
        assertEquals(280, readerPageAnimationDurationMs("cover"))
        assertEquals(220, readerPageAnimationDurationMs("slide"))
        assertEquals(360, readerPageAnimationDurationMs("simulated"))
    }

    @Test
    fun continuousScrollNormalizesAwayConflictingPageTurnMode() {
        val options = ReaderUiOptions(useInfiniteScroll = true, pageTurnMode = true)
            .normalizedReaderOptions()

        assertEquals(true, options.useInfiniteScroll)
        assertFalse(options.pageTurnMode)
    }

    @Test
    fun loadedReaderStartsImmersiveButLoadingAndErrorsKeepNavigationReachable() {
        assertFalse(readerChromeVisible(hasReadableBody = true, controlsRequested = false))
        assertEquals(true, readerChromeVisible(hasReadableBody = true, controlsRequested = true))
        assertEquals(true, readerChromeVisible(hasReadableBody = false, controlsRequested = false))
        assertEquals(0f, readerContentTopPaddingDp(chromeVisible = false))
        assertEquals(32f, readerContentTopPaddingDp(chromeVisible = true))
    }

    @Test
    fun readerDoesNotExposeSourceDebugLine() {
        assertNull(readerSourceDebugLine("api"))
        assertNull(readerSourceDebugLine("fallback"))
        assertNull(readerSourceDebugLine(""))
    }

    @Test
    fun readerDoesNotExposeBookAndChapterDebugIdentityLine() {
        assertNull(readerDebugIdentityLine(bookId = 354491, chapterId = 8001))
    }

    @Test
    fun readerActionRailUsesTheSourceMobileActionOrder() {
        assertEquals(
            listOf("关闭", "帮助", "目录", "设置", "主题", "上章", "下章", "滑动", "听书", "全屏", "导航"),
            readerActionRailLabels(),
        )
        assertEquals(readerActionRailLabels(), readerToolbarLabels())
        assertEquals("滑动", readerReadingModeActionLabel(pageTurnMode = false))
        assertEquals("翻页", readerReadingModeActionLabel(pageTurnMode = true))
    }

    @Test
    fun readerNavigationKeepsTheBookAndWebEscapesDistinct() {
        assertEquals(
            listOf("关闭", "帮助", "目录", "设置", "主题", "上章", "下章", "滑动", "听书", "全屏", "导航"),
            readerCompactToolbarLabels(fontSizeSp = 17, theme = "sepia")
        )
        assertEquals("系统", "system".readerThemeLabel())
        assertEquals("深色", "dark".readerThemeLabel())
        assertEquals(listOf("书本页", "网页正文"), readerNavigationPanelLabels(showFavoriteAction = false))
        assertEquals(listOf("书本页", "网页正文", "收藏"), readerNavigationPanelLabels(showFavoriteAction = true))
        assertEquals(0, readerActionRailLabels().count { it == "网页正文" })
    }

    @Test
    fun customThemeLabelUsesTheSavedThemeName() {
        val theme = com.novalpie.nativeapp.model.ReaderCustomTheme(
            id = "ocean",
            name = "海边夜读",
        )

        assertEquals(
            "海边夜读",
            readerThemeLabel("custom:ocean", listOf(theme)),
        )
        assertEquals(
            "系统",
            readerThemeLabel("custom:missing", listOf(theme)),
        )
    }

    @Test
    fun readerSidePanelsUseTheSourceMobileLanguage() {
        assertEquals("目录", readerCatalogPanelTitle())
        assertEquals("关闭", readerCloseCatalogLabel())
        assertEquals(listOf("帮助", "目录", "设置", "主题", "导航"), readerSurfaceSections())
    }

    @Test
    fun readerCatalogOnlyMarksAVerifiedLocalCacheAsCurrent() {
        assertEquals("缓存最新", readerCatalogCacheLabel(ReaderChapterCacheState.Current))
        assertEquals("缓存待刷新", readerCatalogCacheLabel(ReaderChapterCacheState.Stale))
        assertEquals("无缓存", readerCatalogCacheLabel(ReaderChapterCacheState.Missing))
        assertEquals("7月", readerCatalogUpdatedLabel("2026-07-02T10:20:00"))
        assertEquals("12月", readerCatalogUpdatedLabel("2026/12/02 10:20"))
        assertEquals(null, readerCatalogUpdatedLabel(null))
    }

    @Test
    fun readerReplacementModesMirrorWebsitePresets() {
        assertEquals("关闭替换", readerReplaceModeOptions().first().second)
        assertEquals("印度模式", "india".readerReplaceModeLabel())
        assertEquals("提瓦特", "genshin".readerReplaceModeLabel())
        assertEquals(11, readerReplaceModeOptions().size)
    }

    @Test
    fun ttsHighlightMapsBackToTheArticleItem() {
        val content = com.novalpie.nativeapp.model.ReaderContent(
            title = "第一章",
            content = "第一段正文。\n第二段正文。",
            source = "test",
        )
        val chapter = com.novalpie.nativeapp.model.ReaderChapterContent(10L, "第一章", content)

        assertEquals(
            2,
            readerBodyItemIndexForText(listOf(chapter), ReaderUiOptions(), "第二段正文"),
        )
    }

    @Test
    fun ttsAutoScrollCountsEachEarlierChapterCommentItem() {
        val first = ReaderChapterContent(
            chapterId = 10L,
            title = "First",
            content = ReaderContent(title = "First", content = "first body", source = "test"),
        )
        val second = ReaderChapterContent(
            chapterId = 20L,
            title = "Second",
            content = ReaderContent(title = "Second", content = "second body", source = "test"),
        )

        assertEquals(
            5,
            readerBodyItemIndexForText(listOf(first, second), ReaderUiOptions(showComments = true), "second body"),
        )
    }

    @Test
    fun continuousScrollPrefetchesBeforeTheVisibleEndAndRotatesItsSentinelAfterAppend() {
        val first = com.novalpie.nativeapp.model.ReaderChapterContent(
            chapterId = 10L,
            title = "第一章",
            content = com.novalpie.nativeapp.model.ReaderContent(
                title = "第一章",
                content = "第一段\n第二段\n第三段",
                source = "test",
            ),
        )
        val second = first.copy(chapterId = 20L, title = "第二章")

        // title + three paragraphs + finish + this chapter's comments + sentinel = seven items.
        // The sentinel is after comments so the rendered order is body -> comments -> next body.
        assertEquals(3, readerNextChapterPrefetchStartIndex(totalItemCount = 7))
        assertFalse(readerShouldPrefetchNextChapter(2, prefetchStartIndex = 3))
        assertEquals(true, readerShouldPrefetchNextChapter(3, prefetchStartIndex = 3))
        assertEquals("reader-body-end-sentinel-10-1", readerBodyEndSentinelKey(listOf(first)))
        assertEquals("reader-body-end-sentinel-20-2", readerBodyEndSentinelKey(listOf(first, second)))
    }

    @Test
    fun continuousReaderKeepsASeparateCommentStateForEveryLoadedChapter() {
        val first = ChapterComment(id = 1, chapterId = 10, authorName = "First", content = "one")
        val second = ChapterComment(id = 2, chapterId = 20, authorName = "Second", content = "two")
        val state = ReaderState(
            bookId = 7,
            chapterId = 10,
            chapterCommentStates = mapOf(
                10L to ReaderChapterCommentState(comments = LoadResult.Success(listOf(first))),
                20L to ReaderChapterCommentState(comments = LoadResult.Success(listOf(second))),
            ),
        )

        assertEquals(
            listOf(first),
            (readerChapterCommentState(state, 10L).comments as LoadResult.Success<List<ChapterComment>>).value,
        )
        assertEquals(
            listOf(second),
            (readerChapterCommentState(state, 20L).comments as LoadResult.Success<List<ChapterComment>>).value,
        )
    }

    @Test
    fun pageTurnAtTheBoundaryOpensTheAdjacentChapterInsteadOfStopping() {
        assertEquals(
            ReaderPageBoundaryTarget.NextChapter,
            readerPageBoundaryTarget(direction = 1, reachedBoundary = true, hasPrevious = true, hasNext = true),
        )
        assertEquals(
            ReaderPageBoundaryTarget.PreviousChapter,
            readerPageBoundaryTarget(direction = -1, reachedBoundary = true, hasPrevious = true, hasNext = true),
        )
        assertEquals(
            ReaderPageBoundaryTarget.None,
            readerPageBoundaryTarget(direction = 1, reachedBoundary = false, hasPrevious = true, hasNext = true),
        )
    }

    @Test
    fun readerVisibleChapterUsesArticleKeysAndIgnoresSentinelAndComments() {
        assertEquals(
            20L,
            readerFirstVisibleChapterId(
                listOf(
                    "reader-body-end-sentinel-30-3",
                    "reader-comments",
                    "reader-text-20-0",
                    "reader-title-10",
                ),
            ),
        )
        assertNull(
            readerFirstVisibleChapterId(
                listOf("reader-body-end-sentinel-30-3", "reader-comments"),
            ),
        )
    }

    @Test
    fun continuousReaderDoesNotTreatALoadingPlaceholderAsAChapterBoundary() {
        assertFalse(
            readerContinuousScrollCanTrigger(
                continuousScrollEnabled = true,
                hasReadableBody = false,
            ),
        )
        assertFalse(
            readerContinuousScrollCanTrigger(
                continuousScrollEnabled = false,
                hasReadableBody = true,
            ),
        )
        assertEquals(
            true,
            readerContinuousScrollCanTrigger(
                continuousScrollEnabled = true,
                hasReadableBody = true,
            ),
        )
    }

    @Test
    fun reachedContinuousBoundarySurvivesCatalogStateChangesUntilTheAppendCanStart() {
        assertFalse(
            readerContinuousScrollCanRequestNext(
                continuousScrollEnabled = true,
                hasReadableBody = true,
                boundaryReached = true,
                catalogReady = false,
                loadingNextChapter = false,
                nextChapterError = null,
                nextChapterWaitingForCatalog = false,
                nextChapterExhausted = false,
            ),
        )
        assertEquals(
            true,
            readerContinuousScrollCanRequestNext(
                continuousScrollEnabled = true,
                hasReadableBody = true,
                boundaryReached = true,
                catalogReady = true,
                loadingNextChapter = false,
                nextChapterError = null,
                nextChapterWaitingForCatalog = false,
                nextChapterExhausted = false,
            ),
        )
        assertFalse(
            readerContinuousScrollCanRequestNext(
                continuousScrollEnabled = true,
                hasReadableBody = true,
                boundaryReached = true,
                catalogReady = true,
                loadingNextChapter = false,
                nextChapterError = null,
                nextChapterWaitingForCatalog = true,
                nextChapterExhausted = false,
            ),
        )
        // The same retained boundary becomes eligible again once a catalog refresh resolves.
        assertEquals(
            true,
            readerContinuousScrollCanRequestNext(
                continuousScrollEnabled = true,
                hasReadableBody = true,
                boundaryReached = true,
                catalogReady = true,
                loadingNextChapter = false,
                nextChapterError = null,
                nextChapterWaitingForCatalog = false,
                nextChapterExhausted = false,
            ),
        )
        assertFalse(
            readerContinuousScrollCanRequestNext(
                continuousScrollEnabled = true,
                hasReadableBody = true,
                boundaryReached = true,
                catalogReady = true,
                loadingNextChapter = false,
                nextChapterError = "temporary failure",
                nextChapterWaitingForCatalog = false,
                nextChapterExhausted = false,
            ),
        )
        assertFalse(
            readerContinuousScrollCanRequestNext(
                continuousScrollEnabled = true,
                hasReadableBody = true,
                boundaryReached = false,
                catalogReady = true,
                loadingNextChapter = false,
                nextChapterError = null,
                nextChapterWaitingForCatalog = false,
                nextChapterExhausted = false,
            ),
        )
    }

    @Test
    fun readerUsesNormalTapToolbarsUnlessRadialControlsAreExplicitlyEnabled() {
        assertFalse(readerUsesRadialMenu(showRadialMenu = false))
        assertEquals(true, readerUsesRadialMenu(showRadialMenu = true))
        assertFalse(ReaderUiOptions().showRadialMenu)
    }

    @Test
    fun settingsNavigationKeepsEverySourceStyleReaderGroupReachable() {
        assertEquals(
            listOf("字体", "排版", "显示", "布局", "替换", "主题", "听书", "其他"),
            readerSettingsCategoryLabels(),
        )
        assertEquals("字号、字体和字重", ReaderSettingsCategory.Font.summary)
        assertEquals("恢复本机阅读偏好", ReaderSettingsCategory.Other.summary)
    }

    @Test
    fun settingsCategoryIconsStayOneToOneWithReaderGroups() {
        assertEquals(
            readerSettingsCategories().size,
            readerSettingsCategories().map(::readerSettingsCategoryIcon).distinct().size,
        )
    }

    @Test
    fun readerToolbarDoesNotExposeCrawlerOrEditorActions() {
        val forbidden = listOf("书源", "规则", "编辑", "爬取", "下载", "净化")
        readerToolbarLabels()
            .plus(readerCatalogPanelTitle())
            .plus(readerCloseCatalogLabel())
            .plus(readerSurfaceSections())
            .forEach { label ->
                forbidden.forEach { word ->
                    assertFalse(label.contains(word, ignoreCase = true))
                }
            }
    }

    @Test
    fun readerRouteOwnsReaderChromeInsteadOfGlobalProductChrome() {
        assertFalse(globalProductTopBarVisible(AppRoute.Reader(bookId = 354491, chapterId = 8001)))
        assertFalse(globalProductTopBarVisible(AppRoute.Forum))
        assertFalse(globalProductTopBarVisible(AppRoute.ForumPostDetail(postId = 535)))
        assertFalse(globalProductTopBarVisible(AppRoute.Home))
        assertFalse(globalProductTopBarVisible(AppRoute.Search))
        assertFalse(globalProductTopBarVisible(AppRoute.Tools))
        assertFalse(globalProductTopBarVisible(AppRoute.Profile))
    }

    @Test
    fun readerTopBarUsesReaderSpecificLabels() {
        assertEquals(ReaderTopBarLabels(title = "阅读"), readerTopBarLabels())
    }

    @Test
    fun ttsEngineFailureActionUsesTheSystemSettingsLabel() {
        assertEquals("系统设置", readerTtsSystemSettingsLabel())
        assertEquals("com.android.settings.TTS_SETTINGS", READER_TTS_SYSTEM_SETTINGS_ACTION)
    }

    @Test
    fun ttsWithoutAnInstalledEngineFailsImmediatelyInsteadOfSilentlyWaiting() {
        assertFalse(readerTtsEngineAvailable(engineCount = 0))
        assertTrue(readerTtsEngineAvailable(engineCount = 1))
    }

    @Test
    fun readerIllustrationPreviewCopyUsesStationaryLongPressOnly() {
        assertEquals("正文插图 1", readerIllustrationLabel(null, 1))
        assertEquals("正文插图 1", readerIllustrationLabel("   ", 0))
        assertEquals("章节插画", readerIllustrationLabel(" 章节插画 ", 2))
        assertTrue(readerIllustrationContentDescription("sample").contains("sample"))
        assertTrue(readerIllustrationPreviewHint().isNotBlank())
        assertFalse(readerIllustrationPreviewHint().contains("/"))
        assertEquals("正在加载插图", readerIllustrationLoadingLabel())
        assertEquals("插图加载失败", readerIllustrationErrorLabel())
    }

    @Test
    fun chapterCommentMetricsMirrorWebsiteActions() {
        val comment = ChapterComment(
            id = 701,
            chapterId = 9901,
            authorName = "章节读者",
            content = "章节评论正文",
            likeCount = 8,
            dislikeCount = 1,
            reactionCount = 3,
            awardPoints = 20
        )

        assertEquals(
            listOf("赞 8", "踩 1", "表情 3", "打赏 20", "回复"),
            chapterCommentMetricLabels(comment)
        )
        assertEquals("章节评论", chapterCommentsSectionTitle())
        assertEquals("打开网页评论", chapterCommentsFallbackLabel())
    }
}
