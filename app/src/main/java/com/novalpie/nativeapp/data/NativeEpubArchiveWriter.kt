package com.novalpie.nativeapp.data

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import kotlinx.coroutines.CancellationException
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Metadata used when the source site grants an EPUB export to the native app. */
data class NativeEpubMetadata(
    val title: String,
    val author: String,
    val description: String = "",
    val language: String = "zh",
    val publisher: String = "NovelPia",
)

/** A binary asset whose bytes must be copied without resizing or recompression. */
data class NativeEpubAsset(
    val mediaType: String? = null,
    val input: InputStream,
)

/** Progress is deliberately count-based so it remains useful for very large books. */
data class NativeEpubExportProgress(
    val totalChapters: Int = 0,
    val completedChapters: Int = 0,
    val totalImages: Int = 0,
    val completedImages: Int = 0,
    val failedImages: Int = 0,
    val currentChapterTitle: String? = null,
    val currentImageUrl: String? = null,
)

/**
 * Builds an EPUB from the source download stream without buffering the complete book or any
 * image in memory. The source TXT is read one line at a time, and each image is first staged in
 * a temporary file so a failed HTTP stream cannot leave a corrupt ZIP entry behind.
 */
object NativeEpubArchiveWriter {
    private const val EPUB_MIMETYPE = "application/epub+zip"
    private const val IMAGE_TOKEN_PREFIX = "__NOVALPIE_IMAGE_"
    private const val IMAGE_TOKEN_SUFFIX = "__"

    private val chapterHeadingPattern = Regex(
        "^\\s*(第\\s*[0-9０-９一二三四五六七八九十百千万零〇两]+\\s*章(?:\\s+.*)?|Chapter\\s+[0-9]+(?:\\s*[:.-].*)?)\\s*$",
        RegexOption.IGNORE_CASE,
    )
    private val bracketImagePattern = Regex("\\[图片\\s*[:：]\\s*([^]\\r\\n]+?)\\]")
    private val markdownImagePattern = Regex(
        "!\\[[^]]*]\\(\\s*<?([^)>\\s]+)>?[^)]*\\)",
        RegexOption.IGNORE_CASE,
    )
    private val htmlImagePattern = Regex(
        "<img\\b[^>]*?(?:src|data-src)\\s*=\\s*[\\\"']([^\\\"']+)[\\\"'][^>]*>",
        RegexOption.IGNORE_CASE,
    )

    private data class ChapterRecord(
        val index: Int,
        val title: String,
    )

    private data class ImageRecord(
        val path: String?,
        val mediaType: String?,
        val error: String? = null,
    )

    private data class ImageMatch(
        val range: IntRange,
        val url: String,
    )

    suspend fun write(
        output: OutputStream,
        metadata: NativeEpubMetadata,
        source: Reader,
        openAsset: suspend (String) -> NativeEpubAsset,
        onProgress: (NativeEpubExportProgress) -> Unit = {},
    ) {
        require(metadata.title.isNotBlank()) { "书名不能为空" }
        require(metadata.author.isNotBlank()) { "作者不能为空" }

        val chapters = mutableListOf<ChapterRecord>()
        val images = linkedMapOf<String, ImageRecord>()
        var completedImages = 0
        var failedImages = 0

        ZipOutputStream(output.buffered()).use { zip ->
            writeStoredText(zip, "mimetype", EPUB_MIMETYPE)
            writeText(zip, "META-INF/container.xml", containerXml())

            suspend fun flushChapter(title: String, body: String) {
                val chapterIndex = chapters.size + 1
                val chapterTitle = title.ifBlank { "第${chapterIndex}章" }
                val renderedBody = renderBody(
                    rawBody = body,
                    zip = zip,
                    imageRecords = images,
                    openAsset = openAsset,
                    onImageResult = { url, succeeded ->
                        if (succeeded) completedImages++ else failedImages++
                        onProgress(
                            NativeEpubExportProgress(
                                completedChapters = chapters.size,
                                totalImages = images.size,
                                completedImages = completedImages,
                                failedImages = failedImages,
                                currentChapterTitle = chapterTitle,
                                currentImageUrl = url,
                            )
                        )
                    },
                )
                writeText(zip, "OEBPS/chapter-$chapterIndex.xhtml", chapterXhtml(chapterTitle, renderedBody))
                chapters += ChapterRecord(chapterIndex, chapterTitle)
                onProgress(
                    NativeEpubExportProgress(
                        completedChapters = chapters.size,
                        completedImages = completedImages,
                        totalImages = images.size,
                        failedImages = failedImages,
                        currentChapterTitle = chapterTitle,
                    )
                )
            }

            BufferedReader(source).use { reader ->
                var currentTitle = ""
                val currentBody = StringBuilder()
                while (true) {
                    val rawLine = reader.readLine() ?: break
                    val line = if (currentTitle.isEmpty() && currentBody.isEmpty()) {
                        rawLine.removePrefix("\uFEFF")
                    } else {
                        rawLine
                    }
                    val heading = chapterHeadingPattern.matchEntire(line.trim())?.groupValues?.getOrNull(1)
                    if (heading != null) {
                        if (currentTitle.isNotBlank() || currentBody.isNotBlank()) {
                            flushChapter(currentTitle, currentBody.toString())
                            currentBody.clear()
                        }
                        currentTitle = heading.trim()
                    } else {
                        if (currentBody.isNotEmpty()) currentBody.append('\n')
                        currentBody.append(line)
                    }
                }
                if (currentTitle.isNotBlank() || currentBody.isNotBlank()) {
                    flushChapter(currentTitle, currentBody.toString())
                }
            }

            if (chapters.isEmpty()) throw IllegalArgumentException("EPUB 正文不能为空")
            writeText(zip, "OEBPS/Styles/style.css", stylesheet())
            writeText(zip, "OEBPS/nav.xhtml", navigationXhtml(metadata.title, chapters))
            writeText(zip, "OEBPS/content.opf", packageXml(metadata, chapters, images))
            onProgress(
                NativeEpubExportProgress(
                    totalChapters = chapters.size,
                    completedChapters = chapters.size,
                    totalImages = images.size,
                    completedImages = completedImages,
                    failedImages = failedImages,
                )
            )
        }
    }

