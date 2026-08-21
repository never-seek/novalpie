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

/** The source status rail shows full-book progress; the first chapter intentionally starts at 0%. */
internal fun readerBookProgressFraction(currentChapterId: Long, chapters: List<Chapter>): Float {
    if (chapters.isEmpty()) return 0f
    val index = chapters.indexOfFirst { it.id == currentChapterId }
    if (index < 0) return 0f
    return (index.toFloat() / chapters.size.toFloat()).coerceIn(0f, 1f)
}
