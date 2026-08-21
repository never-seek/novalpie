package com.novalpie.nativeapp.data

import android.content.Context
import com.novalpie.nativeapp.model.ReaderSession

/**
 * Remembers only the active reader route. Progress belongs in [ReaderProgressStore]; this marker
 * exists so a background process reclaim can reopen the same chapter instead of dropping a
 * reader onto Collection.
 */
class ReaderSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ReaderSession? {
        val bookId = prefs.getLong(KEY_BOOK_ID, 0L)
        val chapterId = prefs.getLong(KEY_CHAPTER_ID, 0L)
        return if (bookId > 0L && chapterId > 0L) ReaderSession(bookId, chapterId) else null
    }

    fun save(bookId: Long, chapterId: Long) {
        if (bookId <= 0L || chapterId <= 0L) return
        prefs.edit()
            .putLong(KEY_BOOK_ID, bookId)
            .putLong(KEY_CHAPTER_ID, chapterId)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "novalpie_native_reader_session"
        const val KEY_BOOK_ID = "book_id"
        const val KEY_CHAPTER_ID = "chapter_id"
    }
}
