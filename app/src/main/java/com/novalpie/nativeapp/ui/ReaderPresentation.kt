package com.novalpie.nativeapp.ui

import android.view.KeyEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.novalpie.nativeapp.data.ReaderSettingsStore
import com.novalpie.nativeapp.model.Chapter
import com.novalpie.nativeapp.model.ChapterComment
import com.novalpie.nativeapp.model.ForumActionResult
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.ReaderChapterCacheState
import com.novalpie.nativeapp.model.ReaderChapterContent
import com.novalpie.nativeapp.model.ReaderContent
import com.novalpie.nativeapp.model.ReaderTapArea
import kotlin.math.abs

/** Keep a chapter-comment composer retryable when the source rejects or loses a submission. */
internal fun readerChapterCommentAfterSubmission(
    state: ReaderChapterCommentState,
    result: Result<ForumActionResult>,
): ReaderChapterCommentState = result.fold(
    onSuccess = { action ->
        if (!action.success) {
            state.copy(
                actionLoading = false,
                actionMessage = action.message ?: "章节评论提交失败",
            )
        } else {
            state.copy(
                draft = "",
                replyingToCommentId = null,
                replyingToName = null,
                actionLoading = false,
                actionMessage = action.message ?: "评论已提交",
            )
        }
    },
    onFailure = {
        state.copy(
            actionLoading = false,
            actionMessage = "章节评论提交失败：${it.message ?: "未知错误"}",
        )
    },
)

/** The compact reader header shows the work title; chapter progress belongs in the footer. */
internal fun readerTopBarTitle(bookTitle: String?): String =
    bookTitle?.trim()?.takeIf(String::isNotBlank) ?: readerTopBarLabels().title

/** Resolves the source-order item that should be visible when the unfiltered catalog opens. */
internal fun readerCatalogCurrentChapterIndex(
    chapters: List<Chapter>,
    currentChapterId: Long,
): Int? = chapters.indexOfFirst { it.id == currentChapterId }.takeIf { it >= 0 }

/** Page mode moves the viewport immediately; only the optional visual veil may animate. */
internal enum class ReaderPageScrollMotion {
    Immediate,
}

internal fun readerPageScrollMotion(): ReaderPageScrollMotion = ReaderPageScrollMotion.Immediate

/** The simulated turn is rendered by a horizontal opaque page cover, not a vertical list fade. */
internal enum class ReaderPageTurnVisualMode {
    ListViewport,
    HorizontalCover,
}

internal fun readerPageTurnVisualMode(effect: String): ReaderPageTurnVisualMode =
    if (effect == "simulated") {
        ReaderPageTurnVisualMode.HorizontalCover
    } else {
        ReaderPageTurnVisualMode.ListViewport
    }

/** Only the fullscreen reader may remove the root Scaffold's system-bar inset budget. */
internal fun readerScaffoldUsesEdgeToEdgeInsets(
    isReaderRoute: Boolean,
    isReaderFullscreen: Boolean,
): Boolean = isReaderRoute && isReaderFullscreen

/** Volume keys turn the reader viewport while a readable reader route is active. */
internal enum class ReaderVolumeKeyAction {
    Ignore,
    Consume,
    PreviousPage,
    NextPage,
}

internal fun readerVolumeKeyAction(
    keyCode: Int,
    action: Int,
    repeatCount: Int,
    readerActive: Boolean,
    volumeKeyPagingEnabled: Boolean = true,
): ReaderVolumeKeyAction {
    if (
        !readerActive ||
        !volumeKeyPagingEnabled ||
        (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN)
    ) {
        return ReaderVolumeKeyAction.Ignore
    }
    if (action != KeyEvent.ACTION_DOWN || repeatCount != 0) {
        return ReaderVolumeKeyAction.Consume
    }
    return if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
        ReaderVolumeKeyAction.PreviousPage
    } else {
        ReaderVolumeKeyAction.NextPage
    }
}

