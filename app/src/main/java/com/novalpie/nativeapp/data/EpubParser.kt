package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.ParsedEpub
import com.novalpie.nativeapp.model.UploadChapter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URLDecoder
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element

object EpubParser {
    private const val CONTAINER_PATH = "META-INF/container.xml"
    private const val MAX_XML_BYTES = 4 * 1024 * 1024
    private const val MAX_CHAPTER_BYTES = 24 * 1024 * 1024
    private const val MAX_LOCAL_TEXT_BYTES = 64 * 1024 * 1024
    private val comments = Regex("<!--[\\s\\S]*?-->")
    private val htmlTag = Regex("""<(?:"[^"]*"|'[^']*'|[^'">])*>""")
    private data class TocEntry(val path: String, val fragment: String?, val title: String, val depth: Int, val parents: List<String>)

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
        var navigationPath: String? = null
        var ncxPath: String? = null
        val manifestNodes = packageDocument.getElementsByTagNameNS("*", "item")
        for (index in 0 until manifestNodes.length) {
            val item = manifestNodes.item(index) as? Element ?: continue
            val id = item.getAttribute("id")
            val href = item.getAttribute("href")
            if (id.isNotBlank() && href.isNotBlank()) {
                if (id in manifest) throw IOException("EPUB manifest中有重复ID：$id")
                val resolved = resolveZipPath(packageDir, decodePath(href.substringBefore('#')))
                manifest[id] = resolved
                if ("nav" in item.getAttribute("properties").split(Regex("\\s+"))) navigationPath = resolved
                if (item.getAttribute("media-type") == "application/x-dtbncx+xml") ncxPath = resolved
            }
        }

        val spinePaths = mutableListOf<String>()
        val spineNodes = packageDocument.getElementsByTagNameNS("*", "itemref")
        for (index in 0 until spineNodes.length) {
            val idRef = (spineNodes.item(index) as? Element)?.getAttribute("idref").orEmpty()
            val path = manifest[idRef] ?: throw IOException("EPUB 目录引用的章节不存在：$idRef")
            spinePaths += path
        }
        if (spinePaths.isEmpty()) throw IOException("EPUB 目录为空")

