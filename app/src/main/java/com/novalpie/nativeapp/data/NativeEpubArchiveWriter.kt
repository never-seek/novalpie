package com.novalpie.nativeapp.data

import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.util.concurrent.atomic.AtomicInteger
import java.util.LinkedHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val NATIVE_EPUB_LOG_TAG = "NovalPieEpub"

private fun logNativeEpubDiagnostic(message: String, failure: Throwable? = null) {
    // Local JVM tests use the Android SDK's "not mocked" stubs. Diagnostics must never change
    // the cache/export result when the platform logger is unavailable, so keep logging best-effort.
    runCatching {
        if (failure == null) {
            Log.w(NATIVE_EPUB_LOG_TAG, message)
        } else {
            Log.e(NATIVE_EPUB_LOG_TAG, message, failure)
        }
    }
}

/** Metadata used when the source site grants an EPUB export to the native app. */
data class NativeEpubMetadata(
    val title: String,
    val author: String,
    val description: String = "",
    val language: String = "zh",
    val publisher: String = "NovelPia",
    /** The source EPUB contains the full-resolution cover as a separate image entry. */
    val coverUrl: String? = null,
)

/** A binary asset whose bytes must be copied without resizing or recompression. */
data class NativeEpubAsset(
    val mediaType: String? = null,
    val input: InputStream,
    /** Called once after the writer has copied (or abandoned) this stream. */
    val onConsumed: (() -> Unit)? = null,
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

/** Metadata captured during the same pass that copies an asset to its staging file. */
internal data class NativeEpubStagedFile(
    val file: File,
    val mediaType: String?,
    val size: Long,
    val crc: Long,
)

/**
 * A small LRU cache for staged image files.
 *
 * The writer only needs staged bytes while it is emitting the current chapter. Keeping every
 * successful image until the EPUB closes duplicates the whole book on disk, so entries are
 * evicted by byte size as soon as they are no longer protected by the active chapter.
 */
internal class NativeEpubStagedAssetCache(
    maxBytes: Long,
) {
    private val limitBytes = maxBytes.coerceAtLeast(1L)
    private val entries = LinkedHashMap<String, NativeEpubStagedFile>(16, 0.75f, true)
    private var totalBytes = 0L

    val sizeBytes: Long
        get() = synchronized(this) { totalBytes }

    fun get(key: String): NativeEpubStagedFile? = synchronized(this) {
        val value = entries[key] ?: return@synchronized null
        val actualSize = if (value.file.isFile) value.file.length() else -1L
        if (actualSize != value.size) {
            logNativeEpubDiagnostic(
                "discarding staged cache entry key=$key file=${value.file.name} expected=${value.size} actual=$actualSize"
            )
            entries.remove(key)
            totalBytes -= value.size.coerceAtLeast(0L)
            value.file.delete()
            return@synchronized null
        }
        value
    }

    fun put(
        key: String,
        value: NativeEpubStagedFile,
        protectedKeys: Set<String> = emptySet(),
    ) {
        synchronized(this) {
            entries.remove(key)?.let { previous ->
                totalBytes -= previous.size.coerceAtLeast(0L)
                val previousSize = if (previous.file.isFile) previous.file.length() else -1L
                if (previousSize != previous.size) {
                    logNativeEpubDiagnostic(
                        "replacing damaged staged cache entry key=$key file=${previous.file.name} expected=${previous.size} actual=$previousSize"
                    )
                }
                if (previous.file != value.file) previous.file.delete()
            }
            entries[key] = value
            totalBytes += value.size.coerceAtLeast(0L)
            // Keep the newly inserted value alive even when it is larger than the soft limit;
            // the caller may be about to write it. It becomes eligible on the next trim.
            trimLocked(protectedKeys + key)
        }
    }

    fun trim(protectedKeys: Set<String> = emptySet()) {
        synchronized(this) {
            trimLocked(protectedKeys)
        }
    }

    fun clear() {
        synchronized(this) {
            val files = entries.values.map { it.file }.distinct()
            entries.clear()
            totalBytes = 0L
            files.forEach { it.delete() }
        }
    }

    private fun trimLocked(protectedKeys: Set<String>) {
        if (totalBytes <= limitBytes) return
        val iterator = entries.entries.iterator()
        while (totalBytes > limitBytes && iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key in protectedKeys) continue
            val staged = entry.value
            iterator.remove()
            totalBytes -= staged.size.coerceAtLeast(0L)
            staged.file.delete()
        }
    }
}

