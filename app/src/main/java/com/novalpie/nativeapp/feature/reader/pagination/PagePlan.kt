package com.novalpie.nativeapp.feature.reader.pagination

/** Offsets refer to the complete derived paragraph, never to a temporary LazyColumn item. */
internal data class ReaderAnchor(
    val bookId: Long,
    val chapterId: Long,
    val blockId: String,
    val textOffset: Int = 0,
)

/** The viewport is the area left AFTER real header/footer/system-inset layout. */
internal data class PageLayoutKey(
    val bookId: Long,
    val chapterId: Long,
    val documentRevision: String,
    val widthPx: Int,
    val heightPx: Int,
    val typographyRevision: String,
    val imageRevision: String,
)

internal data class MeasuredTextLine(
    val startOffset: Int,
    val endOffset: Int,
    val topPx: Float,
    val bottomPx: Float,
)

internal sealed interface MeasuredChapterBlock {
    val id: String
    val spaceAfterPx: Float

    data class Paragraph(
        override val id: String,
        val textLength: Int,
        val lines: List<MeasuredTextLine>,
        override val spaceAfterPx: Float = 0f,
        val heading: Boolean = false,
    ) : MeasuredChapterBlock

    data class Image(
        override val id: String,
        val intrinsicWidthPx: Float,
        val intrinsicHeightPx: Float,
        override val spaceAfterPx: Float = 0f,
    ) : MeasuredChapterBlock
}

internal sealed interface PageFragment {
    val blockId: String
    val yPx: Float
    val heightPx: Float

    /** Draw the original measured paragraph translated by -sourceTopPx, not a reflowed substring. */
    data class Text(
        override val blockId: String,
        val firstLine: Int,
        val endLineExclusive: Int,
        val startOffset: Int,
        val endOffset: Int,
        val sourceTopPx: Float,
        override val yPx: Float,
        override val heightPx: Float,
    ) : PageFragment

    data class Image(
        override val blockId: String,
        val widthPx: Float,
        override val heightPx: Float,
        override val yPx: Float,
    ) : PageFragment
}

internal data class ReaderPage(
    val fragments: List<PageFragment>,
    val startAnchor: ReaderAnchor,
    val usedHeightPx: Float,
)

internal data class PagePlan(val key: PageLayoutKey, val pages: List<ReaderPage>) {
    init { require(pages.isNotEmpty()) }

    private data class Location(val page: Int, val start: Int, val end: Int)
    // Built once per plan. Viewport/utterance following never scans an entire chapter each pixel.
    private val anchorIndex: Map<String, List<Location>> = buildMap {
        val entries = mutableMapOf<String, MutableList<Location>>()
        pages.forEachIndexed { page, value ->
            value.fragments.forEach { fragment ->
                entries.getOrPut(fragment.blockId) { mutableListOf() } += when (fragment) {
                    is PageFragment.Text -> Location(page, fragment.startOffset, fragment.endOffset)
                    is PageFragment.Image -> Location(page, 0, 1)
                }
            }
        }
        entries.forEach { (id, locations) -> put(id, locations.toList()) }
    }

    fun pageForAnchor(anchor: ReaderAnchor): Int {
        if (anchor.bookId != key.bookId || anchor.chapterId != key.chapterId) return 0
        val locations = anchorIndex[anchor.blockId] ?: return 0
        val offset = anchor.textOffset.coerceAtLeast(0)
        var low = 0
        var high = locations.lastIndex
        while (low < high) {
            val middle = (low + high) ushr 1
            if (locations[middle].end <= offset) low = middle + 1 else high = middle
        }
        return locations[low].page
    }
}

internal class ReaderViewportTooSmall(val minimumLineHeightPx: Float) :
    IllegalArgumentException("阅读区域不足以显示一行文字，请缩小字号或减少上下边距")

