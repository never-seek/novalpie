package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ChapterComment
import com.novalpie.nativeapp.model.BookEditPermissions
import com.novalpie.nativeapp.model.Chapter

internal data class ChapterListPresentation(
    val numberLabel: String,
    val updatedLabel: String?,
    val metrics: List<String>,
)

/** The source detail catalogue and reader sidebar use different number and metric copy. */
internal enum class ChapterListContext {
    BookDetail,
    ReaderCatalog,
}

/** The mobile source exposes an explicit direction switch above the chapter list. */
internal enum class BookDetailCatalogOrder {
    Ascending,
    Descending,
}

internal fun sortBookDetailChapters(
    chapters: List<Chapter>,
    order: BookDetailCatalogOrder,
): List<Chapter> = when (order) {
    BookDetailCatalogOrder.Ascending -> chapters
    BookDetailCatalogOrder.Descending -> chapters.asReversed()
}

internal fun bookDetailCatalogHeading(chapterCount: Int): String =
    "正文卷 · 共 ${chapterCount.coerceAtLeast(0)} 章"

/**
 * The directory response is authoritative once it contains rows. During a transient catalogue
 * failure, retain the detail endpoint's advertised count instead of presenting a false zero.
 */
internal fun bookDetailDisplayedChapterCount(
    loadedChapters: List<Chapter>,
    sourceChapterCount: Int?,
): Int = loadedChapters.size.takeIf { it > 0 }
    ?: sourceChapterCount?.coerceAtLeast(0)
    ?: 0

/** A disabled source download policy must override an otherwise authenticated session. */
internal fun bookDetailAllowsNativeEpubDownload(
    hasAuthToken: Boolean,
    allowDownload: Boolean?,
): Boolean = hasAuthToken && allowDownload != false

/** The sole EPUB item in native Book Detail writes to Android Downloads, never to a WebView. */
internal fun nativeEpubDownloadMenuLabel(isDownloading: Boolean): String =
    if (isDownloading) "正在原生下载 EPUB…" else "原生下载 EPUB（保存到下载）"

/** TXT uses the source's own download authorization, then streams straight into Android Downloads. */
internal fun nativeTxtDownloadMenuLabel(isDownloading: Boolean): String =
    if (isDownloading) "正在原生下载 TXT…" else "原生下载 TXT（保存到下载）"

internal fun bookDetailTabLabel(
    tab: BookDetailContentTabLabel,
    chapterCount: Int,
): String = when (tab) {
    BookDetailContentTabLabel.Introduction -> "简介"
    BookDetailContentTabLabel.Catalog -> "目录 ${chapterCount.coerceAtLeast(0)}"
    BookDetailContentTabLabel.Comments -> "评论"
}

/** A UI-independent tab contract keeps source labels testable without composing the screen. */
internal enum class BookDetailContentTabLabel {
    Introduction,
    Catalog,
    Comments,
}

/** Native detail tabs are mutually exclusive so long catalogs never bury the book review panel. */
internal fun bookDetailVisibleContentSections(
    selected: BookDetailContentTabLabel,
): List<BookDetailContentTabLabel> = listOf(selected)

/**
 * Mirrors the source directory row without inventing a cache state: the public chapter endpoint
 * gives us episode number, update time, word count and illustration count, but not per-user cache
 * availability.
 */
internal fun chapterListPresentation(
    chapter: Chapter,
    context: ChapterListContext = ChapterListContext.BookDetail,
): ChapterListPresentation =
    ChapterListPresentation(
        numberLabel = chapter.number?.takeIf { it > 0 }?.let { number ->
            if (context == ChapterListContext.ReaderCatalog) "第${number}章" else "EP.$number"
        } ?: "章节",
        updatedLabel = if (context == ChapterListContext.BookDetail) {
            sourceChapterUpdatedLabel(chapter.updatedAt)
        } else {
            null
        },
        metrics = listOfNotNull(
            chapter.wordCount?.takeIf { it >= 0 }?.let { wordCount ->
                formatSourceChapterCount(wordCount) + if (context == ChapterListContext.BookDetail) "字" else ""
            },
            chapter.imageCount?.takeIf { it > 0 }?.let { "${it}图" },
        ),
    )

