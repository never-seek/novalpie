package com.novalpie.nativeapp.ui

import android.text.Html
import android.os.Build
import com.novalpie.nativeapp.model.ChapterIllustration
import com.novalpie.nativeapp.model.ReaderContent

internal sealed interface ReaderContentBlock {
    data class Text(val value: String) : ReaderContentBlock
    data class Image(
        val url: String,
        val alt: String? = null,
        /** Optional full-resolution/inner source supplied alongside the rendered image. */
        val originalUrl: String? = null,
    ) : ReaderContentBlock
}

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
    readerParagraphsFromContent(raw).forEach { paragraph ->
        target += ReaderContentBlock.Text(paragraph)
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
    val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY)
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(value)
    }
    return decoded.toString().replace('\u00A0', ' ')
}