/** Android's light-bar appearance flag means the bar has dark icons on a light paper background. */
internal fun readerSystemBarUsesDarkIcons(background: Color): Boolean = background.luminance() >= 0.5f

/**
 * Publishes the reader body without replacing auxiliary state. Comments, favourites, and the
 * chapter directory are independent source requests; a slow one must not blank an already
 * readable chapter or make infinite-scroll recovery wait for an unrelated panel.
 */
internal fun readerStateAfterCoreContent(
    current: ReaderState,
    content: LoadResult<ReaderContent>,
    chapterContents: List<ReaderChapterContent>,
    contentFromCache: Boolean,
): ReaderState = current.copy(
    content = content,
    chapterContents = chapterContents,
    contentFromCache = contentFromCache,
)

internal data class ReaderTextLayout(
    val fontSizeSp: Int,
    val lineHeightSp: Float,
    val firstLineIndentSp: Float,
    val paragraphSpacingDp: Float,
    val horizontalPaddingDp: Float,
    val titleFontSizeSp: Float,
    val titleBottomSpacingDp: Float,
    val contentWidthDp: Int = 800,
    val fontFamily: String = "system",
    val fontWeight: Int = 400,
    val letterSpacingSp: Float = 0f,
    val wordSpacingSp: Float = 0f,
)

/** The website uses an 800px article rail on wide screens but keeps mobile reading edge-to-edge. */
internal fun readerContentMaxWidthDp(viewportWidthDp: Float): Float? =
    800f.takeIf { viewportWidthDp > it }

/**
 * Mirrors the live mobile reader CSS: 16px body / 1.6 line-height, 16px page gutters, a 20px
 * chapter title, and 20px paragraph rhythm. The selected body size remains user-owned.
 */
internal fun readerTextLayout(fontSizeSp: Int): ReaderTextLayout {
    val safeFontSize = fontSizeSp.coerceIn(
        ReaderSettingsStore.MIN_FONT_SIZE_SP,
        ReaderSettingsStore.MAX_FONT_SIZE_SP,
    )
    return ReaderTextLayout(
        fontSizeSp = safeFontSize,
        lineHeightSp = safeFontSize * 1.6f,
        firstLineIndentSp = safeFontSize * 2f,
        paragraphSpacingDp = 20f,
        horizontalPaddingDp = 16f,
        titleFontSizeSp = 20f,
        titleBottomSpacingDp = 24f,
    )
}

internal fun readerTextLayout(options: ReaderUiOptions): ReaderTextLayout {
    val safe = options.normalizedReaderOptions()
    val base = readerTextLayout(safe.fontSizeSp)
    return base.copy(
        lineHeightSp = safe.fontSizeSp * safe.lineHeight,
        firstLineIndentSp = if (safe.textIndent) safe.fontSizeSp * 2f else 0f,
        paragraphSpacingDp = if (safe.emptyLine) 20f else 4f,
        contentWidthDp = safe.contentWidthDp,
        fontFamily = safe.fontFamily,
        fontWeight = safe.fontWeight,
        letterSpacingSp = safe.letterSpacing,
        wordSpacingSp = safe.wordSpacing,
    )
}

/** Compose does not have a separate word-spacing TextStyle field; hair spaces preserve CJK text. */
internal fun readerTextWithWordSpacing(value: String, wordSpacingSp: Float): String {
    if (wordSpacingSp == 0f) return value
    val hairSpaces = when {
        wordSpacingSp >= 6f -> "\u200A\u200A\u200A"
        wordSpacingSp >= 3f -> "\u200A\u200A"
        wordSpacingSp > 0f -> "\u200A"
        wordSpacingSp <= -1f -> ""
        else -> " "
    }
    return value.replace(Regex("(?<=\\p{L})[ ](?=\\p{L})")) { hairSpaces }
}

