package com.novalpie.nativeapp.ui

import android.text.Html
import com.novalpie.nativeapp.model.ChapterIllustration

internal sealed interface ReaderContentBlock {
    data class Text(val value: String) : ReaderContentBlock
    data class Image(val url: String, val alt: String? = null) : ReaderContentBlock
}

internal fun readerImagePlaceholdersFromIllustrations(
    illustrations: List<ChapterIllustration>
): Map<Int, ReaderContentBlock.Image> =
    illustrations
        .filter { it.index > 0 && it.src.isNotBlank() }
        .associate { illustration ->
            illustration.index to ReaderContentBlock.Image(
                url = illustration.src,
                alt = "正文插图 ${illustration.index}"
            )
        }

internal fun readerBlocksFromContent(
    raw: String,
    baseUrl: String = "https://novalpie.cc",
    imagePlaceholders: Map<Int, ReaderContentBlock.Image> = emptyMap()
): List<ReaderContentBlock> {
    if (raw.isBlank()) return emptyList()
    val blocks = mutableListOf<ReaderContentBlock>()
    var cursor = 0
    readerImagePattern.findAll(raw).forEach { match ->
        appendReaderTextBlocks(blocks, raw.substring(cursor, match.range.first))
        val token = match.value
        val markdown = markdownImagePattern.matchEntire(token)
        val placeholder = imagePlaceholderPattern.matchEntire(token)
        if (placeholder != null) {
            val placeholderImage = placeholder.groupValues.getOrNull(1)
                ?.toIntOrNull()
                ?.let(imagePlaceholders::get)
            val normalizedImage = placeholderImage?.let { image ->
                normalizeReaderImageUrl(image.url, baseUrl)?.let { normalizedUrl ->
                    image.copy(
                        url = normalizedUrl,
                        alt = decodeHtmlValue(image.alt).takeIf(String::isNotBlank)
                    )
                }
            }
            if (normalizedImage != null) {
                blocks += normalizedImage
            } else {
                appendReaderTextBlocks(blocks, token)
            }
        } else {
            val rawUrl = if (markdown != null) {
                markdown.groupValues[2]
            } else {
                htmlImageAttribute(token, "data-src")
                    ?: htmlImageAttribute(token, "data-original")
                    ?: htmlImageAttribute(token, "src")
            }
            val alt = markdown?.groupValues?.getOrNull(1)?.takeIf(String::isNotBlank)
                ?: htmlImageAttribute(token, "alt")?.takeIf(String::isNotBlank)
            normalizeReaderImageUrl(rawUrl, baseUrl)?.let { url ->
                blocks += ReaderContentBlock.Image(url, decodeHtmlValue(alt).takeIf(String::isNotBlank))
            }
        }
        cursor = match.range.last + 1
    }
    appendReaderTextBlocks(blocks, raw.substring(cursor))
    return blocks
}

internal fun readerParagraphsFromContent(raw: String): List<String> {
    val prepared = raw.trim()
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), LINE_BREAK_MARKER)
        .replace(blockEndTagRegex, PARAGRAPH_BREAK_MARKER)

    if (prepared.isBlank()) return emptyList()

    val decoded = Html.fromHtml(prepared, Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace(' ', ' ')
        .replace(LINE_BREAK_MARKER, "\n")
        .replace(PARAGRAPH_BREAK_MARKER, "\n\n")
        .replace("\r\n", "\n")
        .replace('\r', '\n')

    return decoded
        .split(Regex("\n{2,}"))
        .mapNotNull(::normalizeReaderParagraph)
}

private val blockEndTagRegex = Regex(
    "</(p|div|section|article|li|h[1-6])>",
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

private fun decodeHtmlValue(value: String?): String = value?.let {
    Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString().replace(' ', ' ')
}.orEmpty()