/** Removes only files left by an interrupted native EPUB asset pass. */
internal fun cleanupNativeEpubTempFiles(directory: File): Int {
    val files = directory.listFiles().orEmpty()
    var deleted = 0
    files.forEach { file ->
        if (file.isFile && (file.name.startsWith("novalpie-asset-") ||
                file.name.startsWith("novalpie-epub-")) && file.delete()
        ) {
            deleted++
        }
    }
    return deleted
}

private fun detectNativeEpubMediaType(declared: String?, header: ByteArray): String? {
    val normalized = declared
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase(Locale.US)
        ?.takeIf(String::isNotBlank)
    if (normalized?.startsWith("image/") == true) return normalized

    fun startsWith(vararg bytes: Int): Boolean =
        header.size >= bytes.size && bytes.indices.all { index ->
            (header[index].toInt() and 0xFF) == bytes[index]
        }

    if (startsWith(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return "image/png"
    if (startsWith(0xFF, 0xD8, 0xFF)) return "image/jpeg"
    if (header.size >= 6 && String(header, 0, 6, Charsets.US_ASCII) in setOf("GIF87a", "GIF89a")) {
        return "image/gif"
    }
    if (header.size >= 12 &&
        String(header, 0, 4, Charsets.US_ASCII) == "RIFF" &&
        String(header, 8, 4, Charsets.US_ASCII) == "WEBP"
    ) {
        return "image/webp"
    }
    if (header.size >= 12 &&
        String(header, 4, 4, Charsets.US_ASCII) == "ftyp" &&
        String(header, 8, 4, Charsets.US_ASCII) in setOf("avif", "avis")
    ) {
        return "image/avif"
    }
    if (startsWith(0x42, 0x4D)) return "image/bmp"

    val textHeader = String(header, Charsets.UTF_8).trimStart('\uFEFF', ' ', '\t', '\r', '\n')
    if (textHeader.startsWith("<svg", ignoreCase = true) ||
        (textHeader.startsWith("<?xml", ignoreCase = true) &&
            textHeader.contains("<svg", ignoreCase = true))
    ) {
        return "image/svg+xml"
    }
    return normalized?.takeUnless { it == "application/octet-stream" || it == "binary/octet-stream" }
}

/** Copies an asset once while calculating the exact metadata required by a STORE ZIP entry. */
internal fun stageNativeEpubFile(
    input: InputStream,
    mediaType: String?,
    destination: File,
): NativeEpubStagedFile {
    val crc = CRC32()
    var size = 0L
    val header = ByteArray(32)
    var headerSize = 0
    // Never expose the destination while the network bytes are still being copied. Android's
    // cache/filesystem can be observed by another worker (or an interrupted old job) between two
    // reads; publishing a partially written final path lets that observer retain a bad length and
    // later makes FileInputStream stop early even though the path metadata looks complete. The
    // sibling temporary file is renamed only after the copy and buffered flush finish.
    val temporary = File.createTempFile("novalpie-stage-", ".part", destination.parentFile)
    input.use { source ->
        try {
            FileOutputStream(temporary).buffered().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = source.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                output.write(buffer, 0, read)
                crc.update(buffer, 0, read)
                if (headerSize < header.size) {
                    val copied = minOf(read, header.size - headerSize)
                    System.arraycopy(buffer, 0, header, headerSize, copied)
                    headerSize += copied
                }
                size += read
            }
            }
            if (size <= 0L) throw IllegalStateException("图片内容为空")
            if (destination.exists() && !destination.delete()) {
                throw IllegalStateException("无法替换 EPUB 阶段文件")
            }
            if (!temporary.renameTo(destination)) {
                throw IllegalStateException("无法发布 EPUB 阶段文件")
            }
        } catch (failure: Throwable) {
            temporary.delete()
            throw failure
        }
    }
    return NativeEpubStagedFile(
        file = destination,
        mediaType = detectNativeEpubMediaType(mediaType, header.copyOf(headerSize)),
        size = size,
        crc = crc.value,
    )
}