/**
 * The mobile source keeps a narrow title/status rail, then places reader controls vertically at
 * the right edge.  Keeping these measurements in presentation code makes the Compose layout and
 * its interaction tests share the same source-derived contract.
 */
internal data class ReaderChromeLayout(
    val headerHeightDp: Float = 32f,
    val statusHeightDp: Float = 26f,
    val sidePaddingDp: Float = 12f,
)

internal fun readerChromeLayout(): ReaderChromeLayout = ReaderChromeLayout()

/** The source sidebar uses a slim full-height vertical rail on phones. */
internal fun readerActionRailWidthDp(): Float = 64f

/** The side drawer occupies the remaining readable width without becoming a tablet-wide sheet. */
internal fun readerSidePanelWidthFraction(): Float = 0.84f

internal fun readerSidePanelMaxWidthDp(): Float = 440f

/**
 * A loaded chapter is immersive by default. Loading and error states keep navigation visible so
 * the reader never traps someone behind an empty canvas.
 */
internal fun readerChromeVisible(hasReadableBody: Boolean, controlsRequested: Boolean): Boolean =
    controlsRequested || !hasReadableBody

/** The article only reserves space for the overlaid top rail while that rail is visible. */
internal fun readerContentTopPaddingDp(
    chromeVisible: Boolean,
    chromeLayout: ReaderChromeLayout = readerChromeLayout(),
): Float = if (chromeVisible) chromeLayout.headerHeightDp else 0f

/** Resolves a source-style tap rail against a normalized horizontal coordinate. */
internal fun readerTapAreaAt(
    areas: List<ReaderTapArea>,
    xFraction: Float,
): ReaderTapArea? {
    val ordered = listOf("left", "center", "right").mapNotNull { position ->
        areas.firstOrNull { it.position == position }
    }
    if (ordered.size != 3) return null
    val widths = ordered.map { readerTapWidthFraction(it.width) }
    val total = widths.sum().takeIf { it > 0f } ?: 1f
    val normalized = widths.map { it / total }
    val x = xFraction.coerceIn(0f, 0.999999f)
    var cursor = 0f
    normalized.forEachIndexed { index, width ->
        cursor += width
        if (x < cursor) return ordered[index]
    }
    return ordered.last()
}

/** A drag must cross the system touch-slop before the reader treats it as a scroll rather than a tap. */
internal fun readerTapExceedsTouchSlop(distancePx: Float, touchSlopPx: Float): Boolean =
    distancePx > touchSlopPx.coerceAtLeast(1f)

/**
 * Text selection consumes the final pointer pass. A normal short press must still reach the
 * reader chrome, while a drag or platform-length press remains available for selection.
 */
internal fun readerBodyTapIsEligible(
    moved: Boolean,
    durationMillis: Long,
    longPressTimeoutMillis: Long,
): Boolean = !moved && durationMillis < longPressTimeoutMillis.coerceAtLeast(1L)

/**
 * Reader-wide chrome only owns ordinary article taps. Inline chapter-comment controls receive the
 * same pointer stream, so their press must not also open or dismiss the reader rail.
 */
internal fun readerBodyTapCanHandle(
    isShortBodyTap: Boolean,
    inlineInteractionInProgress: Boolean,
): Boolean = isShortBodyTap && !inlineInteractionInProgress

/** A quick second tap at the same point should not immediately undo the toolbar opened by the first one. */
internal fun readerShouldToggleChrome(
    lastToggleUptimeMillis: Long,
    currentUptimeMillis: Long,
    sameTapTarget: Boolean = true,
    debounceMillis: Long = READER_CHROME_TAP_DEBOUNCE_MILLIS,
): Boolean =
    lastToggleUptimeMillis <= 0L ||
        !sameTapTarget ||
        currentUptimeMillis - lastToggleUptimeMillis > debounceMillis.coerceAtLeast(0L)

/**
 * A fast double tap must never end by hiding the reader controls. A normal tap still toggles
 * immediately, while a repeated same-target tap within the guard window keeps an already-open
 * toolbar open. Tapping another paragraph remains an immediate dismissal.
 */
