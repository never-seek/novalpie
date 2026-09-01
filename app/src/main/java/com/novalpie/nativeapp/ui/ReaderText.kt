package com.novalpie.nativeapp.ui

import android.text.Html
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.text.style.StrikethroughSpan
import android.os.Build
import android.graphics.Typeface
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.novalpie.nativeapp.model.ChapterIllustration
import com.novalpie.nativeapp.model.ReaderContent

internal sealed interface ReaderContentBlock {
    data class Text(
        val value: String,
        /** Kept only for reader rendering; other consumers retain the plain source-compatible value. */
        val formatted: ReaderFormattedParagraph? = null,
    ) : ReaderContentBlock
    data class Image(
        val url: String,
        val alt: String? = null,
        /** Optional full-resolution/inner source supplied alongside the rendered image. */
        val originalUrl: String? = null,
    ) : ReaderContentBlock
}

/** Reader prose plus the source emphasis spans retained from rendered HTML. */
internal data class ReaderFormattedParagraph(
    val text: String,
    val spanStyles: List<AnnotatedString.Range<SpanStyle>> = emptyList(),
)

internal fun readerImagePlaceholdersFromIllustrations(
    illustrations: List<ChapterIllustration>
): Map<Int, ReaderContentBlock.Image> =
    illustrations
        .filter { it.index > 0 && it.src.isNotBlank() }
        .associate { illustration ->
            illustration.index to ReaderContentBlock.Image(
                url = illustration.src,
                originalUrl = illustration.originalSrc,
                alt = "正文插图 ${illustration.index}"
            )
        }

/** Keeps source text, Markdown/HTML images, and separately supplied illustrations in one order. */
internal fun readerBlocksForContent(content: ReaderContent): List<ReaderContentBlock> =
    readerBlocksFromContent(
        raw = content.content,
        imagePlaceholders = readerImagePlaceholdersFromIllustrations(content.illustrations)
    )

internal fun readerBlocksFromContent(
    raw: String,
    baseUrl: String = "https://novalpie.cc",
    imagePlaceholders: Map<Int, ReaderContentBlock.Image> = emptyMap()
): List<ReaderContentBlock> {
    if (raw.isBlank()) return emptyList()
    val blocks = mutableListOf<ReaderContentBlock>()
    // A URL is not an image identity in authored chapter content: an author may intentionally use
    // the same illustration twice. Track only source illustration placeholder indices so a
    // `[[img:n]]` token and the trailing unplaced-illustration fallback cannot duplicate one
    // server-provided asset, while explicit HTML/Markdown images retain every occurrence.
    val placedIllustrationIndices = mutableSetOf<Int>()
    var cursor = 0
    readerImagePattern.findAll(raw).forEach { match ->
        appendReaderTextBlocks(blocks, raw.substring(cursor, match.range.first))
        val token = match.value
        val markdown = markdownImagePattern.matchEntire(token)
        val placeholder = imagePlaceholderPattern.matchEntire(token)
        if (placeholder != null) {
            val illustrationIndex = placeholder.groupValues.getOrNull(1)?.toIntOrNull()
            val placeholderImage = illustrationIndex?.let(imagePlaceholders::get)
            val normalizedImage = placeholderImage?.let { image ->
                normalizeReaderImageUrl(image.url, baseUrl)?.let { normalizedUrl ->
                    image.copy(
                        url = normalizedUrl,
                        alt = decodeHtmlValue(image.alt).takeIf(String::isNotBlank)
                    )
                }
            }
            if (normalizedImage != null) {
                illustrationIndex?.let(placedIllustrationIndices::add)
                blocks += normalizedImage
            } else {
                appendReaderTextBlocks(blocks, token)
            }
        } else {
            val rawUrl = if (markdown != null) {
                markdown.groupValues[2]
            } else {
                htmlImageAttribute(token, "data-src")
                    ?: htmlImageAttribute(token, "src")
            }
            val rawOriginalUrl = if (markdown != null) {
                null
            } else {
                htmlImageAttribute(token, "data-original")
                    ?: htmlImageAttribute(token, "data-full")
                    ?: htmlImageAttribute(token, "data-true-url")
            }
            val alt = markdown?.groupValues?.getOrNull(1)?.takeIf(String::isNotBlank)
                ?: htmlImageAttribute(token, "alt")?.takeIf(String::isNotBlank)
            normalizeReaderImageUrl(rawUrl, baseUrl)?.let { url ->
                blocks += ReaderContentBlock.Image(
                    url = url,
                    alt = decodeHtmlValue(alt).takeIf(String::isNotBlank),
                    originalUrl = normalizeReaderImageUrl(rawOriginalUrl, baseUrl),
                )
            }
        }
        cursor = match.range.last + 1
    }
    appendReaderTextBlocks(blocks, raw.substring(cursor))

    // The source sometimes returns illustrations separately from translated/plain text without
    // [[img:n]] markers. Render any unplaced source image at the end instead of dropping it.
    imagePlaceholders
        .toSortedMap()
        .forEach { (index, image) ->
            if (index in placedIllustrationIndices) return@forEach
            normalizeReaderImageUrl(image.url, baseUrl)?.let { url ->
                blocks += image.copy(url = url, alt = decodeHtmlValue(image.alt).takeIf(String::isNotBlank))
            }
        }
    return blocks
}

