package com.novalpie.nativeapp.data

import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
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
    val fallbackCoverUrls: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val originalTitle: String? = null,
    val status: String? = null,
    val wordCount: Long? = null,
    val platform: String? = null,
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
    val coverFailed: Boolean = false,
    val currentChapterTitle: String? = null,
    val currentImageUrl: String? = null,
    val statusLog: String? = null,
)

/** Immutable transformed text for one source chapter; binary assets remain outside this contract. */
data class NativeDownloadChapterText(
    val title: String,
    val body: String,
    val originalBody: String? = null,
    val transformTextNode: ((String) -> String)? = null,
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
        if (file.isDirectory && nativeEpubWorkDirectoryNameRegex.matches(file.name)) {
            // EPUB jobs live in a per-run directory under filesDir. A force-stop can leave an
            // entire staged-image tree behind, so remove only directories with our generated
            // bookId-timestamp-nanotime name; unrelated app directories remain untouched.
            if (file.deleteRecursively()) deleted++
        } else if (file.isFile && (file.name.startsWith("novalpie-asset-") ||
                file.name.startsWith("novalpie-epub-")) && file.delete()
        ) {
            deleted++
        }
    }
    return deleted
}

private val nativeEpubWorkDirectoryNameRegex = Regex("""\d+-\d+-\d+""")

private fun detectNativeEpubMediaType(declared: String?, header: ByteArray): String? {
    val normalized = declared
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase(Locale.US)
        ?.takeIf(String::isNotBlank)

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
    if (Regex("^(?:<!doctype\\s+html\\b|<html\\b|<head\\b|<body\\b)", RegexOption.IGNORE_CASE).containsMatchIn(textHeader)) {
        throw java.io.IOException("图片服务返回了错误网页，未作为图片写入 EPUB")
    }
    if (textHeader.startsWith("<svg", ignoreCase = true) ||
        (textHeader.startsWith("<?xml", ignoreCase = true) &&
            textHeader.contains("<svg", ignoreCase = true))
    ) {
        return "image/svg+xml"
    }
    return normalized?.takeUnless { it == "application/octet-stream" || it == "binary/octet-stream" }
}

/** Bounded inspection of a task-owned original; bytes are never rewritten or decoded here. */
internal fun inspectNativeEpubFileType(file: File, declared: String?): String? {
    val header = ByteArray(512)
    var length = 0
    file.inputStream().use { input ->
        while (length < header.size) {
            val count = input.read(header, length, header.size - length)
            if (count < 0) break
            if (count > 0) length += count
        }
    }
    if (length == 0) throw java.io.IOException("图片内容为空")
    return detectNativeEpubMediaType(declared, header.copyOf(length))
}

/** Copies an asset once while calculating the exact metadata required by a STORE ZIP entry. */
internal suspend fun stageNativeEpubFile(
    input: InputStream,
    mediaType: String?,
    destination: File,
    compressImages: Boolean = false,
    imageQuality: Int = DEFAULT_DOWNLOAD_IMAGE_QUALITY,
    awaitIfPaused: suspend () -> Unit = {},
): NativeEpubStagedFile {
    val crc = CRC32()
    var size = 0L
    val header = ByteArray(512)
    var headerSize = 0
    var detectedType: String? = null
    val temporary = File.createTempFile("novalpie-stage-", ".part", destination.parentFile)
    input.use { source ->
        try {
            FileOutputStream(temporary).buffered().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    awaitIfPaused()
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
            detectedType = detectNativeEpubMediaType(mediaType, header.copyOf(headerSize))

            var finalFile = temporary
            var finalSize = size
            var finalCrc = crc.value

            if (compressImages && detectedType in setOf("image/jpeg", "image/png", "image/webp")) {
                val compressed = File.createTempFile("novalpie-compressed-", ".jpg", destination.parentFile)
                val result = compressStagedImageIfPossible(temporary, compressed, imageQuality)
                if (result != null) {
                    finalFile = compressed
                    finalSize = result.first
                    finalCrc = result.second
                    detectedType = "image/jpeg"
                    temporary.delete()
                } else {
                    compressed.delete()
                }
            }

            if (destination.exists() && !destination.delete()) {
                throw IllegalStateException("无法替换 EPUB 阶段文件")
            }
            if (!finalFile.renameTo(destination)) {
                throw IllegalStateException("无法发布 EPUB 阶段文件")
            }
            return NativeEpubStagedFile(
                file = destination,
                mediaType = detectedType,
                size = finalSize,
                crc = finalCrc,
            )
        } catch (failure: Throwable) {
            temporary.delete()
            throw failure
        }
    }
}