internal fun readerChromeVisibilityAfterBodyTap(
    controlsVisible: Boolean,
    lastToggleUptimeMillis: Long,
    currentUptimeMillis: Long,
    sameTapTarget: Boolean,
): Boolean = if (
    controlsVisible &&
    !readerShouldToggleChrome(
        lastToggleUptimeMillis = lastToggleUptimeMillis,
        currentUptimeMillis = currentUptimeMillis,
        sameTapTarget = sameTapTarget,
    )
) {
    true
} else {
    !controlsVisible
}

/**
 * Once reader chrome is open, a body tap must control the chrome before the configured page-turn
 * zones are considered. Otherwise a tap on the left/right body rail can become a no-op in
 * continuous mode and leave the dock stuck on screen.
 */
internal enum class ReaderBodyTapResolution {
    ApplyConfiguredTapArea,
    KeepChromeVisible,
    DismissChrome,
}

internal fun readerBodyTapResolution(
    controlsVisible: Boolean,
    lastToggleUptimeMillis: Long,
    currentUptimeMillis: Long,
    sameTapTarget: Boolean,
): ReaderBodyTapResolution {
    if (!controlsVisible) return ReaderBodyTapResolution.ApplyConfiguredTapArea
    return if (
        readerChromeVisibilityAfterBodyTap(
            controlsVisible = true,
            lastToggleUptimeMillis = lastToggleUptimeMillis,
            currentUptimeMillis = currentUptimeMillis,
            sameTapTarget = sameTapTarget,
        )
    ) {
        ReaderBodyTapResolution.KeepChromeVisible
    } else {
        ReaderBodyTapResolution.DismissChrome
    }
}

/** Distinguishes a physical double-tap from a reader moving to another paragraph to close chrome. */
internal fun readerChromeTapTargetsOverlap(
    previousXFraction: Float,
    previousYFraction: Float,
    currentXFraction: Float,
    currentYFraction: Float,
    toleranceFraction: Float = READER_CHROME_TAP_TARGET_TOLERANCE,
): Boolean {
    val tolerance = toleranceFraction.coerceIn(0f, 1f)
    return abs(previousXFraction - currentXFraction) <= tolerance &&
        abs(previousYFraction - currentYFraction) <= tolerance
}

/** The reader keeps the central rail as the reliable chrome toggle even if an old saved map is invalid. */
internal fun readerTapActionAt(areas: List<ReaderTapArea>, xFraction: Float): String? {
    val area = readerTapAreaAt(areas, xFraction)
    return when {
        area?.position == "center" -> "sidebar"
        area == null && xFraction in 0.30f..0.70f -> "sidebar"
        else -> area?.action
    }
}

/**
 * In page mode the left/right rail is a page-turn surface, even when the article currently shows
 * a clickable chapter-comment action beneath that rail. Consume that child gesture before it can
 * open the WebView fallback or submit another comment by accident. Continuous reading and an
 * already-open reader toolbar keep their ordinary child interactions.
 */
internal fun readerPageModeTapShouldInterceptChild(
    pageTurnEnabled: Boolean,
    controlsVisible: Boolean,
    action: String?,
): Boolean = pageTurnEnabled && !controlsVisible && action in setOf("pagePrev", "pageNext")

internal fun readerPageAnimationDurationMs(effect: String): Int = when (effect) {
    "none" -> 0
    "cover" -> 280
    "slide" -> 220
    "simulated" -> 360
    else -> 160
}

private fun readerTapWidthFraction(value: String): Float = when {
    value.trim().endsWith('%') -> value.trim().removeSuffix("%").toFloatOrNull()?.div(100f)
    else -> null
}?.coerceIn(0.05f, 0.9f) ?: (1f / 3f)

@Suppress("UNUSED_PARAMETER")
internal fun readerSourceDebugLine(source: String): String? = null