/**
 * Builds an EPUB from the source download stream without buffering the complete book or any
 * image in memory. The source TXT is read one line at a time, and each image is first staged in
 * a temporary file so a failed HTTP stream cannot leave a corrupt ZIP entry behind.
 */
object NativeEpubArchiveWriter {
    private const val EPUB_MIMETYPE = "application/epub+zip"
    private const val IMAGE_TOKEN_PREFIX = "__NOVALPIE_IMAGE_"
    private const val IMAGE_TOKEN_SUFFIX = "__"
    private const val IMAGE_OPEN_MAX_ATTEMPTS = 4
    private const val IMAGE_RETRY_DELAY_MILLIS = 150L
    private const val DEFAULT_IMAGE_CONCURRENCY = 6
    internal const val DEFAULT_STAGED_ASSET_CACHE_MAX_BYTES = 128L * 1024L * 1024L

    private val chapterHeadingPattern = Regex(
        "^\\s*(第\\s*[0-9０-９一二三四五六七八九十百千万零〇两]+\\s*(?:章|话|話|節|节)(?:\\s+.*)?|Chapter\\s+[0-9]+(?:\\s*[:.-].*)?)\\s*$",
        RegexOption.IGNORE_CASE,
    )
    /**
     * Keep this in lockstep with the website EPUB generator. It bundles only the source's
     * `[图片 ...]` placeholders; arbitrary Markdown/HTML image tags remain document content and
     * must not silently become additional EPUB assets.
     */
    private val bracketImagePattern = Regex("\\[图片(?:[:：]|\\s)*?(.*?)\\]")

    /** The paid download TXT starts with a metadata block that is not a readable chapter. */
    private fun isDownloadMetadataPreamble(body: String): Boolean {
        val normalized = body
            .replace("\u200B", "")
            .replace("\u200C", "")
            .replace("\u200D", "")
        val hasChapterCount = normalized.contains("总章节数")
        val hasIdentity = normalized.contains("书名") || normalized.contains("metadata")
        val hasSeparator = normalized.contains("====")
        return hasChapterCount && (hasIdentity || hasSeparator)
    }

    private fun chapterNumber(heading: String): Int? {
        val token = Regex("^\\s*第\\s*([0-9０-９一二三四五六七八九十百千万零〇两]+)")
            .find(heading)
            ?.groupValues
            ?.getOrNull(1)
            ?: Regex("^\\s*Chapter\\s+([0-9]+)", RegexOption.IGNORE_CASE)
                .find(heading)
                ?.groupValues
                ?.getOrNull(1)
            ?: return null
        val asciiDigits = token.map { character ->
            when (character) {
                in '０'..'９' -> ('0'.code + (character.code - '０'.code)).toChar()
                else -> character
            }
        }.joinToString("")
        if (asciiDigits.all(Char::isDigit)) return asciiDigits.toIntOrNull()

        val digits = mapOf(
            '零' to 0,
            '〇' to 0,
            '一' to 1,
            '二' to 2,
            '两' to 2,
            '三' to 3,
            '四' to 4,
            '五' to 5,
            '六' to 6,
            '七' to 7,
            '八' to 8,
            '九' to 9,
        )
        val units = mapOf('十' to 10, '百' to 100, '千' to 1000, '万' to 10_000)
        var total = 0
        var section = 0
        var pending = 0
        for (character in token) {
            val digit = digits[character]
            if (digit != null) {
                pending = digit
                continue
            }
            val unit = units[character] ?: return null
            if (unit == 10_000) {
                section += if (pending == 0) 1 else pending
                total += section * unit
                section = 0
            } else {
                section += (if (pending == 0) 1 else pending) * unit
            }
            pending = 0
        }
        return total + section + pending
    }