internal fun compressStagedImageIfPossible(
    sourceFile: File,
    targetFile: File,
    imageQuality: Int,
    maxWidth: Int = 1080,
    maxHeight: Int = 1920,
): Pair<Long, Long>? {
    return try {
        val boundsOptions = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(sourceFile.absolutePath, boundsOptions)
        val origW = boundsOptions.outWidth
        val origH = boundsOptions.outHeight
        if (origW <= 0 || origH <= 0) return null
        val scale = minOf(1.0, maxWidth.toDouble() / origW, maxHeight.toDouble() / origH)
        val targetW = maxOf(1, (origW * scale).toInt())
        val targetH = maxOf(1, (origH * scale).toInt())
        val sampleSize = maxOf(1, minOf(origW / targetW, origH / targetH))
        val decodeOptions = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = android.graphics.BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions) ?: return null
        val scaled = if (bitmap.width != targetW || bitmap.height != targetH) {
            val s = android.graphics.Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            if (s != bitmap) bitmap.recycle()
            s
        } else {
            bitmap
        }
        val quality = imageQuality.coerceIn(1, 95)
        val crc = CRC32()
        val bos = java.io.ByteArrayOutputStream()
        val compressedOk = scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, bos)
        scaled.recycle()
        if (!compressedOk) return null
        val bytes = bos.toByteArray()
        if (bytes.isEmpty()) return null
        crc.update(bytes)
        targetFile.outputStream().use { it.write(bytes) }
        Pair(bytes.size.toLong(), crc.value)
    } catch (_: Throwable) {
        null
    }
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
    private const val IMAGE_OPEN_MAX_ATTEMPTS = 5
    private fun calculateImageRetryDelay(attempt: Int): Long {
        val expDelay = 400L * (1L shl attempt.coerceIn(0, 4))
        val jitter = (Math.random() * 250).toLong()
        return minOf(6000L, expDelay + jitter)
    }
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
        transformChapter: (chapterOrder: Int, title: String, body: String) -> NativeDownloadChapterText = { _, title, body ->
            NativeDownloadChapterText(title = title, body = body)
        },
        imageConcurrency: Int = DEFAULT_IMAGE_CONCURRENCY,
        compressImages: Boolean = false,
        imageQuality: Int = DEFAULT_DOWNLOAD_IMAGE_QUALITY,
        zipCompressionLevel: Int = DEFAULT_DOWNLOAD_ZIP_COMPRESSION_LEVEL,
        stagingDirectory: File? = null,
        stagedAssetCacheMaxBytes: Long = DEFAULT_STAGED_ASSET_CACHE_MAX_BYTES,
        allowMissingCover: Boolean = false,
        awaitIfPaused: suspend () -> Unit = {},
        onProgress: (NativeEpubExportProgress) -> Unit = {},
        reconcileSourceImages: suspend (chapterOrder: Int, body: String) -> String = { _, body -> body },
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
        var coverFailed = false
        var nextImageNumber = 1

        try {
            ZipOutputStream(output.buffered()).use { zip ->
                if (zipCompressionLevel in 1..9) {
                    zip.setLevel(zipCompressionLevel)
                }
                awaitIfPaused()
                writeStoredText(zip, "mimetype", EPUB_MIMETYPE)
                writeText(zip, "META-INF/container.xml", containerXml())

                fun totalImageCount(): Int = images.size
                fun completedImageCount(): Int = completedImages
                fun failedImageCount(): Int = failedImages

                val coverCandidates = (listOfNotNull(metadata.coverUrl) + metadata.fallbackCoverUrls)
                    .map(::normalizeAssetUrl)
                    .filter(String::isNotBlank)
                    .distinct()

                val coverRecord = if (coverCandidates.isNotEmpty()) {
                    var chosenRecord: ImageRecord? = null
                    for (candidateUrl in coverCandidates) {
                        awaitIfPaused()
                        stagedAssets.trim(protectedKeys = setOf(candidateUrl))
                        onProgress(
                            NativeEpubExportProgress(
                                completedChapters = chapters.size,
                                totalImages = totalImageCount(),
                                completedImages = completedImageCount(),
                                failedImages = failedImageCount(),
                                coverFailed = coverFailed,
                                currentImageUrl = candidateUrl,
                                statusLog = "正在下载封面图片...",
                            ),
                        )
                        val staged = stageAsset(
                            stagedAssets = stagedAssets,
                            url = candidateUrl,
                            openAsset = openAsset,
                            protectedKeys = setOf(candidateUrl),
                            stagingDirectory = stagingDirectory,
                            compressImages = compressImages,
                            imageQuality = imageQuality,
                            awaitIfPaused = awaitIfPaused,
                            onRetry = { url, attempt, max ->
                                onProgress(
                                    NativeEpubExportProgress(
                                        completedChapters = chapters.size,
                                        totalImages = totalImageCount(),
                                        completedImages = completedImageCount(),
                                        failedImages = failedImageCount(),
                                        coverFailed = coverFailed,
                                        currentImageUrl = url,
                                        statusLog = "封面图片重试中...（$attempt/$max）",
                                    ),
                                )
                            },
                        )
                        val record = writeStagedImage(
                            zip = zip,
                            staged = staged,
                            path = "images/cover.${imageExtension(staged.mediaType, candidateUrl)}",
                            awaitIfPaused = awaitIfPaused,
                        )
                        if (record.path != null) {
                            onProgress(
                                NativeEpubExportProgress(
                                    completedChapters = chapters.size,
                                    totalImages = totalImageCount(),
                                    completedImages = completedImageCount(),
                                    failedImages = failedImageCount(),
                                    coverFailed = coverFailed,
                                    currentImageUrl = candidateUrl,
                                    statusLog = "封面图片下载完成",
                                ),
                            )
                            chosenRecord = record
                            break
                        }
                    }
                    if (chosenRecord == null) {
                        if (!allowMissingCover) {
                            throw java.io.IOException("封面获取失败，未生成缺图 EPUB；可从检查点重试")
                        }
                        coverFailed = true
                        onProgress(
                            NativeEpubExportProgress(
                                completedChapters = chapters.size,
                                totalImages = totalImageCount(),
                                completedImages = completedImageCount(),
                                failedImages = failedImageCount(),
                                coverFailed = coverFailed,
                                currentImageUrl = coverCandidates.first(),
                                statusLog = "封面图片下载失败（已重试全部候选封面）",
                            ),
                        )
                    }
                    stagedAssets.trim()
                    chosenRecord
                } else {
                    null
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
                writeText(zip, "OEBPS/intro.xhtml", introPageXhtml(metadata))

                suspend fun flushChapter(title: String, body: String) {
                    awaitIfPaused()
                    val chapterIndex = chapters.size + 1
                    val sourceTitle = title.ifBlank { "第${chapterIndex}章" }
                    val chapterOrder = chapterNumber(sourceTitle) ?: chapterIndex
                    val reconciledBody = reconcileSourceImages(chapterOrder, body)
                    val transformed = transformChapter(
                        chapterOrder,
                        sourceTitle,
                        reconciledBody,
                    )
                    val chapterTitle = transformed.title.ifBlank { sourceTitle }
                    val renderedBody = renderBody(
                        rawBody = transformed.originalBody ?: transformed.body,
                        transformTextNode = transformed.transformTextNode ?: { it },
                        zip = zip,
                        imageRecords = images,
                        stagedAssets = stagedAssets,
                        openAsset = openAsset,
                        imageConcurrency = effectiveImageConcurrency,
                        compressImages = compressImages,
                        imageQuality = imageQuality,
                        stagingDirectory = stagingDirectory,
                        awaitIfPaused = awaitIfPaused,
                        nextImageNumber = { nextImageNumber++ },
                        onImageResult = { url, succeeded, errorMsg ->
                            if (succeeded) completedImages++ else failedImages++
                            val currentTotal = totalImageCount()
                            val currentDone = completedImageCount() + failedImageCount()
                            val log = if (succeeded) "插图 $currentDone/$currentTotal 已完成（成功 ${completedImageCount()} / 失败 ${failedImageCount()}）"
                            else "插图 $currentDone/$currentTotal 失败：${errorMsg ?: "获取失败"}（成功 ${completedImageCount()} / 失败 ${failedImageCount()}）"
                            onProgress(
                                NativeEpubExportProgress(
                                    completedChapters = chapters.size,
                                    totalImages = currentTotal,
                                    completedImages = completedImageCount(),
                                    failedImages = failedImageCount(),
                                    coverFailed = coverFailed,
                                    currentChapterTitle = chapterTitle,
                                    currentImageUrl = url,
                                    statusLog = log,
                                )
                            )
                        },
                        onImageRetry = { url, attempt, max ->
                            val currentTotal = totalImageCount()
                            val currentDone = completedImageCount() + failedImageCount() + 1
                            onProgress(
                                NativeEpubExportProgress(
                                    completedChapters = chapters.size,
                                    totalImages = currentTotal,
                                    completedImages = completedImageCount(),
                                    failedImages = failedImageCount(),
                                    coverFailed = coverFailed,
                                    currentChapterTitle = chapterTitle,
                                    currentImageUrl = url,
                                    statusLog = "插图 $currentDone/$currentTotal 重试中...（$attempt/$max）",
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
                            coverFailed = coverFailed,
                            currentChapterTitle = chapterTitle,
                            statusLog = "第 ${chapters.size} 章《$chapterTitle》已完成",
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
                        awaitIfPaused()
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
                writeText(zip, "OEBPS/nav.xhtml", navigationXhtml(metadata.title, chapters, coverRecord?.path != null))
                writeText(zip, "OEBPS/toc.ncx", navigationNcx(metadata, chapters, coverRecord))
                writeText(zip, "OEBPS/content.opf", packageXml(metadata, chapters, images, coverRecord))
                onProgress(
                    NativeEpubExportProgress(
                        completedChapters = chapters.size,
                        totalChapters = chapters.size,
                        completedImages = completedImageCount(),
                        totalImages = totalImageCount(),
                        failedImages = failedImageCount(),
                        statusLog = "EPUB 生成完成！",
                    )
                )
                awaitIfPaused()
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

    /**
     * Streams a source TXT through the exact same chapter boundary rules as EPUB. The metadata
     * preamble is copied byte-for-character at line granularity, while only recognized chapter
     * titles/bodies reach [transformChapter]. This avoids materializing an entire paid download.
     */
    suspend fun writeTransformedTxt(
        output: Writer,
        source: Reader,
        transformChapter: suspend (chapterOrder: Int, title: String, body: String) -> NativeDownloadChapterText,
        awaitIfPaused: suspend () -> Unit = {},
    ) {
        val writer = output.buffered()
        BufferedReader(source).use { reader ->
            var currentTitle: String? = null
            val currentBody = StringBuilder()
            var lastChapterNumber: Int? = null
            var nextChapterIndex = 1
            var firstLine = true

            suspend fun flushCurrentChapter() {
                val sourceTitle = currentTitle ?: return
                val transformed = transformChapter(
                    chapterNumber(sourceTitle) ?: nextChapterIndex,
                    sourceTitle,
                    currentBody.toString(),
                )
                writer.write(transformed.title.ifBlank { sourceTitle })
                writer.write('\n'.code)
                val original = transformed.originalBody
                val textTransform = transformed.transformTextNode
                if (original != null && textTransform != null) {
                    var cursor = 0
                    imageMatches(original).forEach { image ->
                        writer.write(textTransform(original.substring(cursor, image.range.first)))
                        writer.write(original.substring(image.range.first, image.range.last + 1))
                        cursor = image.range.last + 1
                    }
                    writer.write(textTransform(original.substring(cursor)))
                } else writer.write(transformed.body)
                writer.write('\n'.code)
                nextChapterIndex += 1
            }

            while (true) {
                awaitIfPaused()
                val rawLine = reader.readLine() ?: break
                val line = if (firstLine) rawLine.removePrefix("\uFEFF") else rawLine
                firstLine = false
                val heading = chapterHeadingPattern.matchEntire(line.trim())?.groupValues?.getOrNull(1)
                if (heading != null && isChapterBoundary(heading, lastChapterNumber)) {
                    if (currentTitle != null) {
                        flushCurrentChapter()
                        currentBody.clear()
                    }
                    currentTitle = heading.trim()
                    lastChapterNumber = chapterNumber(currentTitle.orEmpty()) ?: lastChapterNumber
                } else if (currentTitle == null) {
                    // Preserve the source preamble as-is; it is not reader prose and should never
                    // be mistaken for a replacement target.
                    writer.write(line)
                    writer.write('\n'.code)
                } else {
                    if (currentBody.isNotEmpty()) currentBody.append('\n')
                    currentBody.append(line)
                }
            }
            flushCurrentChapter()
            writer.flush()
        }
    }

    private suspend fun renderBody(
        rawBody: String,
        transformTextNode: (String) -> String,
        zip: ZipOutputStream,
        imageRecords: MutableList<ImageRecord>,
        stagedAssets: NativeEpubStagedAssetCache,
        openAsset: suspend (String) -> NativeEpubAsset,
        imageConcurrency: Int,
        compressImages: Boolean = false,
        imageQuality: Int = DEFAULT_DOWNLOAD_IMAGE_QUALITY,
        stagingDirectory: File?,
        awaitIfPaused: suspend () -> Unit,
        nextImageNumber: () -> Int,
        onImageResult: (url: String, succeeded: Boolean, errorMsg: String?) -> Unit,
        onImageRetry: (url: String, attempt: Int, max: Int) -> Unit = { _, _, _ -> },
    ): String {
        val matches = imageMatches(rawBody)
        if (matches.isEmpty()) return paragraphs(renderStyledText(rawBody, transformTextNode))

        // Fetch assets concurrently, but keep ZIP writes below in source order. This mirrors the
        // website's six-worker image phase without allowing completion order to change chapter
        // references or image numbering.
        val stagedByUrl = stageAssetsConcurrently(
            urls = matches.map(ImageMatch::url),
            stagedAssets = stagedAssets,
            openAsset = openAsset,
            imageConcurrency = imageConcurrency,
            compressImages = compressImages,
            imageQuality = imageQuality,
            stagingDirectory = stagingDirectory,
            awaitIfPaused = awaitIfPaused,
            onRetry = onImageRetry,
        )

        val rendered = StringBuilder()
        var cursor = 0
        for (match in matches) {
            awaitIfPaused()
            rendered.append(renderStyledText(rawBody.substring(cursor, match.range.first), transformTextNode))
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
                    compressImages = compressImages,
                    imageQuality = imageQuality,
                    awaitIfPaused = awaitIfPaused,
                    onRetry = onImageRetry,
                )
            val record = writeStagedImage(
                zip = zip,
                staged = staged,
                path = "images/image-${nextImageNumber()}.${imageExtension(staged.mediaType, url)}",
                awaitIfPaused = awaitIfPaused,
            )
            imageRecords += record
            val index = imageRecords.lastIndex
            onImageResult(url, record.path != null, record.error)
            val token = "$IMAGE_TOKEN_PREFIX${index}$IMAGE_TOKEN_SUFFIX"
            rendered.append(token)
            cursor = match.range.last + 1
        }
        rendered.append(renderStyledText(rawBody.substring(cursor), transformTextNode))

        val tokenPattern = Regex("${Regex.escape(IMAGE_TOKEN_PREFIX)}(\\d+)${Regex.escape(IMAGE_TOKEN_SUFFIX)}")
        val withImages = tokenPattern.replace(rendered.toString()) { result ->
            val index = result.groupValues[1].toIntOrNull() ?: return@replace ""
            val record = imageRecords.getOrNull(index)
            if (record?.path != null) {
                "<img class=\"chapter-image\" src=\"${escapeXml(record.path)}\" alt=\"插图\"/>"
            } else {
                val error = record?.error?.takeIf(String::isNotBlank) ?: "插图获取失败"
                "<p class=\"image-missing\">[${escapeXml(error)}]</p>"
            }
        }
        return paragraphs(withImages)
    }

    /** Parse authored emphasis before replacement; replacement output can only become XML text. */
    private fun renderStyledText(source: String, transform: (String) -> String): String {
        val formatted = com.novalpie.nativeapp.ui.applyMarkdownRanges(com.novalpie.nativeapp.ui.ReaderFormattedParagraph(source))
        val boundaries = (listOf(0, formatted.text.length) + formatted.spanStyles.flatMap { listOf(it.start, it.end) }).distinct().sorted()
        return buildString {
            boundaries.zipWithNext().forEach { (start, end) ->
                val styles = formatted.spanStyles.filter { it.start <= start && it.end >= end }.map { it.item }
                val bold = styles.any { (it.fontWeight?.weight ?: 0) >= 600 }
                val italic = styles.any { it.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic }
                val underline = styles.any { it.textDecoration?.contains(androidx.compose.ui.text.style.TextDecoration.Underline) == true }
                val strike = styles.any { it.textDecoration?.contains(androidx.compose.ui.text.style.TextDecoration.LineThrough) == true }
                val text = escapeXml(transform(formatted.text.substring(start, end))).replace("\r", "")
                text.split(Regex("\n\\s*\n")).forEachIndexed { index, part ->
                    if (index > 0) append("\n\n")
                    if (part.isNotEmpty()) {
                        if (bold) append("<strong>")
                        if (italic) append("<em>")
                        if (underline) append("<u>")
                        if (strike) append("<del>")
                        append(part)
                        if (strike) append("</del>")
                        if (underline) append("</u>")
                        if (italic) append("</em>")
                        if (bold) append("</strong>")
                    }
                }
            }
        }
    }

    private fun isStagedAssetIntact(staged: StagedAsset): Boolean =
        staged.file?.let { file -> file.isFile && file.length() == staged.size } == true

    private suspend fun stageAssetsConcurrently(
        urls: List<String>,
        stagedAssets: NativeEpubStagedAssetCache,
        openAsset: suspend (String) -> NativeEpubAsset,
        imageConcurrency: Int,
        compressImages: Boolean = false,
        imageQuality: Int = DEFAULT_DOWNLOAD_IMAGE_QUALITY,
        stagingDirectory: File?,
        awaitIfPaused: suspend () -> Unit,
        onRetry: ((url: String, attempt: Int, max: Int) -> Unit)? = null,
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
                        awaitIfPaused()
                        val url = uniqueUrls[index]
                        results[index] = stageAsset(
                            stagedAssets = stagedAssets,
                            url = url,
                            openAsset = openAsset,
                            protectedKeys = protectedKeys,
                            stagingDirectory = stagingDirectory,
                            compressImages = compressImages,
                            imageQuality = imageQuality,
                            awaitIfPaused = awaitIfPaused,
                            onRetry = onRetry,
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
        compressImages: Boolean = false,
        imageQuality: Int = DEFAULT_DOWNLOAD_IMAGE_QUALITY,
        awaitIfPaused: suspend () -> Unit,
        onRetry: ((url: String, attempt: Int, max: Int) -> Unit)? = null,
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
        awaitIfPaused()
        val staged = stageAssetUncached(url, openAsset, stagingDirectory, compressImages, imageQuality, awaitIfPaused, onRetry)
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
        compressImages: Boolean = false,
        imageQuality: Int = DEFAULT_DOWNLOAD_IMAGE_QUALITY,
        awaitIfPaused: suspend () -> Unit,
        onRetry: ((url: String, attempt: Int, max: Int) -> Unit)? = null,
    ): StagedAsset {
        var lastFailure: Throwable? = null
        repeat(IMAGE_OPEN_MAX_ATTEMPTS) { attempt ->
            awaitIfPaused()
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
                    compressImages = compressImages,
                    imageQuality = imageQuality,
                    awaitIfPaused = awaitIfPaused,
                )
                awaitIfPaused()
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
                    onRetry?.invoke(url, attempt + 1, IMAGE_OPEN_MAX_ATTEMPTS)
                    awaitIfPaused()
                    delay(calculateImageRetryDelay(attempt))
                }
            }
        }
        return StagedAsset(
            file = null,
            mediaType = null,
            error = lastFailure?.message?.takeIf { it.isNotBlank() } ?: "插图获取失败",
        )
    }

    private suspend fun writeStagedImage(
        zip: ZipOutputStream,
        staged: StagedAsset,
        path: String,
        awaitIfPaused: suspend () -> Unit,
    ): ImageRecord {
        if (staged.file == null) {
            return ImageRecord(path = null, mediaType = null, error = staged.error)
        }
        awaitIfPaused()
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
                    awaitIfPaused()
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
        // Deflate changes the container size, not the decoded text. Original image entries and
        // the required first mimetype entry remain STORED; no image is decoded/re-encoded here.
        zip.putNextEntry(ZipEntry(path).apply { method = ZipEntry.DEFLATED })
        try {
            zip.write(text.toByteArray(Charsets.UTF_8))
        } finally {
            zip.closeEntry()
        }
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
            "    <item id=\"cover-image\" href=\"$path\" media-type=\"${escapeXml(cover.mediaType ?: mediaTypeForPath(path))}\"/>"
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
        val coverGuide = cover?.path?.let {
            "  <guide><reference type=\"cover\" title=\"封面\" href=\"cover.xhtml\"/><reference type=\"text\" title=\"作品简介\" href=\"intro.xhtml\"/></guide>"
        } ?: "  <guide><reference type=\"text\" title=\"作品简介\" href=\"intro.xhtml\"/></guide>"
        val dcSubjects = metadata.tags
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("\n") { "    <dc:subject>${escapeXml(it)}</dc:subject>" }
            .let { if (it.isNotBlank()) "\n$it" else "" }
        val dcSource = metadata.platform?.trim()?.takeIf { it.isNotBlank() }?.let {
            val label = when {
                it.equals("novelPia", ignoreCase = true) -> "NovelPia"
                it.equals("upload", ignoreCase = true) -> "上传"
                else -> it
            }
            "\n    <dc:source>${escapeXml(label)}</dc:source>"
        }.orEmpty()

        return """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="book-id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
    <dc:identifier id="book-id">${escapeXml(epubIdentifier(metadata))}</dc:identifier>
    <dc:title>${escapeXml(metadata.title)}</dc:title>
    <dc:creator>${escapeXml(metadata.author)}</dc:creator>
    <dc:language>${escapeXml(metadata.language.ifBlank { "zh" })}</dc:language>
    <dc:publisher>${escapeXml(metadata.publisher)}</dc:publisher>
    <dc:description>${escapeXml(metadata.description)}</dc:description>$dcSubjects$dcSource
${cover?.path?.let { "    <meta name=\"cover\" content=\"cover-image\"/>" }.orEmpty()}
  </metadata>
  <manifest>
    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml"/>
    <item id="style" href="Styles/style.css" media-type="text/css"/>
    <item id="intro-page" href="intro.xhtml" media-type="application/xhtml+xml"/>
$chapterManifest
$coverPageManifest
$coverManifest
$imageManifest
  </manifest>
  <spine toc="ncx">
$coverSpineItem
    <itemref idref="intro-page"/>
$spine
  </spine>
$coverGuide
</package>"""
    }

    /** EPUB 2 navigation keeps legacy Android readers from discarding a valid EPUB 3-only nav. */
    private fun navigationNcx(
        metadata: NativeEpubMetadata,
        chapters: List<ChapterRecord>,
        cover: ImageRecord?,
    ): String {
        val entries = buildList {
            if (cover?.path != null) add("封面" to "cover.xhtml")
            add("作品简介" to "intro.xhtml")
            chapters.forEach { chapter -> add(chapter.title to "chapter-${chapter.index}.xhtml") }
        }
        val navPoints = entries.mapIndexed { index, (label, href) ->
            """    <navPoint id="nav-${index + 1}" playOrder="${index + 1}">
      <navLabel><text>${escapeXml(label)}</text></navLabel>
      <content src="$href"/>
    </navPoint>"""
        }.joinToString("\n")
        return """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE ncx PUBLIC "-//NISO//DTD ncx 2005-1//EN" "http://www.daisy.org/z3986/2005/ncx-2005-1.dtd">
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
  <head>
    <meta name="dtb:uid" content="${escapeXml(epubIdentifier(metadata))}"/>
    <meta name="dtb:depth" content="1"/>
    <meta name="dtb:totalPageCount" content="0"/>
    <meta name="dtb:maxPageNumber" content="0"/>
  </head>
  <docTitle><text>${escapeXml(metadata.title)}</text></docTitle>
  <navMap>
$navPoints
  </navMap>
</ncx>"""
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

    private fun introPageXhtml(metadata: NativeEpubMetadata): String {
        val fields = buildList {
            metadata.originalTitle?.trim()?.takeIf { it.isNotBlank() && it != metadata.title.trim() }?.let {
                add("<p class=\"intro-field\"><span class=\"intro-label\">原名：</span><span class=\"intro-value\">${escapeXml(it)}</span></p>")
            }
            metadata.author.trim().takeIf { it.isNotBlank() }?.let {
                add("<p class=\"intro-field\"><span class=\"intro-label\">作者：</span><span class=\"intro-value\">${escapeXml(it)}</span></p>")
            }
            metadata.platform?.trim()?.takeIf { it.isNotBlank() }?.let {
                val label = when {
                    it.equals("novelPia", ignoreCase = true) -> "NovelPia"
                    it.equals("upload", ignoreCase = true) -> "上传"
                    else -> it
                }
                add("<p class=\"intro-field\"><span class=\"intro-label\">来源：</span><span class=\"intro-value\">${escapeXml(label)}</span></p>")
            }
            metadata.status?.trim()?.takeIf { it.isNotBlank() }?.let {
                add("<p class=\"intro-field\"><span class=\"intro-label\">状态：</span><span class=\"intro-value\">${escapeXml(it)}</span></p>")
            }
            metadata.wordCount?.takeIf { it > 0 }?.let { count ->
                val formatted = if (count >= 10_000) {
                    val w = count / 10000.0
                    val rounded = "%.1f".format(Locale.US, w).removeSuffix(".0")
                    "$rounded 万字"
                } else {
                    "$count 字"
                }
                add("<p class=\"intro-field\"><span class=\"intro-label\">字数：</span><span class=\"intro-value\">${escapeXml(formatted)}</span></p>")
            }
            val cleanTags = metadata.tags.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            if (cleanTags.isNotEmpty()) {
                val tagChips = cleanTags.joinToString(" ") { tag ->
                    "<span class=\"intro-tag\">${escapeXml(tag)}</span>"
                }
                add("<p class=\"intro-field intro-tags-field\"><span class=\"intro-label\">标签：</span><span class=\"intro-tags\">$tagChips</span></p>")
            }
        }

        val metaHtml = if (fields.isNotEmpty()) {
            """<div class="intro-meta">
${fields.joinToString("\n")}
</div>"""
        } else {
            ""
        }

        val descriptionContent = metadata.description.trim()
        val descHtml = if (descriptionContent.isNotBlank()) {
            paragraphs(descriptionContent)
        } else {
            "<p class=\"intro-empty\">暂无简介</p>"
        }

        return """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml">
<head><meta charset="UTF-8"/><title>书籍信息</title><link rel="stylesheet" type="text/css" href="Styles/style.css"/></head>
<body class="intro-page">
<h1 class="intro-title">${escapeXml(metadata.title)}</h1>
$metaHtml
<hr class="intro-divider"/>
<div class="intro-description">
<h2 class="intro-desc-heading">作品简介</h2>
$descHtml
</div>
</body></html>"""
    }

    private fun navigationXhtml(
        title: String,
        chapters: List<ChapterRecord>,
        hasCover: Boolean,
    ): String {
        val entries = buildList {
            if (hasCover) add("封面" to "cover.xhtml")
            add("作品简介" to "intro.xhtml")
            chapters.forEach { chapter -> add(chapter.title to "chapter-${chapter.index}.xhtml") }
        }
        val links = entries.joinToString("\n") { (label, href) ->
            "      <li><a href=\"$href\">${escapeXml(label)}</a></li>"
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
.cover-author { text-indent: 0; color: #666; }
.intro-page { margin: 0; padding: 1.2em; }
.intro-title { text-align: center; font-size: 1.4em; margin-bottom: 0.8em; font-weight: bold; }
.intro-meta { margin: 1em 0; line-height: 1.6; }
.intro-field { text-indent: 0; margin: 0.4em 0; }
.intro-label { font-weight: bold; color: #555; }
.intro-value { color: #222; }
.intro-tags-field { text-indent: 0; }
.intro-tags { display: inline; }
.intro-tag { display: inline-block; padding: 0.1em 0.5em; margin: 0.15em 0.25em 0.15em 0; border: 1px solid #ccc; border-radius: 4px; font-size: 0.85em; color: #444; background-color: #f7f7f7; text-indent: 0; }
.intro-divider { border: none; border-top: 1px solid #ddd; margin: 1.5em 0 1.2em; }
.intro-description { margin-top: 1em; }
.intro-desc-heading { font-size: 1.15em; font-weight: bold; margin: 0 0 0.8em; text-align: left; }
.intro-description p { text-indent: 2em; margin: 0 0 0.8em; }
.intro-empty { color: #888; font-style: italic; }"""

    private fun mediaTypeForPath(path: String): String = when (path.substringAfterLast('.', "").lowercase(Locale.US)) {
        "webp" -> "image/webp"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "avif" -> "image/avif"
        "svg" -> "image/svg+xml"
        "bmp" -> "image/bmp"
        else -> "image/jpeg"
    }

    private fun epubIdentifier(metadata: NativeEpubMetadata): String =
        "urn:novalpie:${metadata.title.hashCode()}"

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
