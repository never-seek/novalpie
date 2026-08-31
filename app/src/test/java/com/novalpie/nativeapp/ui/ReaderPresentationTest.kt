package com.novalpie.nativeapp.ui

import android.view.KeyEvent
import androidx.compose.ui.graphics.Color
import com.novalpie.nativeapp.model.Chapter
import com.novalpie.nativeapp.model.ChapterComment
import com.novalpie.nativeapp.model.ForumActionResult
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
    fun pageModeSideTapInterceptsInteractiveCommentControlsBeforeTurningPage() {
        assertTrue(
            readerPageModeTapShouldInterceptChild(
                pageTurnEnabled = true,
                controlsVisible = false,
                action = "pageNext",
            )
        )
        assertTrue(
            readerPageModeTapShouldInterceptChild(
                pageTurnEnabled = true,
                controlsVisible = false,
                action = "pagePrev",
            )
        )
        assertFalse(
            readerPageModeTapShouldInterceptChild(
                pageTurnEnabled = false,
                controlsVisible = false,
                action = "pageNext",
            )
        )
        assertFalse(
            readerPageModeTapShouldInterceptChild(
                pageTurnEnabled = true,
                controlsVisible = true,
                action = "pageNext",
            )
        )
        assertFalse(
            readerPageModeTapShouldInterceptChild(
                pageTurnEnabled = true,
                controlsVisible = false,
                action = "sidebar",
            )
        )
    }

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
    fun inlineCommentInteractionDoesNotAlsoTriggerTheReaderChrome() {
        assertFalse(
            readerBodyTapCanHandle(
                isShortBodyTap = true,
                inlineInteractionInProgress = true,
            )
        )
        assertTrue(
            readerBodyTapCanHandle(
                isShortBodyTap = true,
                inlineInteractionInProgress = false,
            )
        )
        assertFalse(
            readerBodyTapCanHandle(
                isShortBodyTap = false,
                inlineInteractionInProgress = false,
            )
        )
    }

    @Test
    fun readerChromeDoesNotHandleAnyTapThatStartsOnAnInlineCommentSurface() {
        assertFalse(
            readerBodyTapCanHandle(
                isShortBodyTap = true,
                inlineInteractionInProgress = true,
            )
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
    fun readerNavigationKeepsOnlyWorkingNativeDestinations() {
        assertEquals(
            listOf("关闭", "帮助", "目录", "设置", "主题", "上章", "下章", "滑动", "听书", "全屏", "导航"),
            readerCompactToolbarLabels(fontSizeSp = 17, theme = "sepia")
        )
        assertEquals("系统", "system".readerThemeLabel())
        assertEquals("深色", "dark".readerThemeLabel())
        assertEquals(listOf("书本页"), readerNavigationPanelLabels(showFavoriteAction = false))
        assertEquals(listOf("书本页", "收藏"), readerNavigationPanelLabels(showFavoriteAction = true))
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
    fun chapterCommentMutationPreservesTheContinuousReaderWindow() {
        val first = ReaderChapterContent(
            chapterId = 10L,
            title = "第一章",
            content = ReaderContent(title = "第一章", content = "first body", source = "test"),
        )
        val second = ReaderChapterContent(
            chapterId = 20L,
            title = "第二章",
            content = ReaderContent(title = "第二章", content = "second body", source = "test"),
        )
        val firstComment = ChapterComment(id = 1, chapterId = 10, authorName = "First", content = "one")
        val state = ReaderState(
            bookId = 7,
            chapterId = 10,
            chapterContents = listOf(first, second),
            comments = LoadResult.Success(listOf(firstComment)),
            chapterCommentStates = mapOf(
                10L to ReaderChapterCommentState(comments = LoadResult.Success(listOf(firstComment))),
            ),
        )

        val next = readerStateWithChapterCommentState(state, 20L) {
            it.copy(actionLoading = false, actionMessage = "评论已同步")
        }

        assertEquals(listOf(first, second), next.chapterContents)
        assertEquals(state.chapters, next.chapters)
        assertEquals(
            listOf(firstComment),
            (readerChapterCommentState(next, 10L).comments as LoadResult.Success<List<ChapterComment>>).value,
        )
        assertEquals("评论已同步", readerChapterCommentState(next, 20L).actionMessage)
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
    fun readerVisibleChapterUsesArticleAndInlineCommentKeysButIgnoresSentinels() {
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
        assertEquals(
            30L,
            readerFirstVisibleChapterId(
                listOf(
                    "reader-chapter-comments-30",
                    "reader-title-40",
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
    fun continuousReaderStatusUsesTheVisibleAppendedChapterInsteadOfTheInitialRouteChapter() {
        assertEquals(
            20L,
            readerStatusChapterId(
                routeChapterId = 10L,
                visibleChapterId = 20L,
            ),
        )
        assertEquals(
            10L,
            readerStatusChapterId(
                routeChapterId = 10L,
                visibleChapterId = null,
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
    fun disablingTtsHidesTheFeedbackSurfaceEvenAfterAnError() {
        assertTrue(readerTtsFeedbackVisible(showTts = true, state = ReaderTtsState.Error))
        assertFalse(readerTtsFeedbackVisible(showTts = false, state = ReaderTtsState.Error))
        assertFalse(readerTtsFeedbackVisible(showTts = false, state = ReaderTtsState.Speaking))
        assertFalse(readerTtsFeedbackVisible(showTts = true, state = ReaderTtsState.Stopped))
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

    @Test
    fun readerTopBarPrefersTheBookTitleAndHasAReaderFallback() {
        assertEquals("Sample Book", readerTopBarTitle("  Sample Book  "))
        assertEquals(readerTopBarLabels().title, readerTopBarTitle(null))
        assertEquals(readerTopBarLabels().title, readerTopBarTitle("   "))
    }

    @Test
    fun readerCatalogResolvesTheCurrentChapterIndexInSourceOrder() {
        val chapters = listOf(
            Chapter(id = 10L, title = "One", number = 1),
            Chapter(id = 20L, title = "Two", number = 2),
            Chapter(id = 30L, title = "Three", number = 3),
        )

        assertEquals(2, readerCatalogCurrentChapterIndex(chapters, currentChapterId = 30L))
        assertEquals(null, readerCatalogCurrentChapterIndex(chapters, currentChapterId = 999L))
    }

    @Test
    fun readerPageTurnsUseImmediateListMovement() {
        assertEquals(ReaderPageScrollMotion.Immediate, readerPageScrollMotion())
    }

    @Test
    fun pageTurnsSnapToAReaderItemBoundaryInsteadOfAViewportPixelFraction() {
        val firstPageItems = listOf(
            ReaderViewportItem(index = 0, offset = 0, size = 220),
            ReaderViewportItem(index = 1, offset = 220, size = 300),
            ReaderViewportItem(index = 2, offset = 520, size = 330),
            ReaderViewportItem(index = 3, offset = 850, size = 220),
        )

        assertEquals(
            3,
            readerPageScrollTargetIndex(
                direction = 1,
                firstVisibleItemIndex = 0,
                totalItemCount = 8,
                viewportStartOffset = 0,
                viewportEndOffset = 1_000,
                visibleItems = firstPageItems,
            ),
        )
        val laterPageItems = listOf(
            ReaderViewportItem(index = 3, offset = 0, size = 220),
            ReaderViewportItem(index = 4, offset = 220, size = 300),
            ReaderViewportItem(index = 5, offset = 520, size = 330),
            ReaderViewportItem(index = 6, offset = 850, size = 220),
        )
        assertEquals(
            0,
            readerPageScrollTargetIndex(
                direction = -1,
                firstVisibleItemIndex = 3,
                totalItemCount = 8,
                viewportStartOffset = 0,
                viewportEndOffset = 1_000,
                visibleItems = laterPageItems,
            ),
        )
    }

    @Test
    fun pageTurnsBeginWithTheFirstItemThatWouldOverflowThePreviousPage() {
        val currentPageItems = listOf(
            ReaderViewportItem(index = 0, offset = 0, size = 120),
            ReaderViewportItem(index = 8, offset = 800, size = 60),
            ReaderViewportItem(index = 9, offset = 860, size = 60),
            ReaderViewportItem(index = 10, offset = 920, size = 120),
        )

        assertEquals(
            10,
            readerPageScrollTargetIndex(
                direction = 1,
                firstVisibleItemIndex = 0,
                totalItemCount = 13,
                viewportStartOffset = 0,
                viewportEndOffset = 1_000,
                visibleItems = currentPageItems,
            ),
        )
    }

    @Test
    fun pageViewportHidesTheTrailingItemThatWouldOtherwiseBeSplitAcrossTwoPages() {
        val partiallyVisibleNextItem = listOf(
            ReaderViewportItem(index = 0, offset = 0, size = 220),
            ReaderViewportItem(index = 1, offset = 220, size = 300),
            ReaderViewportItem(index = 2, offset = 520, size = 330),
            ReaderViewportItem(index = 3, offset = 850, size = 220),
        )
        assertEquals(
            850,
            readerPageTrailingMaskStartOffset(
                viewportStartOffset = 0,
                viewportEndOffset = 1_000,
                visibleItems = partiallyVisibleNextItem,
            ),
        )
        assertNull(
            readerPageTrailingMaskStartOffset(
                viewportStartOffset = 0,
                viewportEndOffset = 1_000,
                visibleItems = partiallyVisibleNextItem.dropLast(1),
            ),
        )
    }

    @Test
    fun pageHistoryReturnsToTheExactPriorPageInsteadOfEstimatingByVisibleItemCount() {
        assertEquals(
            4,
            readerPageBackwardTargetIndex(
                pageStartHistory = listOf(0, 4),
                fallbackTargetIndex = 0,
            ),
        )
        assertEquals(
            2,
            readerPageBackwardTargetIndex(
                pageStartHistory = emptyList(),
                fallbackTargetIndex = 2,
            ),
        )
    }

    @Test
    fun fullscreenReaderKeepsDynamicSystemBarProtectionForTransientBars() {
        assertTrue(readerFullscreenArticleUsesSystemBarInsets(isFullscreen = true))
        assertFalse(readerFullscreenArticleUsesSystemBarInsets(isFullscreen = false))
    }

    @Test
    fun readerTextSelectionUsesOneArticleScopeRatherThanOneScopePerParagraph() {
        assertEquals(ReaderSelectionScope.Article, readerSelectionScope())
    }

    @Test
    fun everyReaderChapterStartsWithItsCommentSectionCollapsedLikeTheWebsite() {
        assertTrue(readerChapterCommentsDefaultCollapsed(pageTurnMode = true, useInfiniteScroll = false))
        assertTrue(readerChapterCommentsDefaultCollapsed(pageTurnMode = false, useInfiniteScroll = true))
        assertTrue(readerChapterCommentsDefaultCollapsed(pageTurnMode = false, useInfiniteScroll = false))
        assertEquals("展开评论 (3)", readerChapterCommentsToggleLabel(collapsed = true, commentCount = 3))
        assertEquals("收起评论", readerChapterCommentsToggleLabel(collapsed = false, commentCount = 3))
        assertEquals("暂无评论，点击展开后写评论", readerChapterCommentsCollapsedSummary(0))
        assertEquals("已有 3 条评论，点击展开查看", readerChapterCommentsCollapsedSummary(3))
    }

    @Test
    fun readerLayoutOverviewUsesReadingWidthRatherThanRawDp() {
        assertEquals("舒适宽度", readerLayoutWidthLabel(contentWidthDp = 800))
        assertEquals("全宽", readerLayoutWidthLabel(contentWidthDp = 1_200))
        assertEquals(
            "舒适宽度 · 翻页模式",
            readerLayoutOverviewSummary(contentWidthDp = 800, pageTurnMode = true),
        )
        assertEquals(
            "全宽 · 滚动模式",
            readerLayoutOverviewSummary(contentWidthDp = 1_200, pageTurnMode = false),
        )
    }

    @Test
    fun readerWidthControlUsesTheSameReaderNamesAsTheLayoutOverview() {
        assertEquals("窄", readerContentWidthControlLabel(contentWidthDp = 480))
        assertEquals("舒适宽度", readerContentWidthControlLabel(contentWidthDp = 800))
        assertEquals("宽", readerContentWidthControlLabel(contentWidthDp = 1_000))
        assertEquals("全宽", readerContentWidthControlLabel(contentWidthDp = 1_200))
    }

    @Test
    fun readerLayoutOverviewMakesVolumeKeyPagingDiscoverable() {
        assertEquals("音量键翻页开", readerVolumeKeyPagingOverviewTag(enabled = true))
        assertEquals("音量键翻页关", readerVolumeKeyPagingOverviewTag(enabled = false))
    }

    @Test
    fun simulatedPageTurnsUseAHorizontalCoverInsteadOfAVerticalScrollEffect() {
        assertEquals(
            ReaderPageTurnVisualMode.HorizontalCover,
            readerPageTurnVisualMode("simulated"),
        )
        assertEquals(
            ReaderPageTurnVisualMode.ListViewport,
            readerPageTurnVisualMode("fade"),
        )
    }

    @Test
    fun fullscreenReaderRemovesTheParentScaffoldInsetBudget() {
        assertTrue(readerScaffoldUsesEdgeToEdgeInsets(isReaderRoute = true, isReaderFullscreen = true))
        assertFalse(readerScaffoldUsesEdgeToEdgeInsets(isReaderRoute = true, isReaderFullscreen = false))
        assertFalse(readerScaffoldUsesEdgeToEdgeInsets(isReaderRoute = false, isReaderFullscreen = true))
    }

    @Test
    fun readerVolumeKeysMapToPagesWhileTheReaderIsActive() {
        assertEquals(
            ReaderVolumeKeyAction.PreviousPage,
            readerVolumeKeyAction(
                keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                action = KeyEvent.ACTION_DOWN,
                repeatCount = 0,
                readerActive = true,
            ),
        )
        assertEquals(
            ReaderVolumeKeyAction.NextPage,
            readerVolumeKeyAction(
                keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                action = KeyEvent.ACTION_DOWN,
                repeatCount = 0,
                readerActive = true,
            ),
        )
        assertEquals(
            ReaderVolumeKeyAction.Consume,
            readerVolumeKeyAction(
                keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                action = KeyEvent.ACTION_DOWN,
                repeatCount = 1,
                readerActive = true,
            ),
        )
        assertEquals(
            ReaderVolumeKeyAction.NextPage,
            readerVolumeKeyAction(
                keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                action = KeyEvent.ACTION_DOWN,
                repeatCount = 0,
                readerActive = true,
            ),
        )
        assertEquals(
            ReaderVolumeKeyAction.Ignore,
            readerVolumeKeyAction(
                keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                action = KeyEvent.ACTION_DOWN,
                repeatCount = 0,
                readerActive = false,
            ),
        )
    }

    @Test
    fun disabledVolumeKeyPagingReleasesBothVolumeKeysToTheSystem() {
        assertEquals(
            ReaderVolumeKeyAction.Ignore,
            readerVolumeKeyAction(
                keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                action = KeyEvent.ACTION_DOWN,
                repeatCount = 0,
                readerActive = true,
                volumeKeyPagingEnabled = false,
            ),
        )
        assertEquals(
            ReaderVolumeKeyAction.Ignore,
            readerVolumeKeyAction(
                keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                action = KeyEvent.ACTION_UP,
                repeatCount = 0,
                readerActive = true,
                volumeKeyPagingEnabled = false,
            ),
        )
    }

    @Test
    fun readerPageTurnOffersAnExplicitNoAnimationMode() {
        assertEquals(0, readerPageAnimationDurationMs("none"))
        assertEquals(160, readerPageAnimationDurationMs("fade"))
        assertEquals(
            ReaderPageTurnVisualMode.ListViewport,
            readerPageTurnVisualMode("none"),
        )
        assertEquals(
            ReaderPageTurnVisualMode.HorizontalCover,
            readerPageTurnVisualMode("simulated"),
        )
    }

    @Test
    fun readerSystemBarIconsFollowTheActiveReaderPaper() {
        assertTrue(readerSystemBarUsesDarkIcons(Color.White))
        assertTrue(readerSystemBarUsesDarkIcons(Color(0xFFF4ECD8)))
        assertFalse(readerSystemBarUsesDarkIcons(Color.Black))
    }

    @Test
    fun rejectedChapterReplyKeepsDraftAndTargetForRetry() {
        val state = ReaderChapterCommentState(
            draft = "@Reply User retry",
            replyingToCommentId = 4925,
            replyingToName = "Reply User",
            actionLoading = true,
        )

        val next = readerChapterCommentAfterSubmission(
            state,
            Result.success(ForumActionResult(success = false, message = "评论接口拒绝")),
        )

        assertEquals("@Reply User retry", next.draft)
        assertEquals(4925L, next.replyingToCommentId)
        assertEquals("Reply User", next.replyingToName)
        assertFalse(next.actionLoading)
        assertEquals("评论接口拒绝", next.actionMessage)
    }

    @Test
    fun failedChapterReplyKeepsDraftAndTargetForRetry() {
        val state = ReaderChapterCommentState(
            draft = "@Reply User retry",
            replyingToCommentId = 4925,
            replyingToName = "Reply User",
            actionLoading = true,
        )

        val next = readerChapterCommentAfterSubmission(
            state,
            Result.failure(IllegalStateException("network")),
        )

        assertEquals("@Reply User retry", next.draft)
        assertEquals(4925L, next.replyingToCommentId)
        assertEquals("Reply User", next.replyingToName)
        assertFalse(next.actionLoading)
        assertEquals("章节评论提交失败：network", next.actionMessage)
    }

    @Test
    fun collapsedChapterCommentsHideTheComposerAndExistingThreadsUntilExpanded() {
        val comment = ChapterComment(
            id = 1,
            chapterId = 9,
            authorName = "读者",
            content = "已有评论",
        )

        val collapsed = readerChapterCommentsUiVisibility(
            comments = LoadResult.Success(listOf(comment)),
            collapsed = true,
        )
        val empty = readerChapterCommentsUiVisibility(
            comments = LoadResult.Success(emptyList()),
            collapsed = false,
        )
        val collapsedEmpty = readerChapterCommentsUiVisibility(
            comments = LoadResult.Success(emptyList()),
            collapsed = true,
        )

        assertFalse(collapsed.showComposer)
        assertFalse(collapsed.showExistingThreads)
        assertTrue(empty.showComposer)
        assertFalse(empty.showExistingThreads)
        assertFalse(collapsedEmpty.showComposer)
        assertFalse(collapsedEmpty.showExistingThreads)
    }
}
