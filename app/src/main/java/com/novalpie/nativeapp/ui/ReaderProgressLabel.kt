package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.Chapter

internal fun readerChapterProgressLabel(currentChapterId: Long, chapters: List<Chapter>): String {
    if (chapters.isEmpty()) return "当前章节 $currentChapterId · 目录未加载"

    val index = chapters.indexOfFirst { it.id == currentChapterId }
    if (index < 0) return "当前章节 $currentChapterId · 目录共 ${chapters.size} 章"

    val chapter = chapters[index]
    val number = chapter.number?.takeIf { it > 0 } ?: (index + 1)
    return "${number}章-\"${chapter.title}\""
}

/** Source formula: (chapter index + within-chapter progress) / chapter count. */
internal fun readerBookProgressFraction(currentChapterId: Long, chapters: List<Chapter>, chapterFraction: Float = 0f): Float? {
    if (chapters.isEmpty()) return null
    val index = chapters.indexOfFirst { it.id == currentChapterId }
    if (index < 0) return null
    val within = chapterFraction.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    return ((index + within) / chapters.size.toFloat()).coerceIn(0f, 1f)
}

/** Semantic scroll progress within the visible chapter, excluding its title and comments. */
internal fun readerScrollProgress(index: Int, offsetPx: Int, itemHeightPx: Int, firstBodyIndex: Int, finishIndex: Int): Float {
    if (index >= finishIndex) return 1f
    if (index < firstBodyIndex) return 0f
    val count = (finishIndex - firstBodyIndex).coerceAtLeast(1)
    val within = if (itemHeightPx > 0) offsetPx.toFloat().coerceAtLeast(0f) / itemHeightPx else 0f
    return ((index - firstBodyIndex + within.coerceIn(0f, 1f)) / count).coerceIn(0f, 1f)
}