@Suppress("UNUSED_PARAMETER")
internal fun readerDebugIdentityLine(bookId: Long, chapterId: Long): String? = null

/** Exact mobile source action order: close, tabbed surfaces, chapter navigation, then utilities. */
internal fun readerActionRailLabels(): List<String> =
    readerRailActionSpecs().map(ReaderRailActionSpec::label)

/** Retained name for callers that previously consumed the reader control vocabulary. */
internal fun readerToolbarLabels(): List<String> = readerActionRailLabels()

/** The source calls the button 滑动 while scrolling and 翻页 while page turning is active. */
internal fun readerReadingModeActionLabel(pageTurnMode: Boolean): String =
    if (pageTurnMode) "翻页" else "滑动"

/** Navigation keeps only native reader exits; broken webpage-body fallback is intentionally absent. */
internal fun readerNavigationPanelLabels(showFavoriteAction: Boolean): List<String> =
    buildList {
        add("书本页")
        if (showFavoriteAction) add("收藏")
    }

/** Source drawer cache copy, backed by the native disk cache instead of a guessed remote state. */
internal fun readerCatalogCacheLabel(state: ReaderChapterCacheState): String = when (state) {
    ReaderChapterCacheState.Current -> "缓存最新"
    ReaderChapterCacheState.Stale -> "缓存待刷新"
    ReaderChapterCacheState.Missing -> "无缓存"
}

/** The source drawer uses the compact month marker rather than Book Detail's full date. */
internal fun readerCatalogUpdatedLabel(value: String?): String? {
    val normalized = value?.trim()?.takeIf(String::isNotBlank) ?: return null
    val month = READER_CATALOG_MONTH.find(normalized)?.groupValues?.getOrNull(1)?.toIntOrNull()
    return month?.let { "${it}月" } ?: normalized
}

internal fun readerTtsSystemSettingsLabel(): String = "系统设置"

/** Android's public Settings class does not expose this legacy system route as a constant. */
internal const val READER_TTS_SYSTEM_SETTINGS_ACTION = "com.android.settings.TTS_SETTINGS"

/** The TTS feedback surface belongs to the same user-visible switch as the rail entry. */
internal fun readerTtsFeedbackVisible(
    showTts: Boolean,
    state: ReaderTtsState,
): Boolean = showTts && state != ReaderTtsState.Stopped

/** Legacy presentation entry point now maps to the source-style vertical rail. */
internal fun readerCompactToolbarLabels(fontSizeSp: Int, theme: String): List<String> =
    readerActionRailLabels()

internal fun String.readerThemeLabel(): String = when (this) {
    "light" -> "明亮"
    "sepia" -> "护眼"
    "green" -> "绿色"
    "gray" -> "灰色"
    "dark" -> "深色"
    "high_contrast" -> "高对比"
    else -> "系统"
}

internal fun readerThemeLabel(theme: String, customThemes: List<com.novalpie.nativeapp.model.ReaderCustomTheme>): String =
    com.novalpie.nativeapp.model.readerCustomThemeIdFromKey(theme)
        ?.let { id -> customThemes.firstOrNull { it.id == id }?.name }
        ?: theme.readerThemeLabel()

internal fun readerThemeOptions(): List<Pair<String, String>> = listOf(
    "system" to "系统",
    "light" to "明亮",
    "sepia" to "护眼",
    "green" to "绿色",
    "gray" to "灰色",
    "dark" to "深色",
    "high_contrast" to "高对比",
)

internal fun readerReplaceModeOptions(): List<Pair<String, String>> = listOf(
    "" to "关闭替换",
    "korea" to "韩国模式",
    "india" to "印度模式",
    "europe" to "欧洲模式",
    "usa" to "美国模式",
    "hyrule" to "海拉鲁",
    "azeroth" to "艾泽拉斯",
    "tamriel" to "泰瑞尔",
    "middle_earth" to "中土世界",
    "terra" to "泰拉",
    "genshin" to "提瓦特",
)

