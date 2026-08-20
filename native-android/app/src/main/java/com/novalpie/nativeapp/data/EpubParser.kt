package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.ParsedEpub
import com.novalpie.nativeapp.model.UploadChapter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element

object EpubParser {
    private const val CONTAINER_PATH = "META-INF/container.xml"
    private const val MAX_XML_BYTES = 4 * 1024 * 1024
    private const val MAX_CHAPTER_BYTES = 24 * 1024 * 1024

    fun parse(source: UploadFileSource): ParsedEpub {
        val container = readEntries(source, setOf(CONTAINER_PATH), MAX_XML_BYTES)[CONTAINER_PATH]
            ?: throw IOException("EPUB 缺少 META-INF/container.xml")
        val containerDocument = parseXml(container)
        val packagePath = firstElement(containerDocument, "rootfile")
            ?.getAttribute("full-path")
            ?.takeIf { it.isNotBlank() }
            ?: throw IOException("EPUB 未声明 OPF 路径")

        val packageBytes = readEntries(source, setOf(normalizeZipPath(packagePath)), MAX_XML_BYTES)
            .get(normalizeZipPath(packagePath))
            ?: throw IOException("EPUB 缺少 OPF 文件")
        val packageDocument = parseXml(packageBytes)
        val packageDir = packagePath.substringBeforeLast('/', "")

        val manifest = linkedMapOf<String, String>()
        val manifestNodes = packageDocument.getElementsByTagNameNS("*", "item")
        for (index in 0 until manifestNodes.length) {
            val item = manifestNodes.item(index) as? Element ?: continue
            val id = item.getAttribute("id")
            val href = item.getAttribute("href")
            if (id.isNotBlank() && href.isNotBlank()) {
                manifest[id] = resolveZipPath(packageDir, href.substringBefore('#'))
            }
        }

        val spinePaths = mutableListOf<String>()
        val spineNodes = packageDocument.getElementsByTagNameNS("*", "itemref")
        for (index in 0 until spineNodes.length) {
            val idRef = (spineNodes.item(index) as? Element)?.getAttribute("idref").orEmpty()
            manifest[idRef]?.let(spinePaths::add)
        }
        if (spinePaths.isEmpty()) throw IOException("EPUB 目录为空")

        val chapterEntries = readEntries(source, spinePaths.toSet(), MAX_CHAPTER_BYTES)
        val chapters = spinePaths.mapIndexedNotNull { index, path ->
            val bytes = chapterEntries[path] ?: return@mapIndexedNotNull null
            val html = bytes.toString(Charsets.UTF_8)
            val content = htmlToPlainText(html)
            if (content.isBlank()) return@mapIndexedNotNull null
            UploadChapter(
                title = extractHtmlTitle(html).ifBlank { "第 ${index + 1} 章" },
                content = content,
                chapterNumber = index + 1,
                rawPath = path,
                spineIndex = index
            )
        }
        if (chapters.isEmpty()) throw IOException("EPUB 未解析到有效章节")

        return ParsedEpub(
            title = elementText(packageDocument, "title"),
            author = elementText(packageDocument, "creator"),
            description = elementText(packageDocument, "description"),
            language = elementText(packageDocument, "language").ifBlank { "zh" },
            chapters = chapters
        )
    }

    private fun readEntries(
        source: UploadFileSource,
        requested: Set<String>,
        maxEntryBytes: Int
    ): Map<String, ByteArray> {
        val normalizedRequested = requested.map(::normalizeZipPath).toSet()
        val found = linkedMapOf<String, ByteArray>()
        source.openStream().use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = normalizeZipPath(entry.name)
                    if (!entry.isDirectory && name in normalizedRequested) {
                        found[name] = readCurrentEntry(zip, maxEntryBytes, name)
                        if (found.size == normalizedRequested.size) break
                    }
                    zip.closeEntry()
                }
            }
        }
        return found
    }

    private fun readCurrentEntry(zip: ZipInputStream, limit: Int, name: String): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = zip.read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) throw IOException("EPUB 条目过大：$name")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun parseXml(bytes: ByteArray): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        }
        return factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
    }

    private fun firstElement(document: Document, localName: String): Element? =
        document.getElementsByTagNameNS("*", localName).item(0) as? Element

    private fun elementText(document: Document, localName: String): String =
        firstElement(document, localName)?.textContent.orEmpty().trim()

    private fun extractHtmlTitle(html: String): String {
        val heading = Regex("<h[1-3][^>]*>(.*?)</h[1-3]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.groupValues?.getOrNull(1)
        val title = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.groupValues?.getOrNull(1)
        return decodeHtmlEntities(stripTags(heading ?: title.orEmpty())).trim()
    }

    private fun htmlToPlainText(html: String): String {
        val withoutNoise = html
            .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("</?(?:p|div|section|article|blockquote|li|h[1-6])[^>]*>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
        return decodeHtmlEntities(stripTags(withoutNoise))
            .replace("\r", "")
            .replace(Regex("[ \t]+\n"), "\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun stripTags(value: String): String = value.replace(Regex("<[^>]+>"), "")

    private fun decodeHtmlEntities(value: String): String = value
        .replace("&nbsp;", " ", ignoreCase = true)
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)

    private fun resolveZipPath(baseDir: String, relative: String): String =
        normalizeZipPath(listOf(baseDir, relative).filter { it.isNotBlank() }.joinToString("/"))

    private fun normalizeZipPath(path: String): String {
        val parts = mutableListOf<String>()
        path.replace('\\', '/').split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
                else -> parts += segment
            }
        }
        return parts.joinToString("/")
    }
}