internal fun readerParagraphsFromContent(raw: String): List<String> {
    val source = raw.trim()
    if (source.isBlank()) return emptyList()

    // The source emits both HTML paragraphs and newline-delimited plain text. Plain text lines
    // must remain individual paragraphs so each one receives its own indent and layout work.
    val keepsExplicitLineBreaks = readerStructuralMarkupRegex.containsMatchIn(source)
    // Avoid routing ordinary API plain text through android.text.Html. Besides being cheaper on
    // Android, this keeps the presentation layer usable in JVM tests and preserves authored line
    // breaks exactly as the source reader does.
    if (!keepsExplicitLineBreaks && !source.contains('<') && !source.contains('>')) {
        return source
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
            .mapNotNull(::normalizeReaderParagraph)
    }
    val prepared = source
        .replace(consecutiveBreakTagRegex, PARAGRAPH_BREAK_MARKER)
        .replace(breakTagRegex, LINE_BREAK_MARKER)
        .replace(blockEndTagRegex, PARAGRAPH_BREAK_MARKER)

    val decoded = htmlToPlainText(
        if (keepsExplicitLineBreaks) prepared else prepared.replace("\n", "<br>")
    )
        .replace(' ', ' ')
        .replace(LINE_BREAK_MARKER, "\n")
        .replace(PARAGRAPH_BREAK_MARKER, "\n\n")
        .replace("\r\n", "\n")
        .replace('\r', '\n')

    return decoded
        .split(Regex("\n{2,}"))
        .flatMap { section ->
            if (keepsExplicitLineBreaks) {
                listOfNotNull(normalizeReaderParagraph(section))
            } else {
                section.lines().mapNotNull(::normalizeReaderParagraph)
            }
        }
}

/**
 * Converts the chapter's safe HTML subset into display text without losing <strong>/<b> ranges.
 * The normal paragraph splitter remains the source of truth for article rhythm; this rich parser
 * is deliberately scoped to an individual reader paragraph so markup never leaks across blocks.
 */