    private suspend fun renderBody(
        rawBody: String,
        zip: ZipOutputStream,
        imageRecords: LinkedHashMap<String, ImageRecord>,
        openAsset: suspend (String) -> NativeEpubAsset,
        onImageResult: (url: String, succeeded: Boolean) -> Unit,
    ): String {
        val matches = imageMatches(rawBody)
        if (matches.isEmpty()) return paragraphs(escapeXml(rawBody))

        val rendered = StringBuilder()
        var cursor = 0
        for (match in matches) {
            rendered.append(escapeXml(rawBody.substring(cursor, match.range.first)))
            val url = normalizeAssetUrl(match.url)
            val existing = imageRecords[url]
            val record = if (existing != null) {
                existing
            } else {
                val loaded = loadImage(zip, url, openAsset, imageRecords.size + 1)
                imageRecords[url] = loaded
                onImageResult(url, loaded.path != null)
                loaded
            }
            val token = "$IMAGE_TOKEN_PREFIX${imageRecords.keys.indexOf(url)}$IMAGE_TOKEN_SUFFIX"
            rendered.append(token)
            cursor = match.range.last + 1
        }
        rendered.append(escapeXml(rawBody.substring(cursor)))

        val tokenPattern = Regex("${Regex.escape(IMAGE_TOKEN_PREFIX)}(\\d+)${Regex.escape(IMAGE_TOKEN_SUFFIX)}")
        val withImages = tokenPattern.replace(rendered.toString()) { result ->
            val index = result.groupValues[1].toIntOrNull() ?: return@replace ""
            val record = imageRecords.values.elementAtOrNull(index)
            if (record?.path != null) {
                "<img src=\"${escapeXml(record.path)}\" alt=\"插图 ${index + 1}\" class=\"chapter-image\" />"
            } else {
                "<span class=\"image-missing\">【${escapeXml(record?.error ?: "插图获取失败")}】</span>"
            }
        }
        return paragraphs(withImages)
    }

    private fun imageMatches(body: String): List<ImageMatch> {
        return (bracketImagePattern.findAll(body).mapNotNull { match ->
            match.groupValues.getOrNull(1)?.let { ImageMatch(match.range, it) }
        } + markdownImagePattern.findAll(body).mapNotNull { match ->
            match.groupValues.getOrNull(1)?.let { ImageMatch(match.range, it) }
        } + htmlImagePattern.findAll(body).mapNotNull { match ->
            match.groupValues.getOrNull(1)?.let { ImageMatch(match.range, it) }
        })
            .sortedBy { it.range.first }
            .distinctBy { it.range }
            .toList()
    }