internal fun formatSourceChapterCount(value: Long): String = when {
    value < 1_000L -> value.toString()
    value < 1_000_000L -> formatSourceChapterDecimal(value, 1_000L) + "K"
    else -> formatSourceChapterDecimal(value, 1_000_000L) + "M"
}

internal fun sourceChapterUpdatedLabel(value: String?): String? {
    val normalized = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val match = SOURCE_CHAPTER_DATE_TIME.find(normalized) ?: return normalized
    val (year, month, day, hour, minute) = match.destructured
    return "${year}年${month.padStart(2, '0')}月${day.padStart(2, '0')}日 ${hour.padStart(2, '0')}:${minute.padStart(2, '0')}"
}

private fun formatSourceChapterDecimal(value: Long, unit: Long): String {
    val tenths = (value * 10L + unit / 2L) / unit
    val whole = tenths / 10L
    val fraction = tenths % 10L
    return if (fraction == 0L) whole.toString() else "$whole.$fraction"
}

private val SOURCE_CHAPTER_DATE_TIME = Regex(
    """(\d{4})[-/](\d{1,2})[-/](\d{1,2})[T\s]+(\d{1,2}):(\d{2})""",
)

/**
 * Management controls are not ordinary book-detail actions. The website decides access per
 * field, so a viewer sees none while an administrator or a permitted owner retains the controls.
 */
internal fun bookManagementActionsVisible(permissions: BookEditPermissions?): Boolean =
    permissions?.let {
        it.title || it.titleTranslation || it.authorName || it.description || it.source ||
            it.sourceUrl || it.language || it.isAdult || it.photoUrl || it.spans || it.tags
    } == true

internal fun bookDetailPrimaryActions(hasProgress: Boolean): List<String> =
    if (hasProgress) {
        listOf("继续阅读", "开始阅读", "网页详情")
    } else {
        listOf("开始阅读", "网页详情")
    }

internal fun bookDetailFavoriteLabel(isFavorited: Boolean): String =
    if (isFavorited) "已收藏" else "未收藏"

internal fun bookDetailFavoriteLoadingLabel(): String = "收藏同步中"

internal fun bookDetailFavoriteUnavailableLabel(): String = "收藏状态不可用"

internal fun bookCommentMetricLabels(comment: ChapterComment): List<String> =
    chapterCommentMetricLabels(comment)

internal fun bookCommentsSectionTitle(): String = "书评"

internal fun bookCommentsFallbackLabel(): String = "打开网页书评"

/** A book/chapter comment thread is the same parent/reply contract used by the source forum. */
internal data class ChapterCommentThread(
    val comment: ChapterComment,
    val replies: List<ChapterComment> = emptyList(),
)

internal fun chapterCommentThreads(comments: List<ChapterComment>): List<ChapterCommentThread> {
    if (comments.isEmpty()) return emptyList()
    val byId = comments.associateBy(ChapterComment::id)
    val roots = linkedMapOf<Long, ChapterComment>()
    val repliesByRoot = linkedMapOf<Long, MutableList<ChapterComment>>()

    comments.forEach { comment ->
        val root = chapterCommentRoot(comment, byId)
        if (!roots.containsKey(root.id)) roots[root.id] = root
        if (root.id != comment.id) {
            repliesByRoot.getOrPut(root.id) { mutableListOf() }.add(comment)
        }
    }

    return roots.values.map { root ->
        ChapterCommentThread(comment = root, replies = repliesByRoot[root.id].orEmpty())
    }
}

internal fun chapterCommentThreadSummary(
    threads: List<ChapterCommentThread>,
    rootLabel: String,
): String {
    val replies = threads.sumOf { thread -> thread.replies.size }
    return "${threads.size} 条$rootLabel · $replies 条回复"
}

private fun chapterCommentRoot(
    comment: ChapterComment,
    byId: Map<Long, ChapterComment>,
): ChapterComment {
    val visited = mutableSetOf<Long>()
    var current = comment
    while (visited.add(current.id)) {
        val parentId = current.parentCommentId ?: return current
        current = byId[parentId] ?: return current
    }
    return comment
}