    private fun isChapterBoundary(heading: String, previousNumber: Int?): Boolean {
        val currentNumber = chapterNumber(heading) ?: return true
        return previousNumber == null || currentNumber > previousNumber
    }

    private data class ChapterRecord(
        val index: Int,
        val title: String,
    )

    private data class ImageRecord(
        val path: String?,
        val mediaType: String?,
        val error: String? = null,
    )

    /** One staged source asset can be emitted for several repeated image occurrences. */
    private data class StagedAsset(
        val file: File?,
        val mediaType: String?,
        val size: Long = 0L,
        val crc: Long = 0L,
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
        imageConcurrency: Int = DEFAULT_IMAGE_CONCURRENCY,
        stagingDirectory: File? = null,
        stagedAssetCacheMaxBytes: Long = DEFAULT_STAGED_ASSET_CACHE_MAX_BYTES,
        onProgress: (NativeEpubExportProgress) -> Unit = {},
    ) {
        require(metadata.title.isNotBlank()) { "书名不能为空" }
        require(metadata.author.isNotBlank()) { "作者不能为空" }

        val chapters = mutableListOf<ChapterRecord>()
        // Keep one EPUB manifest item per source placeholder occurrence, matching the website.
        // Successful bytes may still be reused through stagedAssets; network deduplication must
        // not change how many illustrations the exported document contains.
        val images = mutableListOf<ImageRecord>()
        val stagedAssets = NativeEpubStagedAssetCache(stagedAssetCacheMaxBytes)
        val effectiveImageConcurrency = imageConcurrency.coerceAtLeast(1)
        var completedImages = 0
        var failedImages = 0
        var nextImageNumber = 1

        try {
            ZipOutputStream(output.buffered()).use { zip ->
                writeStoredText(zip, "mimetype", EPUB_MIMETYPE)
                writeText(zip, "META-INF/container.xml", containerXml())

                val coverRecord = metadata.coverUrl
                    ?.let(::normalizeAssetUrl)
                    ?.takeIf(String::isNotBlank)
                    ?.let { coverUrl ->
                        stagedAssets.trim(protectedKeys = setOf(coverUrl))
                        val staged = stageAsset(
                            stagedAssets = stagedAssets,
                            url = coverUrl,
                            openAsset = openAsset,
                            protectedKeys = setOf(coverUrl),
                            stagingDirectory = stagingDirectory,
                        )
                        val record = writeStagedImage(
                            zip = zip,
                            staged = staged,
                            path = "images/cover.${imageExtension(staged.mediaType, coverUrl)}",
                        )
                        stagedAssets.trim()
                        record
                    }
                // A cover-image manifest property is valid EPUB 3, but a number of Android
                // readers still discover the cover only through a dedicated XHTML page in the
                // spine. Keep the original bytes and media type; the page is just a compatibility
                // entry and never duplicates the image asset.
                coverRecord?.let { cover ->
                    if (cover.path != null) {
                        writeText(zip, "OEBPS/cover.xhtml", coverPageXhtml(metadata, cover))
                    }
                }

                // The website reports illustration progress for chapter descriptors only. The
                // cover is a separate EPUB phase and must not make the displayed image count one
                // larger than the source chapter image baseline.
                fun totalImageCount(): Int = images.size
                fun completedImageCount(): Int = completedImages
                fun failedImageCount(): Int = failedImages

                suspend fun flushChapter(title: String, body: String) {
                    val chapterIndex = chapters.size + 1
                    val chapterTitle = title.ifBlank { "第${chapterIndex}章" }
                    val renderedBody = renderBody(
                        rawBody = body,
                        zip = zip,
                        imageRecords = images,
                        stagedAssets = stagedAssets,
                        openAsset = openAsset,
                        imageConcurrency = effectiveImageConcurrency,
                        stagingDirectory = stagingDirectory,
                        nextImageNumber = { nextImageNumber++ },
                        onImageResult = { url, succeeded ->
                            if (succeeded) completedImages++ else failedImages++
                            onProgress(
                                NativeEpubExportProgress(
                                    completedChapters = chapters.size,
                                    totalImages = totalImageCount(),
                                    completedImages = completedImageCount(),
                                    failedImages = failedImageCount(),
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
                            completedImages = completedImageCount(),
                            totalImages = totalImageCount(),
                            failedImages = failedImageCount(),
                            currentChapterTitle = chapterTitle,
                        )
                    )
                    stagedAssets.trim()
                }

                BufferedReader(source).use { reader ->
                    var currentTitle = ""
                    val currentBody = StringBuilder()
                    var hasWrittenChapter = false
                    var lastChapterNumber: Int? = null
                    while (true) {
                        val rawLine = reader.readLine() ?: break
                        val line = if (currentTitle.isEmpty() && currentBody.isEmpty()) {
                            rawLine.removePrefix("\uFEFF")
                        } else {
                            rawLine
                        }
                        val heading = chapterHeadingPattern.matchEntire(line.trim())?.groupValues?.getOrNull(1)
                        val metadataPreamble = heading != null &&
                            !hasWrittenChapter &&
                            isDownloadMetadataPreamble(currentBody.toString())
                        if (heading != null && (metadataPreamble || isChapterBoundary(heading, lastChapterNumber))) {
                            if (!hasWrittenChapter && isDownloadMetadataPreamble(currentBody.toString())) {
                                // Some source exports put the metadata before the first heading;
                                // others put a bare pseudo-heading before the metadata. In both
                                // forms it must disappear before the first real chapter.
                                currentTitle = ""
                                currentBody.clear()
                            } else if (currentTitle.isNotBlank() || currentBody.isNotBlank()) {
                                flushChapter(currentTitle, currentBody.toString())
                                hasWrittenChapter = true
                                currentBody.clear()
                            }
                            currentTitle = heading.trim()
                            lastChapterNumber = chapterNumber(currentTitle) ?: lastChapterNumber
                        } else {
                            if (currentBody.isNotEmpty()) currentBody.append('\n')
                            currentBody.append(line)
                        }
                    }
                    if (currentTitle.isNotBlank() || currentBody.isNotBlank()) {
                        if (!hasWrittenChapter && isDownloadMetadataPreamble(currentBody.toString())) {
                            currentTitle = ""
                            currentBody.clear()
                        } else {
                            flushChapter(currentTitle, currentBody.toString())
                        }
                    }
                }

                if (chapters.isEmpty()) throw IllegalArgumentException("EPUB 正文不能为空")
                writeText(zip, "OEBPS/Styles/style.css", stylesheet())
                writeText(zip, "OEBPS/nav.xhtml", navigationXhtml(metadata.title, chapters))
                writeText(zip, "OEBPS/content.opf", packageXml(metadata, chapters, images, coverRecord))
                onProgress(
                    NativeEpubExportProgress(
                        totalChapters = chapters.size,
                        completedChapters = chapters.size,
                        totalImages = totalImageCount(),
                        completedImages = completedImageCount(),
                        failedImages = failedImageCount(),
                    )
                )
            }
        } finally {
            stagedAssets.clear()
        }
    }

    private suspend fun renderBody(
        rawBody: String,
        zip: ZipOutputStream,
        imageRecords: MutableList<ImageRecord>,
        stagedAssets: NativeEpubStagedAssetCache,
        openAsset: suspend (String) -> NativeEpubAsset,
        imageConcurrency: Int,
        stagingDirectory: File?,
        nextImageNumber: () -> Int,
        onImageResult: (url: String, succeeded: Boolean) -> Unit,
    ): String {
        val matches = imageMatches(rawBody)
        if (matches.isEmpty()) return paragraphs(escapeXml(rawBody))

        // Fetch assets concurrently, but keep ZIP writes below in source order. This mirrors the
        // website's six-worker image phase without allowing completion order to change chapter
        // references or image numbering.
        val stagedByUrl = stageAssetsConcurrently(
            urls = matches.map(ImageMatch::url),
            stagedAssets = stagedAssets,
            openAsset = openAsset,
            imageConcurrency = imageConcurrency,
            stagingDirectory = stagingDirectory,
        )

        val rendered = StringBuilder()
        var cursor = 0
        for (match in matches) {
            rendered.append(escapeXml(rawBody.substring(cursor, match.range.first)))
            val url = match.url
            // A failed first staging attempt must be retried for this occurrence, just as the
            // website retries each descriptor independently. Successful staged bytes are shared.
            val staged = stagedByUrl[url]
                ?.takeIf(::isStagedAssetIntact)
                ?: stageAsset(
                    stagedAssets = stagedAssets,
                    url = url,
                    openAsset = openAsset,
                    protectedKeys = matches.map(ImageMatch::url).toSet(),
                    stagingDirectory = stagingDirectory,
                )
            val record = writeStagedImage(
                zip = zip,
                staged = staged,
                path = "images/image-${nextImageNumber()}.${imageExtension(staged.mediaType, url)}",
            )
            imageRecords += record
            val index = imageRecords.lastIndex
            onImageResult(url, record.path != null)
            val token = "$IMAGE_TOKEN_PREFIX${index}$IMAGE_TOKEN_SUFFIX"
            rendered.append(token)
            cursor = match.range.last + 1
        }
        rendered.append(escapeXml(rawBody.substring(cursor)))

        val tokenPattern = Regex("${Regex.escape(IMAGE_TOKEN_PREFIX)}(\\d+)${Regex.escape(IMAGE_TOKEN_SUFFIX)}")
        val withImages = tokenPattern.replace(rendered.toString()) { result ->
            val index = result.groupValues[1].toIntOrNull() ?: return@replace ""
            val record = imageRecords.getOrNull(index)
            if (record?.path != null) {
                "<img src=\"${escapeXml(record.path)}\" alt=\"插图 ${index + 1}\" class=\"chapter-image\" />"
            } else {
                "<span class=\"image-missing\">【${escapeXml(record?.error ?: "插图获取失败")}】</span>"
            }
        }
        return paragraphs(withImages)
    }

    private fun isStagedAssetIntact(staged: StagedAsset): Boolean =
        staged.file?.let { file -> file.isFile && file.length() == staged.size } == true

    private suspend fun stageAssetsConcurrently(
        urls: List<String>,
        stagedAssets: NativeEpubStagedAssetCache,
        openAsset: suspend (String) -> NativeEpubAsset,
        imageConcurrency: Int,
        stagingDirectory: File?,
    ): Map<String, StagedAsset> {
        val uniqueUrls = urls.distinct()
        if (uniqueUrls.isEmpty()) return emptyMap()
        val protectedKeys = uniqueUrls.toSet()
        stagedAssets.trim(protectedKeys)
        val results = arrayOfNulls<StagedAsset>(uniqueUrls.size)
        val nextIndex = AtomicInteger(0)
        coroutineScope {
            val workerCount = minOf(imageConcurrency, uniqueUrls.size)
            List(workerCount) {
                launch(Dispatchers.IO) {
                    while (true) {
                        val index = nextIndex.getAndIncrement()
                        if (index >= uniqueUrls.size) break
                        val url = uniqueUrls[index]
                        results[index] = stageAsset(
                            stagedAssets = stagedAssets,
                            url = url,
                            openAsset = openAsset,
                            protectedKeys = protectedKeys,
                            stagingDirectory = stagingDirectory,
                        )
                    }
                }
            }.joinAll()
        }
        return uniqueUrls.mapIndexed { index, url ->
            url to (results[index] ?: StagedAsset(file = null, mediaType = null, error = "插图获取失败"))
        }.toMap()
    }

    private fun imageMatches(body: String): List<ImageMatch> {
        return bracketImagePattern.findAll(body).mapNotNull { match ->
            match.groupValues.getOrNull(1)
                ?.let(::normalizeAssetUrl)
                ?.takeIf(String::isNotBlank)
                ?.let { ImageMatch(match.range, it) }
        }
            .sortedBy { it.range.first }
            .toList()
    }

    private suspend fun stageAsset(
        stagedAssets: NativeEpubStagedAssetCache,
        url: String,
        openAsset: suspend (String) -> NativeEpubAsset,
        protectedKeys: Set<String>,
        stagingDirectory: File?,
    ): StagedAsset {
        // Share only successful bytes. The website retries each descriptor independently; caching
        // a transient failure would turn one bad request into missing images for every later
        // occurrence of the same URL.
        stagedAssets.get(url)?.let { staged ->
            return StagedAsset(
                file = staged.file,
                mediaType = staged.mediaType,
                size = staged.size,
                crc = staged.crc,
            )
        }
        val staged = stageAssetUncached(url, openAsset, stagingDirectory)
        if (staged.file != null) {
            stagedAssets.put(
                key = url,
                value = NativeEpubStagedFile(
                    file = staged.file,
                    mediaType = staged.mediaType,
                    size = staged.size,
                    crc = staged.crc,
                ),
                protectedKeys = protectedKeys,
            )
        }
        return staged
    }

    private suspend fun stageAssetUncached(
        url: String,
        openAsset: suspend (String) -> NativeEpubAsset,
        stagingDirectory: File?,
    ): StagedAsset {
        var lastFailure: Throwable? = null
        repeat(IMAGE_OPEN_MAX_ATTEMPTS) { attempt ->
            var temporary: File? = null
            try {
                val asset = openAsset(url)
                try {
                    val destination = File.createTempFile("novalpie-epub-", ".asset", stagingDirectory)
                    temporary = destination
                val staged = stageNativeEpubFile(
                    input = asset.input,
                    mediaType = asset.mediaType,
                    destination = destination,
                )
                val copiedBytes = staged.size
                val mediaType = staged.mediaType
                if (copiedBytes <= 0L) throw IllegalStateException("图片内容为空")
                return StagedAsset(
                    file = staged.file,
                    mediaType = mediaType,
                    size = staged.size,
                    crc = staged.crc,
                )
                } finally {
                    // The producer owns its network spool. Release it immediately after the
                    // writer copies it into the staging file, even when this attempt fails.
                    runCatching { asset.onConsumed?.invoke() }
                }
            } catch (failure: Throwable) {
                if (failure is CancellationException) throw failure
                temporary?.delete()
                lastFailure = failure
                if (attempt + 1 < IMAGE_OPEN_MAX_ATTEMPTS) {
                    delay(IMAGE_RETRY_DELAY_MILLIS)
                }
            }
        }
        return StagedAsset(
            file = null,
            mediaType = null,
            error = lastFailure?.message?.takeIf { it.isNotBlank() } ?: "插图获取失败",
        )
    }

    private fun writeStagedImage(
        zip: ZipOutputStream,
        staged: StagedAsset,
        path: String,
    ): ImageRecord {
        if (staged.file == null) {
            return ImageRecord(path = null, mediaType = null, error = staged.error)
        }
        val entry = ZipEntry("OEBPS/$path").apply {
            method = ZipEntry.STORED
            this.size = staged.size
            compressedSize = staged.size
            this.crc = staged.crc
        }
        zip.putNextEntry(entry)
        try {
            var written = 0L
            staged.file.inputStream().buffered().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read > 0) {
                        zip.write(buffer, 0, read)
                        written += read
                    }
                }
            }
            if (written != staged.size) {
                throw java.io.IOException(
                    "staged image changed while writing: expected ${staged.size} bytes but read $written from ${staged.file.name}",
                )
            }
        } finally {
            try {
                zip.closeEntry()
            } catch (failure: Throwable) {
                val actualSize = if (staged.file.isFile) staged.file.length() else -1L
                logNativeEpubDiagnostic(
                    "ZIP image entry failed path=$path file=${staged.file.name} expected=${staged.size} actual=$actualSize",
                    failure,
                )
                throw failure
            }
        }
        return ImageRecord(path = path, mediaType = staged.mediaType)
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
        var trimmed = value
            .trim()
            .replace(Regex("\\s+"), "")
        if (trimmed.isBlank()) return ""
        trimmed = trimmed.replace(Regex("^[:：]+"), "")
        if (trimmed.startsWith("图片://")) trimmed = trimmed.substring(5)
        return when {
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("./") -> trimmed
            else -> "https://${trimmed.trimStart('/')}"
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
        writeStoredBytes(zip, path, bytes)
    }