    private suspend fun loadImage(
        zip: ZipOutputStream,
        url: String,
        openAsset: suspend (String) -> NativeEpubAsset,
        imageNumber: Int,
    ): ImageRecord {
        var temporary: File? = null
        return try {
            val asset = openAsset(url)
            temporary = File.createTempFile("novalpie-epub-", ".asset")
            var copiedBytes = 0L
            asset.input.use { input ->
                temporary.outputStream().buffered().use { fileOutput ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        fileOutput.write(buffer, 0, read)
                        copiedBytes += read
                    }
                }
            }
            if (copiedBytes <= 0L) throw IllegalStateException("图片内容为空")
            val mediaType = asset.mediaType?.substringBefore(';')?.trim()?.lowercase(Locale.US)
            val extension = imageExtension(mediaType, url)
            val path = "images/image-$imageNumber.$extension"
            zip.putNextEntry(ZipEntry("OEBPS/$path"))
            try {
                temporary.inputStream().buffered().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read > 0) zip.write(buffer, 0, read)
                    }
                }
            } finally {
                zip.closeEntry()
            }
            ImageRecord(path = path, mediaType = mediaType)
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            ImageRecord(
                path = null,
                mediaType = null,
                error = failure.message?.takeIf { it.isNotBlank() } ?: "插图获取失败",
            )
        } finally {
            temporary?.delete()
        }
    }

    private fun imageExtension(mediaType: String?, url: String): String = when {
        mediaType == "image/webp" -> "webp"
        mediaType == "image/png" -> "png"
        mediaType == "image/gif" -> "gif"
        mediaType == "image/avif" -> "avif"
        mediaType == "image/svg+xml" -> "svg"
        mediaType == "image/bmp" -> "bmp"
        mediaType == "image/jpeg" || mediaType == "image/jpg" -> "jpg"
        else -> url.substringBefore('?').substringBefore('#').substringAfterLast('.', "bin")
            .lowercase(Locale.US)
            .filter { it.isLetterOrDigit() }
            .takeIf { it.length in 1..5 }
            ?: "bin"
    }

    private fun normalizeAssetUrl(value: String): String {
        val trimmed = value.trim().trim('<', '>', '"', '\'')
        return when {
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("/") ||
                trimmed.startsWith("./") -> trimmed
            else -> "https://$trimmed"
        }
    }

    private fun paragraphs(value: String): String = value
        .replace("\r", "")
        .split(Regex("\\n\\s*\\n"))
        .filter { it.isNotBlank() }
        .joinToString("\n") { paragraph ->
            "<p>${paragraph.replace("\n", "<br />")}</p>"
        }

    private fun writeStoredText(zip: ZipOutputStream, path: String, text: String) {
        val bytes = text.toByteArray(Charsets.US_ASCII)
        val crc = CRC32().apply { update(bytes) }
        val entry = ZipEntry(path).apply {
            method = ZipEntry.STORED
            size = bytes.size.toLong()
            compressedSize = bytes.size.toLong()
            this.crc = crc.value
        }
        zip.putNextEntry(entry)
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun writeText(zip: ZipOutputStream, path: String, text: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(text.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun containerXml(): String = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
</container>"""

    private fun packageXml(
        metadata: NativeEpubMetadata,
        chapters: List<ChapterRecord>,
        images: Map<String, ImageRecord>,
    ): String {
        val chapterManifest = chapters.joinToString("\n") { chapter ->
            "    <item id=\"chapter-${chapter.index}\" href=\"chapter-${chapter.index}.xhtml\" media-type=\"application/xhtml+xml\"/>"
        }
        val imageManifest = images.values.mapIndexedNotNull { index, image ->
            image.path?.let { path ->
                "    <item id=\"image-${index + 1}\" href=\"$path\" media-type=\"${escapeXml(image.mediaType ?: mediaTypeForPath(path))}\"/>"
            }
        }.joinToString("\n")
        val spine = chapters.joinToString("\n") { chapter ->
            "    <itemref idref=\"chapter-${chapter.index}\"/>"
        }
        return """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="book-id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="book-id">urn:novalpie:${escapeXml(metadata.title.hashCode().toString())}</dc:identifier>
    <dc:title>${escapeXml(metadata.title)}</dc:title>
    <dc:creator>${escapeXml(metadata.author)}</dc:creator>
    <dc:language>${escapeXml(metadata.language.ifBlank { "zh" })}</dc:language>
    <dc:publisher>${escapeXml(metadata.publisher)}</dc:publisher>
    <dc:description>${escapeXml(metadata.description)}</dc:description>
  </metadata>
  <manifest>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
    <item id="style" href="Styles/style.css" media-type="text/css"/>
$chapterManifest
$imageManifest
  </manifest>
  <spine>
$spine
  </spine>
</package>"""
    }

    private fun navigationXhtml(title: String, chapters: List<ChapterRecord>): String {
        val links = chapters.joinToString("\n") { chapter ->
            "      <li><a href=\"chapter-${chapter.index}.xhtml\">${escapeXml(chapter.title)}</a></li>"
        }
        return """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
<head><title>${escapeXml(title)}</title></head>
<body><nav epub:type="toc"><h1>目录</h1><ol>
$links
</ol></nav></body></html>"""
    }

    private fun chapterXhtml(title: String, body: String): String = """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
<head><meta charset="UTF-8"/><title>${escapeXml(title)}</title><link rel="stylesheet" type="text/css" href="Styles/style.css"/></head>
<body><h1>${escapeXml(title)}</h1>
$body
</body></html>"""

    private fun stylesheet(): String = """body { font-family: serif; line-height: 1.8; margin: 1.2em; }
h1 { text-align: center; font-size: 1.35em; margin: 0 0 1.5em; }
p { margin: 0 0 0.9em; text-indent: 2em; }
.chapter-image { display: block; max-width: 100%; height: auto; margin: 1em auto; }
.image-missing { color: #a33; }"""

    private fun mediaTypeForPath(path: String): String = when (path.substringAfterLast('.', "").lowercase(Locale.US)) {
        "webp" -> "image/webp"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "avif" -> "image/avif"
        "svg" -> "image/svg+xml"
        "bmp" -> "image/bmp"
        else -> "image/jpeg"
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
