package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.Chapter

internal fun readerChapterProgressLabel(currentChapterId: Long, chapters: List<Chapter>): String {
    if (chapters.isEmpty()) return "当前章节 $currentChapterId · 目录未加载"

    val index = chapters.indexOfFirst { it.id == currentChapterId }
    if (index < 0) return "当前章节 $currentChapterId · 目录共 ${chapters.size} 章"

    val chapter = chapters[index]
    return "第 ${index + 1} / ${chapters.size} 章 · ${chapter.title}"
}