internal fun String.readerReplaceModeLabel(): String =
    readerReplaceModeOptions().firstOrNull { it.first == this }?.second ?: "印度模式"

private val READER_CATALOG_MONTH = Regex("""\d{4}[-/](\d{1,2})[-/]\d{1,2}""")

/** Returns the LazyColumn item containing a TTS segment, using the same item ordering as the body. */
internal fun readerBodyItemIndexForText(
    contents: List<com.novalpie.nativeapp.model.ReaderChapterContent>,
    options: ReaderUiOptions,
    target: String,
): Int? {
    val query = target.trim().takeIf(String::isNotBlank) ?: return null
    var itemIndex = 0
    contents.forEach { chapter ->
        val blocks = readerBlocksForDisplay(readerBlocksForContent(chapter.content), options.removeDuplicateLines)
        if (!chapter.title.isNullOrBlank()) itemIndex++
        blocks.forEach { block ->
            when (block) {
                is ReaderContentBlock.Text -> {
                    if (block.value.contains(query) || query.contains(block.value.take(24))) {
                        return itemIndex
                    }
                    itemIndex++
                }
                is ReaderContentBlock.Image -> if (options.showImages) itemIndex++
            }
        }
        itemIndex++ // chapter finish marker
        if (options.showComments) itemIndex++ // this chapter's inline comments section
    }
    return null
}

/**
 * Start the safe read-only append while a few article items are still visible. Every chapter owns
 * one inline comments item before the current sentinel, so the live item count stays aligned with
 * TTS auto-scroll and continuous-reading ordering without reparsing the window on every frame.
 */
internal fun readerNextChapterPrefetchStartIndex(
    totalItemCount: Int,
    itemsAfterSentinel: Int = 0,
    lookAheadItems: Int = 3,
): Int = (
    totalItemCount.coerceAtLeast(0) -
        itemsAfterSentinel.coerceAtLeast(0) -
        1 - // sentinel
        lookAheadItems.coerceAtLeast(1)
    ).coerceAtLeast(0)

/** Lightweight form for scroll observers after the body boundary has been calculated once. */
internal fun readerShouldPrefetchNextChapter(
    lastVisibleItemIndex: Int,
    prefetchStartIndex: Int,
): Boolean = lastVisibleItemIndex >= prefetchStartIndex

/** Returns the comments belonging to one visible chapter without falling back to another chapter. */
internal fun readerChapterCommentState(
    state: ReaderState,
    chapterId: Long,
): ReaderChapterCommentState = state.chapterCommentStates[chapterId]
    ?: if (chapterId == state.chapterId) {
        ReaderChapterCommentState(
            comments = state.comments,
            draft = state.commentDraft,
            replyingToCommentId = state.replyingToCommentId,
            replyingToName = state.replyingToName,
            actionMessage = state.actionMessage,
            actionLoading = state.actionLoading,
        )
    } else {
        ReaderChapterCommentState()
    }

/**
 * Mutates only one chapter's comment state. The continuous reader window, scroll anchor,
 * loaded chapters and other chapter comment states remain untouched after a comment action.
 */
internal fun readerStateWithChapterCommentState(
    state: ReaderState,
    chapterId: Long,
    transform: (ReaderChapterCommentState) -> ReaderChapterCommentState,
): ReaderState {
    if (chapterId <= 0L) return state
    val current = readerChapterCommentState(state, chapterId)
    return state.copy(
        chapterCommentStates = state.chapterCommentStates + (chapterId to transform(current)),
    )
}

internal enum class ReaderPageBoundaryTarget {
    None,
    PreviousChapter,
    NextChapter,
}

/** The concrete adjacent chapter selected from the reader's currently visible route. */
internal data class ReaderAdjacentBoundaryRequest(
    val sourceChapterId: Long,
    val targetChapterId: Long,
)

