package com.novalpie.nativeapp.data

import android.content.Context
import com.novalpie.nativeapp.model.Chapter
import com.novalpie.nativeapp.model.ChapterIllustration
import com.novalpie.nativeapp.model.ReaderChapterCacheState
import com.novalpie.nativeapp.model.ReaderContent
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.io.IOException
import kotlinx.coroutines.CancellationException

/** Token expiry affects remote authority, not the identity of already-owned offline data. */
internal fun readerCacheAccountScope(token: String?): String {
    val value = token?.trim()?.takeIf(String::isNotEmpty) ?: return "guest"
    decodeAuthTokenProfile(value, nowEpochSeconds = 0)?.id?.takeIf { it > 0 }?.let { return "user-$it" }
    // Opaque sessions must never share the anonymous namespace. No credentials are persisted here.
    return "session-" + MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }
}

internal fun readerCacheMayServeFailure(error: Throwable?): Boolean = when (error) {
    is CancellationException -> false
    is NovalPieApiException -> error.statusCode in listOf(408, 429) || error.statusCode >= 500
    is IOException -> true
    else -> false
}

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
class ReaderChapterCacheStore(
    context: Context,
    private val accountScopeProvider: () -> String = {
        readerCacheAccountScope(AuthSessionStore(context.applicationContext).loadToken())
    },
) {
    private val application = context.applicationContext
    private val root = File(application.filesDir, DIRECTORY_NAME)
    private val ownership = application.getSharedPreferences("novalpie_reader_cache_ownership", Context.MODE_PRIVATE)
    init {
        synchronized(CACHE_LOCK) {
            if (!ownership.contains(KEY_LEGACY_OWNER)) {
                // Match the already-pinned Beta7 rule migration owner where available. A guest
                // upgrade cannot silently assign retained authenticated files to a later login.
                val prior = application.getSharedPreferences(ReaderReplacementRulesStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
                val scope = if (prior.contains("beta7_legacy_owner_id")) {
                    prior.getLong("beta7_legacy_owner_id", -1).takeIf { it > 0 }?.let { "user-$it" }
                } else accountScopeProvider().takeUnless { it == "guest" }
                ownership.edit().putString(KEY_LEGACY_OWNER, scope ?: "unassigned").commit()
            }
        }
    }

    /** Capture before suspension so an A response can never be saved into B's namespace. */
    fun forCurrentAccount(): ReaderChapterCacheStore {
        val scope = accountScopeProvider()
        return ReaderChapterCacheStore(application) { scope }
    }

    private fun directory(scope: String): File {
        require(scope.matches(Regex("[A-Za-z0-9_-]+")))
        return File(root, "accounts/$scope")
    }
    private fun legacyAllowed(scope: String) = scope != "guest" && ownership.getString(KEY_LEGACY_OWNER, null) == scope

    fun load(
        bookId: Long,
        chapterId: Long,
        replaceMode: String,
        showImages: Boolean,
    ): CachedReaderChapter? = synchronized(CACHE_LOCK) {
        if (bookId <= 0L || chapterId <= 0L) return null
        val scope = accountScopeProvider()
        val file = cacheFile(directory(scope), bookId, chapterId, replaceMode, showImages)
        if (denied(directory(scope), file.name)) return null
        readEntry(file, bookId, chapterId, replaceMode, showImages, scope) ?: if (legacyAllowed(scope)) {
            readEntry(cacheFile(root, bookId, chapterId, replaceMode, showImages), bookId, chapterId, replaceMode, showImages, null)
        } else null
    }

    /** I/O errors must not prevent the reader from publishing an explicit permission failure. */
    fun revoke(bookId: Long, chapterId: Long, replaceMode: String, showImages: Boolean): Boolean = synchronized(CACHE_LOCK) {
        if (bookId <= 0 || chapterId <= 0) return false
        val scope = accountScopeProvider()
        val folder = directory(scope)
        val file = cacheFile(folder, bookId, chapterId, replaceMode, showImages)
        memoryDenials.add(file.absolutePath)
        val marker = File(folder, "${file.name}.denied")
        val markerSaved = runCatching { atomicWrite(marker, "denied"); true }.getOrDefault(false)
        // A malformed marker is still a persistent tombstone, not permission to read the body.
        if (markerSaved || marker.exists() || File(marker.path + ".bak").exists()) return true
        // Separate storage protects an entry when its own cache directory is unwritable.
        if (runCatching { ownership.edit().putBoolean(denialKey(scope, file.name), true).commit() }.getOrDefault(false)) return true
        // Last resort: rename, never delete, this entry's recovery bytes. No unrelated account or
        // chapter is touched. If every persistence route fails, in-process reads still fail closed.
        val candidates = listOf(file, File(file.path + ".bak")) + if (legacyAllowed(scope)) {
            val legacy = cacheFile(root, bookId, chapterId, replaceMode, showImages)
            listOf(legacy, File(legacy.path + ".bak"))
        } else emptyList()
        val existing = candidates.filter(File::isFile)
        existing.isNotEmpty() && existing.map { entry ->
            runCatching { entry.renameTo(File(entry.path + ".revoked-" + java.util.UUID.randomUUID())) }.getOrDefault(false)
        }.all { it }
    }

    fun save(
        bookId: Long,
        chapterId: Long,
        replaceMode: String,
        showImages: Boolean,
        sourceUpdatedAt: String?,
        content: ReaderContent,
    ): Boolean = synchronized(CACHE_LOCK) {
        if (bookId <= 0L || chapterId <= 0L) return false
        val scope = accountScopeProvider()
        val directory = directory(scope)
        if (!directory.exists() && !directory.mkdirs()) return false

        val target = cacheFile(directory, bookId, chapterId, replaceMode, showImages)
        val payload = JSONObject()
            .put(KEY_ACCOUNT_SCOPE, scope)
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
                                .put(KEY_ILLUSTRATION_SOURCE, image.src)
                                .put(KEY_ILLUSTRATION_ORIGINAL, image.originalSrc ?: JSONObject.NULL),
                        )
                    }
                },
            )

        return runCatching {
            atomicWrite(target, payload.toString())
            val marker = File(directory, "${target.name}.denied")
            val backupMarker = File(marker.path + ".bak")
            if (marker.exists() && !marker.delete()) return@runCatching false
            if (backupMarker.exists() && !backupMarker.delete()) return@runCatching false
            val key = denialKey(scope, target.name)
            if (ownership.contains(key) && !ownership.edit().remove(key).commit()) return@runCatching false
            memoryDenials.remove(target.absolutePath)
            prune(directory, bookId, replaceMode, showImages)
            true
        }.getOrElse {
            false
        }
    }

    private fun atomicWrite(target: File, text: String) {
        val atomic = OwnedAtomicFile(target)
        val stream = atomic.startWrite()
        try {
            stream.write(text.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (error: Throwable) {
            atomic.failWrite(stream)
            throw error
        }
    }

    fun cacheStates(
        bookId: Long,
        replaceMode: String,
        showImages: Boolean,
        chapters: List<Chapter>,
    ): Map<Long, ReaderChapterCacheState> = synchronized(CACHE_LOCK) {
        if (bookId <= 0L || chapters.isEmpty()) return emptyMap()
        val scope = accountScopeProvider()
        val directory = directory(scope)
        val files = (if (legacyAllowed(scope)) cacheFiles(root, bookId, replaceMode, showImages) else emptyList()) +
            cacheFiles(directory, bookId, replaceMode, showImages)
        val cachedByChapter = files
            .filterNot { denied(directory, it.name) }
            .mapNotNull { file -> readMetadata(file, bookId, replaceMode, showImages, scope.takeIf { file.parentFile != root }) }
            .associateBy(CacheMetadata::chapterId)

        chapters.associate { chapter ->
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
        synchronized(CACHE_LOCK) {
            val scope = accountScopeProvider()
            val directories = listOf(directory(scope)) + if (legacyAllowed(scope)) listOf(root) else emptyList()
            directories.forEach { directory -> directory.listFiles()
                ?.filter { file -> file.isFile && file.name.startsWith("$bookId-") }
                ?.forEach(File::delete) }
            val prefix = denialKey(scope, "$bookId-")
            val keys = ownership.all.keys.filter { it.startsWith(prefix) }
            if (keys.isNotEmpty()) ownership.edit().also { edit -> keys.forEach(edit::remove) }.commit()
            val scopedPrefix = File(directory(scope), "$bookId-").absolutePath
            memoryDenials.removeAll { it.startsWith(scopedPrefix) }
        }
    }

    private fun cacheFile(
        directory: File,
        bookId: Long,
        chapterId: Long,
        replaceMode: String,
        showImages: Boolean,
    ): File = File(directory, "$bookId-$chapterId-${variantKey(replaceMode, showImages)}.json")

    private fun cacheFiles(directory: File, bookId: Long, replaceMode: String, showImages: Boolean): List<File> {
        val prefix = "$bookId-"
        val suffix = "-${variantKey(replaceMode, showImages)}.json"
        return directory.listFiles()
            ?.map { if (it.name.endsWith("$suffix.bak")) File(directory, it.name.removeSuffix(".bak")) else it }
            ?.filter { file -> (file.isFile || File(file.path + ".bak").isFile) && file.name.startsWith(prefix) && file.name.endsWith(suffix) }
            ?.distinctBy(File::getName)
            .orEmpty()
    }

    private fun prune(directory: File, bookId: Long, replaceMode: String, showImages: Boolean) {
        cacheFiles(directory, bookId, replaceMode, showImages)
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
        accountScope: String?,
    ): CachedReaderChapter? = runCatching {
        if (!file.isFile && !File(file.path + ".bak").isFile) return null
        val json = JSONObject(OwnedAtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
        if (accountScope != null && json.optString(KEY_ACCOUNT_SCOPE) != accountScope) return null
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
        accountScope: String?,
    ): CacheMetadata? = runCatching {
        if (!file.isFile && !File(file.path + ".bak").isFile) return null
        val json = JSONObject(OwnedAtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
        if (accountScope != null && json.optString(KEY_ACCOUNT_SCOPE) != accountScope) return null
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
                originalSrc = value.optionalString(KEY_ILLUSTRATION_ORIGINAL),
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

    private fun denialKey(scope: String, name: String) = "revoked:$scope:$name"

    private fun denied(directory: File, name: String): Boolean =
        File(directory, name).absolutePath in memoryDenials ||
            runCatching {ownership.getBoolean(denialKey(directory.name, name), false)}.getOrDefault(true) ||
            File(directory, "$name.denied").exists() || File(directory, "$name.denied.bak").exists()

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
        const val KEY_ACCOUNT_SCOPE = "account_scope"
        const val KEY_LEGACY_OWNER = "legacy_owner_scope"
        val CACHE_LOCK = Any()
        val memoryDenials = mutableSetOf<String>()
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
        const val KEY_ILLUSTRATION_ORIGINAL = "original_src"
    }
}
