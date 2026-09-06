package com.novalpie.nativeapp.feature.reader.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.novalpie.nativeapp.model.ReaderTextDerivation
import com.novalpie.nativeapp.ui.ReaderContentBlock
import com.novalpie.nativeapp.ui.ReaderFormattedParagraph

/** Original images and emphasis are structural; rule output can only become visible text. */
internal fun deriveReaderTextNodes(blocks: List<ReaderContentBlock>, derivation: ReaderTextDerivation): List<ReaderContentBlock> =
    blocks.flatMap { block ->
        if (block !is ReaderContentBlock.Text) return@flatMap listOf(block)
        val paragraph = block.formatted ?: ReaderFormattedParagraph(block.value)
        val boundaries = (listOf(0, paragraph.text.length) + paragraph.spanStyles.flatMap { listOf(it.start, it.end) }).distinct().sorted()
        val result = StringBuilder()
        val styles = mutableListOf<AnnotatedString.Range<SpanStyle>>()
        boundaries.zipWithNext().forEach { (start, end) ->
            val text = DerivedTextPipeline.transformVisibleText(paragraph.text.substring(start, end), derivation.rules, derivation.chapterOrder)
            val outputStart = result.length
            result.append(text)
            paragraph.spanStyles.filter { it.start <= start && it.end >= end }.forEach { span ->
                if (result.length > outputStart) styles += AnnotatedString.Range(span.item, outputStart, result.length, span.tag)
            }
        }
        val value = result.toString()
        val paragraphs = mutableListOf<ReaderContentBlock>()
        var start = 0
        fun append(end: Int) {
            val raw = value.substring(start, end)
            val first = raw.indexOfFirst { !it.isWhitespace() }
            if (first >= 0) {
                val from = start + first
                val until = start + raw.indexOfLast { !it.isWhitespace() } + 1
                val spans = styles.mapNotNull { span ->
                    val left = maxOf(from, span.start); val right = minOf(until, span.end)
                    if (left < right) AnnotatedString.Range(span.item, left - from, right - from, span.tag) else null
                }
                val text = value.substring(from, until)
                paragraphs += ReaderContentBlock.Text(text, ReaderFormattedParagraph(text, spans))
            }
        }
        Regex("\n{2,}").findAll(value).forEach { separator -> append(separator.range.first); start = separator.range.last + 1 }
        append(value.length)
        paragraphs
    }