        val tocPath = navigationPath ?: ncxPath
        val toc = tocPath?.let { path ->
            val bytes = readEntries(source, setOf(path), MAX_XML_BYTES)[path] ?: throw IOException("EPUB 缺少导航目录文件：$path")
            readToc(bytes, path, navigationPath == null)
        }.orEmpty()
        val requested = (spinePaths + toc.map { it.path }).toSet()
        val chapterEntries = readEntries(source, requested, MAX_CHAPTER_BYTES, MAX_LOCAL_TEXT_BYTES)
        requested.firstOrNull { it !in chapterEntries }?.let { throw IOException("EPUB 缺少目录声明的章节：$it；未导入不完整书籍") }
        fun chapterHtml(path: String) = chapterEntries.getValue(path).toString(Charsets.UTF_8)
        val chapters = mutableListOf<UploadChapter>()
        fun add(path: String, html: String, title: String = "", depth: Int = 0, parents: List<String> = emptyList()) {
            val content = htmlToPlainText(html)
            if (content.isBlank()) return
            val label = title.ifBlank { extractHtmlTitle(html) }.ifBlank { "第 ${chapters.size + 1} 章" }
            chapters += UploadChapter((parents + label).joinToString(" / "), content, chapters.size + 1,
                hierarchyLevel = depth, sectionPath = parents, rawPath = path, spineIndex = spinePaths.indexOf(path).takeIf { it >= 0 })
        }
        if (toc.isEmpty()) spinePaths.forEach { path -> add(path, chapterHtml(path)) }
        else {
            // Keep front matter omitted from the navigation, rather than quietly discarding it.
            val firstIndex = spinePaths.indexOf(toc.first().path)
            if (firstIndex > 0) spinePaths.take(firstIndex).forEach { path -> add(path, chapterHtml(path)) }
            toc.forEachIndexed { index, entry ->
                val next = toc.getOrNull(index + 1)
                val startIndex = spinePaths.indexOf(entry.path)
                val nextIndex = next?.let { spinePaths.indexOf(it.path) } ?: spinePaths.size
                val paths = if (startIndex >= 0 && next?.path != entry.path && nextIndex > startIndex)
                    spinePaths.subList(startIndex, nextIndex) else listOf(entry.path)
                val first = bodyMarkup(chapterHtml(entry.path))
                val start = fragmentOffset(first, entry.fragment)
                val end = if (next?.path == entry.path) fragmentOffset(first, next.fragment) else first.length
                if (end < start) throw IOException("EPUB 同文件目录锚点顺序错误，未提交重复内容")
                if (index == 0 && start > 0) add(entry.path, first.substring(0, start))
                val html = buildString {
                    append(first.substring(start, end))
                    paths.drop(1).forEach { append("\n"); append(bodyMarkup(chapterHtml(it))) }
                }
                add(entry.path, html, entry.title, entry.depth, entry.parents)
            }
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
        maxEntryBytes: Int,
        maxTotalBytes: Int = maxEntryBytes,
    ): Map<String, ByteArray> {
        val normalizedRequested = requested.map(::normalizeZipPath).toSet()
        val found = linkedMapOf<String, ByteArray>()
        var totalBytes = 0L
        source.openStream().use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = normalizeZipPath(entry.name)
                    if (!entry.isDirectory && name in normalizedRequested) {
                        if (name in found) throw IOException("EPUB 中有重复章节路径：$name")
                        val remaining = (maxTotalBytes - totalBytes).coerceAtLeast(0).toInt()
                        val bytes = readCurrentEntry(zip, minOf(maxEntryBytes, remaining), name)
                        totalBytes += bytes.size
                        found[name] = bytes
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
            .find(comments.replace(bodyMarkup(html), ""))?.groupValues?.getOrNull(1)
        val title = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.groupValues?.getOrNull(1)
        return decodeHtmlEntities(stripTags(heading ?: title.orEmpty())).trim()
    }

    private fun htmlToPlainText(html: String): String {
        val withoutNoise = comments.replace(bodyMarkup(html), "")
            .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .let { markup -> htmlTag.replace(markup) { tag ->
                when (Regex("^</?([A-Za-z0-9]+)").find(tag.value)?.groupValues?.get(1)?.lowercase()) {
                    "p", "div", "section", "article", "blockquote", "li", "h1", "h2", "h3", "h4", "h5", "h6", "br" -> "\n"
                    else -> ""
                }
            } }
        return decodeHtmlEntities(stripTags(withoutNoise))
            .replace("\r", "")
            .replace(Regex("[ \t]+\n"), "\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun stripTags(value: String): String = htmlTag.replace(comments.replace(value, ""), "")

    private val entityNames = mapOf("nbsp" to " ", "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'",
        "ldquo" to "“", "rdquo" to "”", "lsquo" to "‘", "rsquo" to "’", "hellip" to "…", "mdash" to "—", "ndash" to "–",
        "ensp" to "\u2002", "emsp" to "\u2003", "thinsp" to "\u2009", "bull" to "•", "copy" to "©", "reg" to "®", "trade" to "™")
    private fun decodeHtmlEntities(value: String): String = Regex("&(#x[0-9a-fA-F]+|#[0-9]+|[A-Za-z]+);").replace(value) { match ->
        val name = match.groupValues[1]
        if (!name.startsWith('#')) entityNames[name.lowercase()] ?: match.value
        else {
            val number = if (name.startsWith("#x", true)) name.drop(2).toIntOrNull(16) else name.drop(1).toIntOrNull()
            if (number != null && Character.isValidCodePoint(number) && number !in 0xD800..0xDFFF) String(Character.toChars(number)) else match.value
        }
    }

    private fun bodyMarkup(html: String): String = Regex("<body\\b[^>]*>([\\s\\S]*?)</body\\s*>", RegexOption.IGNORE_CASE)
        .find(html)?.groupValues?.get(1) ?: html.replace(Regex("<head\\b[^>]*>[\\s\\S]*?</head\\s*>", RegexOption.IGNORE_CASE), "")

    private fun fragmentOffset(html: String, fragment: String?): Int {
        if (fragment.isNullOrEmpty()) return 0
        val attribute = Regex("""(?<![\w:-])(?:id|name)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""", RegexOption.IGNORE_CASE)
        val tag = htmlTag.findAll(html).firstOrNull { match -> attribute.findAll(match.value).any { found ->
            val value = found.groupValues.drop(1).firstOrNull { it.isNotEmpty() }.orEmpty()
            decodeHtmlEntities(value) == fragment
        } }
        return tag?.range?.first ?: throw IOException("EPUB 目录锚点不存在：$fragment；未重复导入整章")
    }

    private fun readToc(bytes: ByteArray, path: String, ncx: Boolean): List<TocEntry> {
        // Ignore the declaration itself; no DTD/entity network request is ever enabled.
        val markup = bytes.toString(Charsets.UTF_8).replace(Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE), "")
        val document = parseXml(markup.toByteArray(Charsets.UTF_8))
        val entries = mutableListOf<TocEntry>()
        fun anchor(href: String, title: String, depth: Int, parents: List<String>) {
            if (href.isBlank()) return
            if (href.contains(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:"))) throw IOException("EPUB 导航引用外部资源，不会自动获取")
            val relative = decodePath(href.substringBefore('#').substringBefore('?'))
            val target = if (relative.isBlank()) path else resolveZipPath(path.substringBeforeLast('/', ""), relative)
            entries += TocEntry(target, href.substringAfter('#', "").takeIf { it.isNotEmpty() }?.let(::decodePath), title.trim(), depth, parents)
        }
        if (ncx) {
            fun walk(element: Element, depth: Int, parents: List<String>) {
                val label = element.children().firstOrNull { it.local() == "navlabel" }?.textContent.orEmpty().trim()
                element.children().firstOrNull { it.local() == "content" }?.getAttribute("src")?.let { anchor(it, label, depth, parents) }
                element.children().filter { it.local() == "navpoint" }.forEach { walk(it, depth + 1, parents + listOf(label).filter { it.isNotBlank() }) }
            }
            firstElement(document, "navMap")?.children()?.filter { it.local() == "navpoint" }?.forEach { walk(it, 0, emptyList()) }
        } else {
            val navs = document.getElementsByTagNameNS("*", "nav")
            val nav = (0 until navs.length).mapNotNull { navs.item(it) as? Element }.firstOrNull {
                "toc" in (it.getAttribute("epub:type") + " " + it.getAttribute("type")).split(Regex("\\s+")) || it.getAttribute("role") == "doc-toc"
            }
            fun walk(list: Element, depth: Int, parents: List<String>) {
                list.children().filter { it.local() == "li" }.forEach { li ->
                    val labelElement = li.children().firstOrNull { it.local() in setOf("a", "span") }
                    val label = labelElement?.textContent.orEmpty().trim()
                    if (labelElement?.local() == "a") anchor(labelElement.getAttribute("href"), label, depth, parents)
                    li.children().filter { it.local() in setOf("ol", "ul") }.forEach { walk(it, depth + 1, parents + listOf(label).filter { it.isNotBlank() }) }
                }
            }
            nav?.children()?.filter { it.local() in setOf("ol", "ul") }?.forEach { walk(it, 0, emptyList()) }
        }
        return entries.distinctBy { it.path to it.fragment }
    }

    private fun Element.children(): List<Element> = (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }
    private fun Element.local() = (localName ?: tagName).lowercase()
    private fun decodePath(value: String) = try { URLDecoder.decode(value.replace("+", "%2B"), "UTF-8") }
        catch (failure: IllegalArgumentException) { throw IOException("EPUB 路径编码无效", failure) }

    private fun resolveZipPath(baseDir: String, relative: String): String =
        normalizeZipPath(if (relative.startsWith('/')) relative else listOf(baseDir, relative).filter { it.isNotBlank() }.joinToString("/"))

    private fun normalizeZipPath(path: String): String {
        val parts = mutableListOf<String>()
        path.replace('\\', '/').split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex) else throw IOException("EPUB 路径越出文件根目录")
                else -> parts += segment
            }
        }
        return parts.joinToString("/")
    }
}