    private fun writeText(zip: ZipOutputStream, path: String, text: String) {
        writeStoredBytes(zip, path, text.toByteArray(Charsets.UTF_8))
    }

    private fun writeStoredBytes(zip: ZipOutputStream, path: String, bytes: ByteArray) {
        val crc = CRC32().apply { update(bytes) }
        val entry = ZipEntry(path).apply {
            method = ZipEntry.STORED
            size = bytes.size.toLong()
            compressedSize = bytes.size.toLong()
            this.crc = crc.value
        }
        zip.putNextEntry(entry)
        try {
            zip.write(bytes)
        } finally {
            zip.closeEntry()
        }
    }

    private fun containerXml(): String = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
</container>"""

    private fun packageXml(
        metadata: NativeEpubMetadata,
        chapters: List<ChapterRecord>,
        images: List<ImageRecord>,
        cover: ImageRecord?,
    ): String {
        val chapterManifest = chapters.joinToString("\n") { chapter ->
            "    <item id=\"chapter-${chapter.index}\" href=\"chapter-${chapter.index}.xhtml\" media-type=\"application/xhtml+xml\"/>"
        }
        val coverManifest = cover?.path?.let { path ->
            "    <item id=\"cover-image\" href=\"$path\" media-type=\"${escapeXml(cover.mediaType ?: mediaTypeForPath(path))}\" properties=\"cover-image\"/>"
        }.orEmpty()
        val coverPageManifest = cover?.path?.let {
            "    <item id=\"cover-page\" href=\"cover.xhtml\" media-type=\"application/xhtml+xml\"/>"
        }.orEmpty()
        val imageManifest = images.mapIndexedNotNull { index, image ->
            image.path?.let { path ->
                "    <item id=\"image-${index + 1}\" href=\"$path\" media-type=\"${escapeXml(image.mediaType ?: mediaTypeForPath(path))}\"/>"
            }
        }.joinToString("\n")
        val spine = chapters.joinToString("\n") { chapter ->
            "    <itemref idref=\"chapter-${chapter.index}\"/>"
        }
        val coverSpineItem = cover?.path?.let { "    <itemref idref=\"cover-page\"/>" }.orEmpty()
        return """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="book-id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="book-id">urn:novalpie:${escapeXml(metadata.title.hashCode().toString())}</dc:identifier>
    <dc:title>${escapeXml(metadata.title)}</dc:title>
    <dc:creator>${escapeXml(metadata.author)}</dc:creator>
    <dc:language>${escapeXml(metadata.language.ifBlank { "zh" })}</dc:language>
    <dc:publisher>${escapeXml(metadata.publisher)}</dc:publisher>
    <dc:description>${escapeXml(metadata.description)}</dc:description>
${cover?.path?.let { "    <meta name=\"cover\" content=\"cover-image\"/>" }.orEmpty()}
  </metadata>
  <manifest>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
    <item id="style" href="Styles/style.css" media-type="text/css"/>
$chapterManifest
$coverPageManifest
$coverManifest
$imageManifest
  </manifest>
  <spine>
$coverSpineItem
$spine
  </spine>
</package>"""
    }

    private fun coverPageXhtml(metadata: NativeEpubMetadata, cover: ImageRecord): String {
        val imagePath = cover.path ?: return ""
        return """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml">
<head><meta charset="UTF-8"/><title>封面</title><link rel="stylesheet" type="text/css" href="Styles/style.css"/></head>
<body class="cover-page">
<img class="cover-image" src="${escapeXml(imagePath)}" alt="封面"/>
<h1>${escapeXml(metadata.title)}</h1>
<p class="cover-author">${escapeXml(metadata.author)}</p>
</body></html>"""
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
.image-missing { color: #a33; }
.cover-page { margin: 0; padding: 1.2em; text-align: center; }
.cover-image { display: block; max-width: 100%; max-height: 78vh; height: auto; margin: 0 auto 1.5em; }
.cover-page h1 { margin: 0.5em 0 0.25em; }
.cover-author { text-indent: 0; color: #666; }"""

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
