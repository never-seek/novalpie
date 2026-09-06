package com.novalpie.nativeapp.feature.reader.pagination

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import com.novalpie.nativeapp.model.ReaderContent
import com.novalpie.nativeapp.ui.ReaderContentBlock
import com.novalpie.nativeapp.ui.readerBlocksForContent
import com.novalpie.nativeapp.ui.readerBlocksForDisplay
import com.novalpie.nativeapp.ui.toAnnotatedString
import com.novalpie.nativeapp.model.ChineseVariant
import com.novalpie.nativeapp.ui.convertChineseVariantAnnotatedText
import com.novalpie.nativeapp.feature.reader.text.readerAnnotatedTextWithWordSpacing

internal data class ChapterDocument(
    val bookId: Long,
    val chapterId: Long,
    val originalContent: String,
    val revision: String,
    val blocks: List<ChapterDocumentBlock>,
)

internal sealed interface ChapterDocumentBlock {
    val id: String

    data class Paragraph(override val id: String, val text: AnnotatedString, val heading: Boolean = false) : ChapterDocumentBlock
    data class Image(override val id: String, val url: String, val originalUrl: String? = null, val alt: String? = null) : ChapterDocumentBlock
}

/** Parsing belongs to document preparation, outside both composition and viewport observers. */
internal fun chapterDocumentFromContent(
    bookId: Long,
    chapterId: Long,
    original: ReaderContent,
    derived: ReaderContent,
    revision: String,
    showImages: Boolean = true,
    removeDuplicateLines: Boolean = false,
    chineseVariant: ChineseVariant = ChineseVariant.Original,
    wordSpacing: Float = 0f,
): ChapterDocument {
    fun display(text:AnnotatedString)=readerAnnotatedTextWithWordSpacing(convertChineseVariantAnnotatedText(text,chineseVariant),wordSpacing)
    val blocks = buildList {
        derived.title?.takeIf(String::isNotBlank)?.let { add(ChapterDocumentBlock.Paragraph("$chapterId:title", display(AnnotatedString(it)), true)) }
        readerBlocksForDisplay(readerBlocksForContent(derived),removeDuplicateLines).forEachIndexed { index, block ->
            when (block) {
                is ReaderContentBlock.Text -> add(ChapterDocumentBlock.Paragraph("$chapterId:p:$index", display(block.formatted?.toAnnotatedString() ?: AnnotatedString(block.value))))
                is ReaderContentBlock.Image -> if(showImages) add(ChapterDocumentBlock.Image("$chapterId:i:$index", block.url, block.originalUrl, block.alt))
            }
        }
    }
    return ChapterDocument(bookId,chapterId,original.content,revision,blocks)
}

internal data class ImageDimensions(val widthPx: Int, val heightPx: Int) {
    init { require(widthPx > 0 && heightPx > 0) }
}

internal data class MeasuredChapterDocument(
    val document: ChapterDocument,
    val plan: PagePlan,
    val textLayouts: Map<String, TextLayoutResult>,
)

/**
 * Measures complete paragraphs once using the exact text style subsequently drawn on the page.
 * The original TextLayoutResult is shared by every fragment of a long paragraph; remeasuring a
 * substring would change indentation, justification and even line endings on backward paging.
 */
internal fun measureChapterDocument(
    document: ChapterDocument,
    key: PageLayoutKey,
    textMeasurer: TextMeasurer,
    paragraphStyle: TextStyle,
    headingStyle: TextStyle = paragraphStyle,
    paragraphSpacingPx: Float = 0f,
    headingSpacingPx: Float = paragraphSpacingPx,
    imageDimensions: Map<String, ImageDimensions> = emptyMap(),
): MeasuredChapterDocument {
    require(key.bookId == document.bookId && key.chapterId == document.chapterId && key.documentRevision == document.revision)
    val layouts = linkedMapOf<String, TextLayoutResult>()
    val measured = document.blocks.map { block ->
        when (block) {
            is ChapterDocumentBlock.Paragraph -> {
                val layout = textMeasurer.measure(
                    text = block.text,
                    style = if (block.heading) headingStyle else paragraphStyle,
                    softWrap = true,
                    maxLines = Int.MAX_VALUE,
                    constraints = Constraints(maxWidth = key.widthPx),
                )
                layouts[block.id] = layout
                MeasuredChapterBlock.Paragraph(
                    id = block.id,
                    textLength = block.text.length,
                    lines = (0 until layout.lineCount).map { line ->
                        MeasuredTextLine(layout.getLineStart(line), layout.getLineEnd(line), layout.getLineTop(line), layout.getLineBottom(line))
                    },
                    spaceAfterPx = if (block.heading) headingSpacingPx else paragraphSpacingPx,
                )
            }
            is ChapterDocumentBlock.Image -> {
                // Unknown size occupies a bounded full page until image metadata arrives. An
                // anchored replan then shrinks it, rather than pushing text out from under a tap.
                val size = imageDimensions[block.id] ?: ImageDimensions(key.widthPx, key.heightPx)
                MeasuredChapterBlock.Image(block.id, size.widthPx.toFloat(), size.heightPx.toFloat(), paragraphSpacingPx)
            }
        }
    }
    return MeasuredChapterDocument(document, planChapterPages(key, measured), layouts.toMap())
}
