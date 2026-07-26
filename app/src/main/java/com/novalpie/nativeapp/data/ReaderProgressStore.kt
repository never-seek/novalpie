package com.novalpie.nativeapp.data

import android.content.Context
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
            updatedAtMillis = prefs.getLong(bookKey(bookId, KEY_UPDATED_AT), 0L)
        )
    }

    fun loadRecent(limit: Int = DEFAULT_RECENT_LIMIT): List<ReaderProgress> {
        if (limit <= 0) return emptyList()
        return loadRecentBookIds()
            .mapNotNull { load(it) }
            .take(limit)
    }

    fun save(bookId: Long, chapterId: Long, chapterTitle: String?) {
        if (bookId <= 0L || chapterId <= 0L) return
        val normalizedTitle = chapterTitle?.trim()?.takeIf { it.isNotBlank() }
        val updatedAt = System.currentTimeMillis()
        val recentBookIds = (listOf(bookId) + loadRecentBookIds().filterNot { it == bookId })
            .take(MAX_RECENT_BOOKS)
        prefs.edit()
            .putLong(KEY_BOOK_ID, bookId)
            .putLong(KEY_CHAPTER_ID, chapterId)
            .putString(KEY_CHAPTER_TITLE, normalizedTitle)
            .putLong(KEY_UPDATED_AT, updatedAt)
            .putLong(bookKey(bookId, KEY_CHAPTER_ID), chapterId)
            .putString(bookKey(bookId, KEY_CHAPTER_TITLE), normalizedTitle)
            .putLong(bookKey(bookId, KEY_UPDATED_AT), updatedAt)
            .putString(KEY_RECENT_BOOK_IDS, recentBookIds.joinToString(","))
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun loadLegacyProgress(bookId: Long): ReaderProgress? {
        val lastBookId = prefs.getLong(KEY_BOOK_ID, 0L)
        val chapterId = prefs.getLong(KEY_CHAPTER_ID, 0L)
        if (lastBookId != bookId || chapterId <= 0L) return null
        return ReaderProgress(
            bookId = bookId,
            chapterId = chapterId,
            chapterTitle = prefs.getString(KEY_CHAPTER_TITLE, null)?.takeIf { it.isNotBlank() },
            updatedAtMillis = prefs.getLong(KEY_UPDATED_AT, 0L)
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

    companion object {
        private const val KEY_BOOK_ID = "book_id"
        private const val KEY_CHAPTER_ID = "chapter_id"
        private const val KEY_CHAPTER_TITLE = "chapter_title"
        private const val KEY_UPDATED_AT = "updated_at"
        private const val KEY_RECENT_BOOK_IDS = "recent_book_ids"
        private const val DEFAULT_RECENT_LIMIT = 5
        private const val MAX_RECENT_BOOKS = 20
    }
}