internal fun readerAdjacentBoundaryRequest(
    currentChapterId: Long,
    chapters: List<Chapter>,
    target: ReaderPageBoundaryTarget,
): ReaderAdjacentBoundaryRequest? {
    val adjacent = adjacentReaderChapters(currentChapterId, chapters)
    val targetChapterId = when (target) {
        ReaderPageBoundaryTarget.PreviousChapter -> adjacent.previous?.id
        ReaderPageBoundaryTarget.NextChapter -> adjacent.next?.id
        ReaderPageBoundaryTarget.None -> null
    } ?: return null
    return ReaderAdjacentBoundaryRequest(
        sourceChapterId = currentChapterId,
        targetChapterId = targetChapterId,
    )
}

/** A page-boundary back step resumes the chapter at its terminal page; ordinary opens start at top. */
enum class ReaderChapterEntryPosition {
    Start,
    End,
}

internal fun readerChapterEntryPositionForPageBoundary(
    target: ReaderPageBoundaryTarget,
): ReaderChapterEntryPosition = when (target) {
    ReaderPageBoundaryTarget.PreviousChapter -> ReaderChapterEntryPosition.End
    ReaderPageBoundaryTarget.None,
    ReaderPageBoundaryTarget.NextChapter -> ReaderChapterEntryPosition.Start
}

/** A LazyColumn has no valid final index when its body has not composed yet. */
internal fun readerChapterEntryScrollIndex(
    entryPosition: ReaderChapterEntryPosition,
    itemCount: Int,
): Int = when (entryPosition) {
    ReaderChapterEntryPosition.Start -> 0
    ReaderChapterEntryPosition.End -> (itemCount - 1).coerceAtLeast(0)
}

internal fun readerPageBoundaryTarget(
    direction: Int,
    reachedBoundary: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
): ReaderPageBoundaryTarget {
    if (!reachedBoundary) return ReaderPageBoundaryTarget.None
    return when {
        direction < 0 && hasPrevious -> ReaderPageBoundaryTarget.PreviousChapter
        direction > 0 && hasNext -> ReaderPageBoundaryTarget.NextChapter
        else -> ReaderPageBoundaryTarget.None
    }
}

/** A boundary route replacement must be dispatched once per physical tap sequence. */
internal fun readerBoundaryNavigationCanStart(
    inProgress: Boolean,
    target: ReaderPageBoundaryTarget,
): Boolean = !inProgress && target != ReaderPageBoundaryTarget.None

/**
 * A route replacement can recompose the reader before the original pointer release has fully
 * drained from Compose's input pipeline.  Keep the physical gesture identity in the gate so the
 * replacement chapter cannot consume that same release as another page turn.
 */
internal fun readerPageTurnGestureCanStart(
    inProgress: Boolean,
    target: ReaderPageBoundaryTarget,
    gestureId: Long,
    lastHandledGestureId: Long?,
): Boolean = readerBoundaryNavigationCanStart(inProgress, target) && gestureId != lastHandledGestureId

internal fun readerContinuousScrollCanTrigger(
    continuousScrollEnabled: Boolean,
    hasReadableBody: Boolean,
): Boolean = continuousScrollEnabled && hasReadableBody

/**
 * Keeps a reached article boundary alive while the catalog or a previous append is changing
 * state.  A LazyColumn visibility snapshot is edge-triggered, so dropping that one event while
 * the catalog is Loading could otherwise leave the reader at the end of a chapter with no later
 * opportunity to start the next safe GET.
 */
internal fun readerContinuousScrollCanRequestNext(
    continuousScrollEnabled: Boolean,
    hasReadableBody: Boolean,
    boundaryReached: Boolean,
    catalogReady: Boolean,
    loadingNextChapter: Boolean,
    nextChapterError: String?,
    nextChapterWaitingForCatalog: Boolean,
    nextChapterExhausted: Boolean,
): Boolean =
    readerContinuousScrollCanTrigger(continuousScrollEnabled, hasReadableBody) &&
        boundaryReached &&
        catalogReady &&
        !loadingNextChapter &&
        nextChapterError == null &&
        !nextChapterWaitingForCatalog &&
        !nextChapterExhausted

