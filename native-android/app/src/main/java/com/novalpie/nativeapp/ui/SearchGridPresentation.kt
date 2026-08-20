package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.NovelCard
import kotlin.math.ceil
import kotlin.math.floor

// These values mirror the compact source tag pills used by the native grid: 8dp horizontal
// padding, 2dp gaps, and one 20dp label line with a 2dp inter-line gap.
internal const val SEARCH_GRID_PAGE_HORIZONTAL_PADDING_DP = 32
internal const val SEARCH_GRID_CELL_GAP_DP = 12
internal const val SEARCH_GRID_CARD_HORIZONTAL_PADDING_DP = 16
internal const val SEARCH_GRID_TAG_LINE_HEIGHT_DP = 22
internal const val SEARCH_GRID_TAG_MIN_AREA_HEIGHT_DP = 68

/** Lazy layouts save item keys in a Bundle; keep row keys String-backed instead of data classes. */
internal const val SEARCH_GRID_ROW_KEY_PREFIX = "search-row:"

internal fun searchGridRowKey(bookIds: List<Long>): String =
    SEARCH_GRID_ROW_KEY_PREFIX + bookIds.joinToString(separator = ",")

internal fun searchGridRowBookIds(key: Any?): List<Long> {
    val value = key as? String ?: return emptyList()
    if (!value.startsWith(SEARCH_GRID_ROW_KEY_PREFIX)) return emptyList()
    return value
        .removePrefix(SEARCH_GRID_ROW_KEY_PREFIX)
        .split(',')
        .mapNotNull(String::toLongOrNull)
}

/** Available width for one card's tag rail after page, grid, and card padding. */
internal fun searchGridTagContentWidthDp(
    availableWidthDp: Int,
    columnCount: Int,
    pageHorizontalPaddingDp: Int = SEARCH_GRID_PAGE_HORIZONTAL_PADDING_DP,
): Int {
    val columns = columnCount.coerceAtLeast(1)
    val gridWidth = availableWidthDp.coerceAtLeast(1) - pageHorizontalPaddingDp.coerceAtLeast(0)
    val cellWidth = floor(
        (gridWidth - SEARCH_GRID_CELL_GAP_DP * (columns - 1)).toDouble() / columns,
    ).toInt()
    return (cellWidth - SEARCH_GRID_CARD_HORIZONTAL_PADDING_DP).coerceAtLeast(48)
}

/**
 * Returns a shared 2:3 cover height for a grid row. Weighted children can differ by one pixel when
 * the available width is not divisible by the column count; rounding from the row's ceiling cell
 * width prevents that remainder from producing visibly uneven card bottoms.
 */
internal fun searchGridCoverHeightDp(
    availableWidthDp: Int,
    columnCount: Int,
    pageHorizontalPaddingDp: Int = SEARCH_GRID_PAGE_HORIZONTAL_PADDING_DP,
    cellGapDp: Int = SEARCH_GRID_CELL_GAP_DP,
): Int {
    val columns = columnCount.coerceAtLeast(1)
    val usableWidth = (
        availableWidthDp.coerceAtLeast(1) - pageHorizontalPaddingDp.coerceAtLeast(0) -
            cellGapDp.coerceAtLeast(0) * (columns - 1)
        ).coerceAtLeast(1)
    val sharedCellWidth = ceil(usableWidth.toDouble() / columns).toInt()
    return ceil(sharedCellWidth / (2.0 / 3.0)).toInt()
}

/**
 * Calculates a conservative tag-line count before grid children are measured. Giving every card
 * in the same row the row maximum keeps their frames aligned without throwing away source tags.
 */
internal fun searchGridTagLineCount(book: NovelCard, availableTagWidthDp: Int): Int =
    searchGridTagLineCount(
        labels = buildList {
            novelPlatformLabel(book.platform)?.let(::add)
            addAll(novelCardContentTags(book))
        },
        availableTagWidthDp = availableTagWidthDp,
        fontScale = 1f,
    )

internal fun searchGridRowTagLineCount(
    books: List<NovelCard>,
    availableTagWidthDp: Int,
    fontScale: Float = 1f,
): Int = books.maxOfOrNull { book ->
    searchGridTagLineCount(
        labels = buildList {
            novelPlatformLabel(book.platform)?.let(::add)
            addAll(novelCardContentTags(book))
        },
        availableTagWidthDp = availableTagWidthDp,
        fontScale = fontScale,
    )
}.orEmptyTagLineCount()

internal fun searchGridTagAreaMinHeightDp(lineCount: Int): Int = maxOf(
    SEARCH_GRID_TAG_MIN_AREA_HEIGHT_DP,
    lineCount.coerceAtLeast(1) * SEARCH_GRID_TAG_LINE_HEIGHT_DP - 2,
)

private fun searchGridTagLineCount(
    labels: List<String>,
    availableTagWidthDp: Int,
    fontScale: Float,
): Int {
    if (labels.isEmpty()) return 1

    val availableWidth = availableTagWidthDp.coerceAtLeast(1)
    var lines = 1
    var usedWidth = 0
    labels.forEach { label ->
        val labelWidth = searchGridTagPillWidthDp(label, fontScale).coerceAtMost(availableWidth)
        val nextWidth = if (usedWidth == 0) labelWidth else usedWidth + 2 + labelWidth
        if (usedWidth > 0 && nextWidth > availableWidth) {
            lines += 1
            usedWidth = labelWidth
        } else {
            usedWidth = nextWidth
        }
    }
    return lines
}

private fun searchGridTagPillWidthDp(label: String, fontScale: Float): Int {
    val glyphWidth = label.fold(0) { total, character ->
        total + when {
            character.isWhitespace() -> 4
            character.code <= 0x7F -> 7
            // labelSmall is 12sp and Chinese glyphs use the full em width. The previous 11dp
            // approximation could reserve one line while FlowRow actually needed two.
            else -> 12
        }
    }
    val scaledGlyphWidth = ceil(glyphWidth * fontScale.coerceAtLeast(1f)).toInt()
    // Four dp of horizontal chip padding plus one dp rounding slack on each side keeps the
    // estimated line count conservative without discarding a tag when a system font is wider.
    return (scaledGlyphWidth + 10).coerceAtLeast(20)
}

private fun Int?.orEmptyTagLineCount(): Int = this ?: 1