/** Complete line-based partition: no scroll padding, viewport masks or page-history heuristics. */
internal fun planChapterPages(key: PageLayoutKey, blocks: List<MeasuredChapterBlock>): PagePlan {
    require(key.widthPx > 0 && key.heightPx > 0)
    require(blocks.map { it.id }.distinct().size == blocks.size) { "Chapter block identities must be unique" }
    val height = key.heightPx.toFloat()
    val pages = mutableListOf<ReaderPage>()
    val fragments = mutableListOf<PageFragment>()
    val headingIds = blocks.filterIsInstance<MeasuredChapterBlock.Paragraph>().filter { it.heading }.map { it.id }.toSet()
    var used = 0f
    var pendingGap = 0f

    fun finishPage() {
        if (fragments.isEmpty()) return
        val first = fragments.first()
        pages += ReaderPage(
            fragments = fragments.toList(),
            startAnchor = ReaderAnchor(key.bookId, key.chapterId, first.blockId, (first as? PageFragment.Text)?.startOffset ?: 0),
            usedHeightPx = used,
        )
        fragments.clear()
        used = 0f
        pendingGap = 0f
    }

    blocks.forEach { block ->
        require(block.spaceAfterPx.isFinite() && block.spaceAfterPx >= 0f)
        when (block) {
            is MeasuredChapterBlock.Paragraph -> {
                if (block.lines.isEmpty()) {
                    require(block.textLength == 0) { "Nonempty text must have measured lines" }
                    return@forEach
                }
                require(block.lines.first().startOffset == 0 && block.lines.last().endOffset == block.textLength)
                block.lines.forEachIndexed { index, line ->
                    require(line.topPx.isFinite() && line.bottomPx.isFinite() && line.bottomPx > line.topPx)
                    require(line.startOffset >= 0 && line.endOffset >= line.startOffset)
                    if (index > 0) {
                        require(line.startOffset == block.lines[index - 1].endOffset)
                        require(line.topPx >= block.lines[index - 1].bottomPx)
                    }
                    if (line.bottomPx - line.topPx > height) throw ReaderViewportTooSmall(line.bottomPx - line.topPx)
                }
                val fullHeight = block.lines.last().bottomPx - block.lines.first().topPx
                // Prefer whole paragraphs when possible, but never prevent a long paragraph from splitting.
                val onlyHeadings = fragments.isNotEmpty() && fragments.all { it.blockId in headingIds }
                if (fragments.isNotEmpty() && !onlyHeadings && fullHeight <= height && used + pendingGap + fullHeight > height) finishPage()
                var firstLine = 0
                while (firstLine < block.lines.size) {
                    val top = block.lines[firstLine].topPx
                    val gap = if (fragments.isEmpty()) 0f else pendingGap
                    if (used + gap + block.lines[firstLine].bottomPx - top > height) {
                        finishPage()
                        continue
                    }
                    used += gap
                    pendingGap = 0f
                    var endLine = firstLine + 1
                    while (endLine < block.lines.size && used + block.lines[endLine].bottomPx - top <= height) endLine++
                    val fragmentHeight = block.lines[endLine - 1].bottomPx - top
                    fragments += PageFragment.Text(
                        blockId = block.id, firstLine = firstLine, endLineExclusive = endLine,
                        startOffset = block.lines[firstLine].startOffset, endOffset = block.lines[endLine - 1].endOffset,
                        sourceTopPx = top, yPx = used, heightPx = fragmentHeight,
                    )
                    used += fragmentHeight
                    firstLine = endLine
                    if (firstLine < block.lines.size) finishPage()
                }
            }
            is MeasuredChapterBlock.Image -> {
                require(block.intrinsicWidthPx.isFinite() && block.intrinsicWidthPx > 0f)
                require(block.intrinsicHeightPx.isFinite() && block.intrinsicHeightPx > 0f)
                val scale = minOf(key.widthPx / block.intrinsicWidthPx, height / block.intrinsicHeightPx)
                val imageHeight = (block.intrinsicHeightPx * scale).coerceAtMost(height)
                if (fragments.isNotEmpty() && used + pendingGap + imageHeight > height) finishPage()
                used += if (fragments.isEmpty()) 0f else pendingGap
                fragments += PageFragment.Image(block.id, block.intrinsicWidthPx * scale, imageHeight, used)
                used += imageHeight
            }
        }
        pendingGap = block.spaceAfterPx
    }
    finishPage()
    if (pages.isEmpty()) pages += ReaderPage(emptyList(), ReaderAnchor(key.bookId, key.chapterId, ""), 0f)
    return PagePlan(key, pages.toList())
}