internal fun readerFormattedParagraphsFromContent(raw: String): List<ReaderFormattedParagraph> {
    val source = raw.trim()
    if (source.isBlank()) return emptyList()
    if (!source.contains('<') || !source.contains('>')) {
        return readerParagraphsFromContent(source)
            .map { paragraph -> applyMarkdownRanges(ReaderFormattedParagraph(paragraph)) }
    }
    val prepared = source
        .replace(consecutiveBreakTagRegex, PARAGRAPH_BREAK_MARKER)
        .replace(breakTagRegex, LINE_BREAK_MARKER)
        .replace(blockEndTagRegex, PARAGRAPH_BREAK_MARKER)
    val spanned = htmlToSpanned(
        if (readerStructuralMarkupRegex.containsMatchIn(source)) prepared else prepared.replace("\n", "<br>")
    )
    val normalizedText = spanned.toString()
        .replace('\u00A0', ' ')
        .replace(LINE_BREAK_MARKER, "\n")
        .replace(PARAGRAPH_BREAK_MARKER, "\n\n")
        .replace("\r\n", "\n")
        .replace('\r', '\n')
    return splitReaderFormattedParagraphs(normalizedText, spanned)
        .map(::applyMarkdownRanges)
}

/** Applies the website's optional duplicate-line cleanup without altering authored repeats. */
internal fun readerBlocksForDisplay(
    blocks: List<ReaderContentBlock>,
    removeDuplicateLines: Boolean,
): List<ReaderContentBlock> {
    if (!removeDuplicateLines) return blocks
    var previousText: String? = null
    return blocks.filter { block ->
        if (block !is ReaderContentBlock.Text) {
            previousText = null
            return@filter true
        }
        val normalized = block.value.trim()
        val keep = normalized.isNotBlank() && normalized != previousText
        if (keep) previousText = normalized
        keep
    }
}

private val blockEndTagRegex = Regex(
    "</(p|div|section|article|li|h[1-6])>",
    setOf(RegexOption.IGNORE_CASE)
)
private val readerStructuralMarkupRegex = Regex(
    "<(?:p|div|section|article|li|h[1-6]|br)\\b",
    setOf(RegexOption.IGNORE_CASE)
)
private val breakTagRegex = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
private val consecutiveBreakTagRegex = Regex(
    "(?:<br\\s*/?>\\s*){2,}",
    setOf(RegexOption.IGNORE_CASE)
)

private val htmlImagePattern = """<img\b[^>]*>"""
private val markdownImagePattern = Regex("""!\[([^]]*)]\(([^)\s]+)(?:\s+["'][^"']*["'])?\)""")
private val imagePlaceholderPattern = Regex("""\[\[\s*img\s*:\s*(\d+)\s*]]""", RegexOption.IGNORE_CASE)
private val readerImagePattern = Regex(
    "(?:$htmlImagePattern)|(?:${markdownImagePattern.pattern})|(?:${imagePlaceholderPattern.pattern})",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)

private const val LINE_BREAK_MARKER = ""
private const val PARAGRAPH_BREAK_MARKER = ""

private fun normalizeReaderParagraph(value: String): String? {
    val lines = value
        .lines()
        .map { line -> line.trim().replace(Regex("[ \\t\\x0B\\f]+"), " ") }
        .filter { it.isNotBlank() }

    return lines.joinToString("\n").takeIf { it.isNotBlank() }
}

private fun appendReaderTextBlocks(target: MutableList<ReaderContentBlock>, raw: String) {
    readerFormattedParagraphsFromContent(raw).forEach { paragraph ->
        target += ReaderContentBlock.Text(value = paragraph.text, formatted = paragraph)
    }
}

