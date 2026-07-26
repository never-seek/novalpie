package com.novalpie.nativeapp.data

import android.content.Context

class SearchHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("novalpie_native_search_history", Context.MODE_PRIVATE)

    fun load(): List<String> =
        prefs.getString(KEY_KEYWORDS, null)
            ?.lines()
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?: emptyList()

    fun loadLastKeyword(): String =
        load().firstOrNull().orEmpty()

    fun saveKeyword(keyword: String) {
        val normalized = keyword.trim()
        if (normalized.isBlank()) return
        val next = (listOf(normalized) + load().filterNot { it == normalized }).take(MAX_HISTORY)
        prefs.edit().putString(KEY_KEYWORDS, next.joinToString("\n")).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_KEYWORDS = "keywords"
        private const val MAX_HISTORY = 10
    }
}