/**
 * A normal center tap is the reader's default route to its toolbars.  Radial controls remain an
 * explicit opt-in for people who prefer them; making it the default previously turned a simple
 * reader action into a fragile double-tap that could be confused with a scroll ending.
 */
internal fun readerUsesRadialMenu(showRadialMenu: Boolean): Boolean = showRadialMenu

/**
 * The sentinel is replaced whenever the continuous window grows. A static key can remain marked
 * visible across a LazyColumn remeasure, suppressing the next visibility emission after an append.
 */
internal fun readerBodyEndSentinelKey(
    contents: List<com.novalpie.nativeapp.model.ReaderChapterContent>,
): String {
    val last = contents.lastOrNull()?.chapterId ?: 0L
    return "reader-body-end-sentinel-$last-${contents.size}"
}

/**
 * Returns the chapter at the top of the reader viewport.  Inline comments belong to the chapter
 * immediately above them, so they must keep that chapter's footer/progress label alive instead of
 * making the reader fall back to the route's opening chapter between appended bodies.
 */
internal fun readerFirstVisibleChapterId(itemKeys: Iterable<Any?>): Long? =
    itemKeys.firstNotNullOfOrNull(::readerChapterIdFromItemKey)

/**
 * A continuous reader keeps its navigation route anchored to the chapter that opened the window,
 * but its footer must describe the article currently on screen after later chapters are appended.
 */
internal fun readerStatusChapterId(routeChapterId: Long, visibleChapterId: Long?): Long =
    visibleChapterId?.takeIf { it > 0L } ?: routeChapterId

internal fun readerChapterIdFromItemKey(key: Any?): Long? {
    val value = key as? String ?: return null
    val prefix = listOf(
        "reader-title-",
        "reader-text-",
        "reader-image-",
        "reader-chapter-finish-",
        "reader-chapter-comments-",
    ).firstOrNull(value::startsWith) ?: return null
    val remainder = value.removePrefix(prefix)
    return remainder.substringBefore('-').toLongOrNull()
}

internal fun readerCatalogPanelTitle(): String = "目录"

internal fun readerCloseCatalogLabel(): String = "关闭"

internal fun readerSurfaceSections(): List<String> =
    listOf("帮助", "目录", "设置", "主题", "导航")

internal data class ReaderTopBarLabels(
    val title: String,
)

internal fun readerTopBarLabels(): ReaderTopBarLabels =
    ReaderTopBarLabels(title = "阅读")

internal fun globalProductTopBarVisible(route: AppRoute): Boolean =
    route !is AppRoute.Reader &&
    route !is AppRoute.Forum &&
    route !is AppRoute.ForumPostDetail &&
    route !is AppRoute.Home &&
    route !is AppRoute.Search &&
    route !is AppRoute.Tools &&
        route !is AppRoute.Profile

// Compose can deliver the releases in a physical double tap farther apart than the input API
// interval. Pair the full guard with a tap-target comparison so a different paragraph can still
// close the chrome immediately.
internal const val READER_CHROME_TAP_DEBOUNCE_MILLIS = 600L
internal const val READER_CHROME_TAP_TARGET_TOLERANCE = 0.08f

internal fun chapterCommentMetricLabels(comment: ChapterComment): List<String> =
    listOf(
        "赞 ${comment.likeCount ?: 0}",
        "踩 ${comment.dislikeCount ?: 0}",
        "表情 ${comment.reactionCount ?: 0}",
        "打赏 ${comment.awardPoints ?: 0}",
        "回复"
    )

internal fun chapterCommentsSectionTitle(): String = "章节评论"

internal fun chapterCommentsFallbackLabel(): String = "打开网页评论"