private fun htmlImageAttribute(tag: String, name: String): String? {
    val quoted = Regex("""\b${Regex.escape(name)}\s*=\s*(["'])(.*?)\1""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .find(tag)?.groupValues?.getOrNull(2)
    if (!quoted.isNullOrBlank()) return quoted
    return Regex("""\b${Regex.escape(name)}\s*=\s*([^\s>]+)""", RegexOption.IGNORE_CASE)
        .find(tag)?.groupValues?.getOrNull(1)
}

private fun normalizeReaderImageUrl(raw: String?, baseUrl: String): String? {
    val value = decodeHtmlValue(raw).trim().takeIf(String::isNotBlank) ?: return null
    if (value.startsWith("javascript:", ignoreCase = true)) return null
    return when {
        value.startsWith("http://", true) || value.startsWith("https://", true) || value.startsWith("data:", true) -> value
        value.startsWith("//") -> "https:$value"
        value.startsWith("/") -> baseUrl.trimEnd('/') + value
        else -> baseUrl.trimEnd('/') + "/" + value.trimStart('/')
    }
}

private fun decodeHtmlValue(value: String?): String = value?.let(::htmlToPlainText).orEmpty()

/** Html.fromHtml(String, flags) was added in API 24; Android 6 uses the legacy overload. */
private fun htmlToPlainText(value: String): String {
    val decoded = htmlToSpanned(value)
    return decoded.toString().replace('\u00A0', ' ')
}

/** Html.fromHtml(String, flags) was added in API 24; Android 6 uses the legacy overload. */
private fun htmlToSpanned(value: String): Spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY)
} else {
    @Suppress("DEPRECATION")
    Html.fromHtml(value)
}

private fun splitReaderFormattedParagraphs(
    text: String,
    original: Spanned,
): List<ReaderFormattedParagraph> {
    val paragraphs = mutableListOf<ReaderFormattedParagraph>()
    var cursor = 0
    Regex("\\n{2,}").findAll(text).forEach { separator ->
        addReaderFormattedParagraph(paragraphs, text, original, cursor, separator.range.first)
        cursor = separator.range.last + 1
    }
    addReaderFormattedParagraph(paragraphs, text, original, cursor, text.length)
    return paragraphs
}

private fun addReaderFormattedParagraph(
    target: MutableList<ReaderFormattedParagraph>,
    text: String,
    original: Spanned,
    start: Int,
    end: Int,
) {
    val raw = text.substring(start, end)
    val leading = raw.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: return
    val trailing = raw.indexOfLast { !it.isWhitespace() }.takeIf { it >= leading } ?: return
    val paragraphStart = start + leading
    val paragraphEnd = start + trailing + 1
    val paragraphText = text.substring(paragraphStart, paragraphEnd)
    fun spanRange(span: Any, style: SpanStyle): AnnotatedString.Range<SpanStyle>? {
        val spanStart = original.getSpanStart(span).coerceIn(paragraphStart, paragraphEnd)
        val spanEnd = original.getSpanEnd(span).coerceIn(paragraphStart, paragraphEnd)
        return if (spanStart >= spanEnd) null else {
            AnnotatedString.Range(
                item = style,
                start = spanStart - paragraphStart,
                end = spanEnd - paragraphStart,
            )
        }
    }
    val styles = buildList {
        original.getSpans(paragraphStart, paragraphEnd, StyleSpan::class.java).forEach { span ->
            val style = SpanStyle(
                fontWeight = FontWeight.Bold.takeIf { span.style and Typeface.BOLD != 0 },
                fontStyle = FontStyle.Italic.takeIf { span.style and Typeface.ITALIC != 0 },
            )
            spanRange(span, style)?.let(::add)
        }
        original.getSpans(paragraphStart, paragraphEnd, UnderlineSpan::class.java).forEach { span ->
            spanRange(span, SpanStyle(textDecoration = TextDecoration.Underline))?.let(::add)
        }
        original.getSpans(paragraphStart, paragraphEnd, StrikethroughSpan::class.java).forEach { span ->
            spanRange(span, SpanStyle(textDecoration = TextDecoration.LineThrough))?.let(::add)
        }
    }
    target += ReaderFormattedParagraph(text = paragraphText, spanStyles = styles)
}

/**
 * The source mixes rendered HTML with plain Markdown in chapter bodies. Html.fromHtml() preserves
 * `<strong>` spans but deliberately leaves `**strong**` and `__strong__` untouched, so normalize
 * the latter after HTML span extraction and remap the existing offsets as delimiters disappear.
 */
private fun applyMarkdownRanges(paragraph: ReaderFormattedParagraph): ReaderFormattedParagraph {
    val source = paragraph.text
    if (!source.contains("**") && !source.contains("__") && !source.contains('*') && !source.contains('_') && !source.contains("~~")) return paragraph

    val output = StringBuilder(source.length)
    val outputOffsets = IntArray(source.length + 1)
    val markdownRanges = mutableListOf<AnnotatedString.Range<SpanStyle>>()
    var cursor = 0
    while (cursor < source.length) {
        outputOffsets[cursor] = output.length
        val delimiter = markdownStyleDelimiterAt(source, cursor)
        val closingIndex = delimiter?.let { markdownStyleClosingIndex(source, cursor + it.token.length, it.token) }
        if (delimiter == null || closingIndex == null) {
            output.append(source[cursor])
            cursor += 1
            continue
        }

        val delimiterEnd = cursor + delimiter.token.length
        (cursor until delimiterEnd).forEach { index -> outputOffsets[index] = output.length }
        val boldStart = output.length
        var inner = delimiterEnd
        while (inner < closingIndex) {
            outputOffsets[inner] = output.length
            output.append(source[inner])
            inner += 1
        }
        val boldEnd = output.length
        (closingIndex until (closingIndex + delimiter.token.length)).forEach { index ->
            outputOffsets[index] = output.length
        }
        cursor = closingIndex + delimiter.token.length
        outputOffsets[cursor] = output.length
        markdownRanges += AnnotatedString.Range(
            item = delimiter.style,
            start = boldStart,
            end = boldEnd,
        )
    }
    outputOffsets[source.length] = output.length

    val remappedHtmlRanges = paragraph.spanStyles.mapNotNull { range ->
        val start = outputOffsets[range.start.coerceIn(0, source.length)]
        val end = outputOffsets[range.end.coerceIn(0, source.length)]
        AnnotatedString.Range(range.item, start, end).takeIf { start < end }
    }
    return ReaderFormattedParagraph(
        text = output.toString(),
        spanStyles = remappedHtmlRanges + markdownRanges,
    )
}

private data class ReaderMarkdownStyleDelimiter(
    val token: String,
    val style: SpanStyle,
)

private fun markdownStyleDelimiterAt(value: String, index: Int): ReaderMarkdownStyleDelimiter? {
    if (isMarkdownEscaped(value, index)) return null
    return when {
        value.startsWith("**", index) -> ReaderMarkdownStyleDelimiter("**", SpanStyle(fontWeight = FontWeight.Bold))
        value.startsWith("__", index) -> ReaderMarkdownStyleDelimiter("__", SpanStyle(fontWeight = FontWeight.Bold))
        value.startsWith("~~", index) -> ReaderMarkdownStyleDelimiter("~~", SpanStyle(textDecoration = TextDecoration.LineThrough))
        value.startsWith("*", index) && !value.startsWith("**", index) -> ReaderMarkdownStyleDelimiter("*", SpanStyle(fontStyle = FontStyle.Italic))
        value.startsWith("_", index) && !value.startsWith("__", index) -> ReaderMarkdownStyleDelimiter("_", SpanStyle(fontStyle = FontStyle.Italic))
        else -> null
    }
}

private fun markdownStyleClosingIndex(value: String, start: Int, delimiter: String): Int? {
    var candidate = value.indexOf(delimiter, start)
    while (candidate >= 0) {
        val content = value.substring(start, candidate)
        if (content.any { !it.isWhitespace() } && !isMarkdownEscaped(value, candidate)) return candidate
        candidate = value.indexOf(delimiter, candidate + delimiter.length)
    }
    return null
}

private fun isMarkdownEscaped(value: String, index: Int): Boolean {
    var slashCount = 0
    var cursor = index - 1
    while (cursor >= 0 && value[cursor] == '\\') {
        slashCount += 1
        cursor -= 1
    }
    return slashCount % 2 != 0
}

internal fun ReaderFormattedParagraph.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    append(text)
    spanStyles.forEach { range ->
        val start = range.start.coerceIn(0, text.length)
        val end = range.end.coerceIn(start, text.length)
        if (start < end) addStyle(range.item, start, end)
    }
}
