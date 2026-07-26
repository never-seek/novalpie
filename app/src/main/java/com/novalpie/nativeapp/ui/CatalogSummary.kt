package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.Chapter

internal fun catalogSummaryLabel(
    allChapters: List<Chapter>,
    visibleChapters: List<Chapter>,
    currentChapterId: Long?
): String {
    if (allChapters.isEmpty()) return "目录未加载"

    val parts = mutableListOf("共 ${allChapters.size} 章")
    if (visibleChapters.size != allChapters.size) {
        parts += "已筛选 ${visibleChapters.size} 章"
    }

    val currentIndex = currentChapterId?.let { id -> allChapters.indexOfFirst { it.id == id } }
    if (currentIndex != null && currentIndex >= 0) {
        parts += "当前第 ${currentIndex + 1} 章"
    }

    return parts.joinToString(" · ")
}
