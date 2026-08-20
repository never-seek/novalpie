package com.novalpie.nativeapp.data

import android.content.Context
import com.novalpie.nativeapp.model.Chapter
import com.novalpie.nativeapp.model.ChapterIllustration
import com.novalpie.nativeapp.model.ReaderChapterCacheState
import com.novalpie.nativeapp.model.ReaderContent
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CachedReaderChapter(
    val content: ReaderContent,
    val sourceUpdatedAt: String?,
    val cachedAtMillis: Long,
)

/**
 * Stores a bounded, variant-aware copy of chapter bodies for resilient reading.  A chapter can
 * be rendered from disk when the transport is unavailable, but the cache is only labelled current
 * after its recorded source revision matches the current directory response.
 */
class ReaderChapterCacheStore(context: Context) {
    private val directory = File(context.filesDir, DIRECTORY_NAME)

    fun load(
        bookId: Long,
        chapterId: Long,
        replaceMode: String,
        showImages: Boolean,
    ): CachedReaderChapter? {
        if (bookId <= 0L || chapterId <= 0L) return null
        val file = cacheFile(bookId, chapterId, replaceMode, showImages)
        return readEntry(file, expectedBookId = bookId, expectedChapterId = chapterId, replaceMode, showImages)
    }

    fun save(
        bookId: Long,
        chapterId: Long,
        replaceMode: String,
        showImages: Boolean,
        sourceUpdatedAt: String?,
        content: ReaderContent,
    ): Boolean {
        if (bookId <= 0L || chapterId <= 0L) return false
        if (!directory.exists() && !directory.mkdirs()) return false

        val target = cacheFile(bookId, chapterId, replaceMode, showImages)
        val temporary = File(directory, "${target.name}.tmp")
        val payload = JSONObject()
            .put(KEY_BOOK_ID, bookId)
            .put(KEY_CHAPTER_ID, chapterId)
            .put(KEY_REPLACE_MODE, normalizedReplaceMode(replaceMode))
            .put(KEY_SHOW_IMAGES, showImages)
            .put(KEY_SOURCE_UPDATED_AT, sourceUpdatedAt ?: JSONObject.NULL)
            .put(KEY_CACHED_AT, System.currentTimeMillis())
            .put(KEY_TITLE, content.title ?: JSONObject.NULL)
            .put(KEY_CONTENT, content.content)
            .put(KEY_SOURCE, content.source)
            .put(
                KEY_ILLUSTRATIONS,
                JSONArray().apply {
                    content.illustrations.forEach { image ->
                        put(
                            JSONObject()
                                .put(KEY_ILLUSTRATION_ID, image.id)
                                .put(KEY_ILLUSTRATION_INDEX, image.index)
                                .put(KEY_ILLUSTRATION_SOURCE, image.src),
                        )
                    }
                },
            )

        return runCatching {
            temporary.outputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
            }
            if (!temporary.renameTo(target)) {
                target.delete()
                check(temporary.renameTo(target)) { "Unable to replace reader cache" }
            }
            prune(bookId, replaceMode, showImages)
            true
        }.getOrElse {
            temporary.delete()
            false
        }
    }

    fun cacheStates(
        bookId: Long,
        replaceMode: String,
        showImages: Boolean,
        chapters: List<Chapter>,
    ): Map<Long, ReaderChapterCacheState> {
        if (bookId <= 0L || chapters.isEmpty()) return emptyMap()
        val cachedByChapter = cacheFiles(bookId, replaceMode, showImages)
            .mapNotNull { file -> readMetadata(file, bookId, replaceMode, showImages) }
            .associateBy(CacheMetadata::chapterId)

        return chapters.associate { chapter ->
            val metadata = cachedByChapter[chapter.id]
            chapter.id to when {
                metadata == null -> ReaderChapterCacheState.Missing
                revisionsMatch(metadata.sourceUpdatedAt, chapter.updatedAt) -> ReaderChapterCacheState.Current
                else -> ReaderChapterCacheState.Stale
            }
        }
    }

    /** Targeted per-book cleanup is useful for tests and never touches files outside this store. */
    fun clearBook(bookId: Long) {
        if (bookId <= 0L) return
        directory.listFiles()
            ?.filter { file -> file.name.startsWith("$bookId-") }
            ?.forEach(File::delete)
    }

    private fun cacheFile(
        bookId: Long,
        chapterId: Long,
        replaceMode: String,
        showImages: Boolean,
    ): File = File(directory, "$bookId-$chapterId-${variantKey(replaceMode, showImages)}.json")

    private fun cacheFiles(bookId: Long, replaceMode: String, showImages: Boolean): List<File> {
        val prefix = "$bookId-"
        val suffix = "-${variantKey(replaceMode, showImages)}.json"
        return directory.listFiles()
            ?.filter { file -> file.isFile && file.name.startsWith(prefix) && file.name.endsWith(suffix) }
            .orEmpty()
    }

    private fun prune(bookId: Long, replaceMode: String, showImages: Boolean) {
        cacheFiles(bookId, replaceMode, showImages)
            .sortedByDescending(File::lastModified)
            .drop(MAX_CHAPTERS_PER_VARIANT)
            .forEach(File::delete)
    }

    private fun readEntry(
        file: File,
        expectedBookId: Long,
        expectedChapterId: Long,
        replaceMode: String,
        showImages: Boolean,
    ): CachedReaderChapter? = runCatching {
        if (!file.isFile) return null
        val json = JSONObject(file.inputStream().bufferedReader(Charsets.UTF_8).use { it.readText() })
        if (
            json.optLong(KEY_BOOK_ID) != expectedBookId ||
            json.optLong(KEY_CHAPTER_ID) != expectedChapterId ||
            json.optString(KEY_REPLACE_MODE) != normalizedReplaceMode(replaceMode) ||
            json.optBoolean(KEY_SHOW_IMAGES, !showImages) != showImages
        ) return null

        val illustrations = json.optJSONArray(KEY_ILLUSTRATIONS).toIllustrations()
        CachedReaderChapter(
            content = ReaderContent(
                title = json.optionalString(KEY_TITLE),
                content = json.optString(KEY_CONTENT),
                source = json.optString(KEY_SOURCE),
                illustrations = illustrations,
            ),
            sourceUpdatedAt = json.optionalString(KEY_SOURCE_UPDATED_AT),
            cachedAtMillis = json.optLong(KEY_CACHED_AT),
        )
    }.getOrNull()

    private fun readMetadata(
        file: File,
        expectedBookId: Long,
        replaceMode: String,
        showImages: Boolean,
    ): CacheMetadata? = runCatching {
        if (!file.isFile) return null
        val json = JSONObject(file.inputStream().bufferedReader(Charsets.UTF_8).use { it.readText() })
        if (
            json.optLong(KEY_BOOK_ID) != expectedBookId ||
            json.optString(KEY_REPLACE_MODE) != normalizedReplaceMode(replaceMode) ||
            json.optBoolean(KEY_SHOW_IMAGES, !showImages) != showImages
        ) return null
        val chapterId = json.optLong(KEY_CHAPTER_ID)
        if (chapterId <= 0L) return null
        CacheMetadata(chapterId = chapterId, sourceUpdatedAt = json.optionalString(KEY_SOURCE_UPDATED_AT))
    }.getOrNull()

    private fun JSONArray?.toIllustrations(): List<ChapterIllustration> {
        val array = this ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val value = array.optJSONObject(index) ?: return@mapNotNull null
            val id = value.optLong(KEY_ILLUSTRATION_ID)
            val source = value.optionalString(KEY_ILLUSTRATION_SOURCE) ?: return@mapNotNull null
            if (id <= 0L) return@mapNotNull null
            ChapterIllustration(
                id = id,
                index = value.optInt(KEY_ILLUSTRATION_INDEX),
                src = source,
            )
        }
    }

    private fun JSONObject.optionalString(key: String): String? =
        if (isNull(key)) null else optString(key).trim().takeIf(String::isNotBlank)

    private fun revisionsMatch(cached: String?, source: String?): Boolean {
        val cachedValue = cached?.trim()?.takeIf { it.isNotBlank() }
        val sourceValue = source?.trim()?.takeIf { it.isNotBlank() }
        return cachedValue == sourceValue
    }

    private fun normalizedReplaceMode(value: String): String =
        value.trim().ifBlank { DEFAULT_REPLACE_VARIANT }.replace(Regex("[^A-Za-z0-9_-]"), "_")

    private fun variantKey(replaceMode: String, showImages: Boolean): String =
        "${normalizedReplaceMode(replaceMode)}-${if (showImages) "images" else "text"}"

    private data class CacheMetadata(
        val chapterId: Long,
        val sourceUpdatedAt: String?,
    )

    private companion object {
        const val DIRECTORY_NAME = "novalpie_reader_chapter_cache"
        const val MAX_CHAPTERS_PER_VARIANT = 80
        const val DEFAULT_REPLACE_VARIANT = "default"
        const val KEY_BOOK_ID = "book_id"
        const val KEY_CHAPTER_ID = "chapter_id"
        const val KEY_REPLACE_MODE = "replace_mode"
        const val KEY_SHOW_IMAGES = "show_images"
        const val KEY_SOURCE_UPDATED_AT = "source_updated_at"
        const val KEY_CACHED_AT = "cached_at"
        const val KEY_TITLE = "title"
        const val KEY_CONTENT = "content"
        const val KEY_SOURCE = "source"
        const val KEY_ILLUSTRATIONS = "illustrations"
        const val KEY_ILLUSTRATION_ID = "id"
        const val KEY_ILLUSTRATION_INDEX = "index"
        const val KEY_ILLUSTRATION_SOURCE = "src"
    }
}
