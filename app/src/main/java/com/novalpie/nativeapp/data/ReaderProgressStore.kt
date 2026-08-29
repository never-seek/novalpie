package com.novalpie.nativeapp.data

import android.content.Context
import com.novalpie.nativeapp.model.FavoriteEntry
import com.novalpie.nativeapp.model.ReaderProgress

class ReaderProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("novalpie_native_reader_progress", Context.MODE_PRIVATE)

    fun load(): ReaderProgress? {
        val bookId = prefs.getLong(KEY_BOOK_ID, 0L)
        if (bookId <= 0L) return null
        return load(bookId) ?: loadLegacyProgress(bookId)
    }

    fun load(bookId: Long): ReaderProgress? {
        if (bookId <= 0L) return null
        val chapterId = prefs.getLong(bookKey(bookId, KEY_CHAPTER_ID), 0L)
        if (chapterId <= 0L) return loadLegacyProgress(bookId)
        return ReaderProgress(
            bookId = bookId,
            chapterId = chapterId,
            chapterTitle = prefs.getString(bookKey(bookId, KEY_CHAPTER_TITLE), null)?.takeIf { it.isNotBlank() },
            updatedAtMillis = prefs.getLong(bookKey(bookId, KEY_UPDATED_AT), 0L),
            bookTitle = prefs.getString(bookKey(bookId, KEY_BOOK_TITLE), null)?.takeIf { it.isNotBlank() },
            chapterNumber = prefs.getInt(bookKey(bookId, KEY_CHAPTER_NUMBER), 0).takeIf { it > 0 },
            chapterCountAtLastRead = prefs.getInt(bookKey(bookId, KEY_CHAPTER_COUNT_AT_LAST_READ), 0)
                .takeIf { it > 0 },
        )
    }

    fun loadRecent(limit: Int = DEFAULT_RECENT_LIMIT): List<ReaderProgress> {
        if (limit <= 0) return emptyList()
        return loadRecentBookIds()
            .mapNotNull { load(it) }
            .take(limit)
    }

    fun save(
        bookId: Long,
        chapterId: Long,
        chapterTitle: String?,
        bookTitle: String? = null,
        chapterNumber: Int? = null,
        chapterCountAtLastRead: Int? = null,
    ) {
        if (bookId <= 0L || chapterId <= 0L) return
        val normalizedTitle = chapterTitle?.trim()?.takeIf { it.isNotBlank() }
        val existing = load(bookId)
        // Callers that only know a chapter must not erase a title learned by an earlier book page.
        val normalizedBookTitle = bookTitle?.trim()?.takeIf { it.isNotBlank() } ?: existing?.bookTitle
        // A directory retry may temporarily lack its sequence number. Retain it only for the
        // same chapter; a different chapter with no known position must not reuse stale progress.
        val normalizedChapterNumber = chapterNumber?.takeIf { it > 0 }
            ?: existing?.takeIf { it.chapterId == chapterId }?.chapterNumber
        // A catalogue total belongs to one concrete read position. If a caller moves to another
        // chapter without a verified catalogue, do not carry an old completed-state baseline over
        // and accidentally label an ordinary paused chapter as an update.
        val normalizedChapterCount = chapterCountAtLastRead?.takeIf { it > 0 }
            ?: existing?.takeIf { it.chapterId == chapterId }?.chapterCountAtLastRead
        val updatedAt = System.currentTimeMillis()
        val recentBookIds = (listOf(bookId) + loadRecentBookIds().filterNot { it == bookId })
            .take(MAX_RECENT_BOOKS)
        val editor = prefs.edit()
            .putLong(KEY_BOOK_ID, bookId)
            .putLong(KEY_CHAPTER_ID, chapterId)
            .putString(KEY_CHAPTER_TITLE, normalizedTitle)
            .putString(KEY_BOOK_TITLE, normalizedBookTitle)
            .putLong(KEY_UPDATED_AT, updatedAt)
            .putLong(bookKey(bookId, KEY_CHAPTER_ID), chapterId)
            .putString(bookKey(bookId, KEY_CHAPTER_TITLE), normalizedTitle)
            .putString(bookKey(bookId, KEY_BOOK_TITLE), normalizedBookTitle)
            .putLong(bookKey(bookId, KEY_UPDATED_AT), updatedAt)
            .putString(KEY_RECENT_BOOK_IDS, recentBookIds.joinToString(","))
        if (normalizedChapterNumber == null) {
            editor.remove(KEY_CHAPTER_NUMBER)
            editor.remove(bookKey(bookId, KEY_CHAPTER_NUMBER))
        } else {
            editor.putInt(KEY_CHAPTER_NUMBER, normalizedChapterNumber)
            editor.putInt(bookKey(bookId, KEY_CHAPTER_NUMBER), normalizedChapterNumber)
        }
        if (normalizedChapterCount == null) {
            editor.remove(KEY_CHAPTER_COUNT_AT_LAST_READ)
            editor.remove(bookKey(bookId, KEY_CHAPTER_COUNT_AT_LAST_READ))
        } else {
            editor.putInt(KEY_CHAPTER_COUNT_AT_LAST_READ, normalizedChapterCount)
            editor.putInt(bookKey(bookId, KEY_CHAPTER_COUNT_AT_LAST_READ), normalizedChapterCount)
        }
        editor.apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    /** Remove one book's progress without destroying the rest of the recent-reading history. */
    fun clear(bookId: Long) {
        if (bookId <= 0L) return
        val remainingBookIds = loadRecentBookIds().filterNot { it == bookId }
        val replacement = remainingBookIds.firstOrNull()?.let(::load)
        val editor = prefs.edit()
            .remove(bookKey(bookId, KEY_CHAPTER_ID))
            .remove(bookKey(bookId, KEY_CHAPTER_NUMBER))
            .remove(bookKey(bookId, KEY_CHAPTER_COUNT_AT_LAST_READ))
            .remove(bookKey(bookId, KEY_CHAPTER_TITLE))
            .remove(bookKey(bookId, KEY_BOOK_TITLE))
            .remove(bookKey(bookId, KEY_UPDATED_AT))

        if (prefs.getLong(KEY_BOOK_ID, 0L) == bookId) {
            if (replacement == null) {
                editor
                    .remove(KEY_BOOK_ID)
                    .remove(KEY_CHAPTER_ID)
                    .remove(KEY_CHAPTER_NUMBER)
                    .remove(KEY_CHAPTER_COUNT_AT_LAST_READ)
                    .remove(KEY_CHAPTER_TITLE)
                    .remove(KEY_BOOK_TITLE)
                    .remove(KEY_UPDATED_AT)
            } else {
                editor
                    .putLong(KEY_BOOK_ID, replacement.bookId)
                    .putLong(KEY_CHAPTER_ID, replacement.chapterId)
                    .putLong(KEY_UPDATED_AT, replacement.updatedAtMillis)
                putNullableString(editor, KEY_CHAPTER_TITLE, replacement.chapterTitle)
                putNullableString(editor, KEY_BOOK_TITLE, replacement.bookTitle)
                if (replacement.chapterNumber == null) {
                    editor.remove(KEY_CHAPTER_NUMBER)
                } else {
                    editor.putInt(KEY_CHAPTER_NUMBER, replacement.chapterNumber)
                }
                if (replacement.chapterCountAtLastRead == null) {
                    editor.remove(KEY_CHAPTER_COUNT_AT_LAST_READ)
                } else {
                    editor.putInt(KEY_CHAPTER_COUNT_AT_LAST_READ, replacement.chapterCountAtLastRead)
                }
            }
        }

        if (remainingBookIds.isEmpty()) {
            editor.remove(KEY_RECENT_BOOK_IDS)
        } else {
            editor.putString(KEY_RECENT_BOOK_IDS, remainingBookIds.joinToString(","))
        }
        editor.apply()
    }

    /**
     * Adds the catalogue baseline missing from older local progress records, but only after two
     * independent reader facts say the person had completed the work: the local native-reader
     * position reached the current catalogue total and the local chapter id is valid.  The source
     * can omit `last_chapter` when reading happened entirely in the native app, and its value can
     * lag behind a native read (for example, a web progress record may still point to chapter 83
     * after the native reader reached chapter 84).  Therefore source progress is never used to
     * veto a locally completed position; it is only useful for the card's source-side display.
     * A local position below the current total still remains a paused book and is never migrated.
     * This lets a later source refresh distinguish a genuinely new chapter without inventing
     * progress for unread items.
     *
     * The migration intentionally changes only the baseline field.  It must not move an item to
     * the front of recent reading, change the last-read timestamp, or overwrite its chapter title.
     */
    fun backfillCompletedFavoriteCatalogues(entries: List<FavoriteEntry>): Int {
        if (entries.isEmpty()) return 0
        val editor = prefs.edit()
        var changed = 0
        entries.forEach { entry ->
            val bookId = entry.book.id
            val currentTotal = (entry.chapterCount ?: entry.book.chapterCount)?.takeIf { it > 0 }
            if (currentTotal == null) return@forEach
            val localProgress = load(bookId)
            if (localProgress == null) return@forEach
            val localChapterNumber = localProgress.chapterNumber?.takeIf { it > 0 }
            if (localChapterNumber == null) return@forEach
            val skipReason = when {
                localProgress.chapterCountAtLastRead != null -> "baseline_present"
                localProgress.chapterId <= 0L -> "no_local_chapter_id"
                localChapterNumber < currentTotal -> "local_earlier"
                else -> null
            }
            if (skipReason != null) return@forEach
            editor.putInt(bookKey(bookId, KEY_CHAPTER_COUNT_AT_LAST_READ), currentTotal)
            if (prefs.getLong(KEY_BOOK_ID, 0L) == bookId) {
                editor.putInt(KEY_CHAPTER_COUNT_AT_LAST_READ, currentTotal)
            }
            changed += 1
        }
        if (changed > 0) editor.apply()
        return changed
    }

    private fun loadLegacyProgress(bookId: Long): ReaderProgress? {
        val lastBookId = prefs.getLong(KEY_BOOK_ID, 0L)
        val chapterId = prefs.getLong(KEY_CHAPTER_ID, 0L)
        if (lastBookId != bookId || chapterId <= 0L) return null
        return ReaderProgress(
            bookId = bookId,
            chapterId = chapterId,
            chapterTitle = prefs.getString(KEY_CHAPTER_TITLE, null)?.takeIf { it.isNotBlank() },
            updatedAtMillis = prefs.getLong(KEY_UPDATED_AT, 0L),
            bookTitle = prefs.getString(KEY_BOOK_TITLE, null)?.takeIf { it.isNotBlank() },
            chapterNumber = prefs.getInt(KEY_CHAPTER_NUMBER, 0).takeIf { it > 0 },
            chapterCountAtLastRead = prefs.getInt(KEY_CHAPTER_COUNT_AT_LAST_READ, 0).takeIf { it > 0 },
        )
    }

    private fun loadRecentBookIds(): List<Long> =
        prefs.getString(KEY_RECENT_BOOK_IDS, null)
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull()?.takeIf { id -> id > 0L } }
            ?.distinct()
            ?: prefs.getLong(KEY_BOOK_ID, 0L).takeIf { it > 0L }?.let { listOf(it) }
            ?: emptyList()

    private fun bookKey(bookId: Long, suffix: String): String = "book_${bookId}_$suffix"

    private fun putNullableString(
        editor: android.content.SharedPreferences.Editor,
        key: String,
        value: String?,
    ) {
        if (value == null) editor.remove(key) else editor.putString(key, value)
    }

    companion object {
        private const val KEY_BOOK_ID = "book_id"
        private const val KEY_CHAPTER_ID = "chapter_id"
        private const val KEY_CHAPTER_NUMBER = "chapter_number"
        private const val KEY_CHAPTER_COUNT_AT_LAST_READ = "chapter_count_at_last_read"
        private const val KEY_CHAPTER_TITLE = "chapter_title"
        private const val KEY_BOOK_TITLE = "book_title"
        private const val KEY_UPDATED_AT = "updated_at"
        private const val KEY_RECENT_BOOK_IDS = "recent_book_ids"
        private const val DEFAULT_RECENT_LIMIT = 5
        private const val MAX_RECENT_BOOKS = 20
    }
}
