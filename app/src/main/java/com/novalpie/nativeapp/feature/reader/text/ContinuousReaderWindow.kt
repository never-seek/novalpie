package com.novalpie.nativeapp.feature.reader.text

import com.novalpie.nativeapp.model.ReaderChapterContent
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.ui.ReaderChapterCommentState

internal const val READER_CONTINUOUS_WINDOW_SIZE = 8

/** Keep a contiguous bounded window, including the visible chapter even after a mid-request scroll. */
internal fun boundedReaderChapterWindow(contents: List<ReaderChapterContent>, visibleChapterId: Long,
    direction: Int = 1, limit: Int = READER_CONTINUOUS_WINDOW_SIZE): List<ReaderChapterContent> {
    val chapters = contents.distinctBy { it.chapterId }
    val count = limit.coerceAtLeast(2)
    if (chapters.size <= count) return chapters
    val visible = chapters.indexOfFirst { it.chapterId == visibleChapterId }.takeIf { it >= 0 }
        ?: if (direction < 0) 0 else chapters.lastIndex
    val start = if (direction < 0) (visible - count + 1).coerceAtLeast(0)
        else minOf(visible, chapters.size - count)
    return chapters.subList(start, minOf(start + count, chapters.size)).toList()
}

internal fun retainedReaderCommentWindow(states: Map<Long, ReaderChapterCommentState>, retainedIds: Set<Long>): Map<Long, ReaderChapterCommentState> = buildMap {
    states.forEach { (id, state) ->
        when {
            id in retainedIds || state.actionLoading -> put(id, state)
            state.draft.isNotEmpty() || state.replyingToCommentId != null -> put(id, state.copy(comments = LoadResult.Idle, bookReferences = emptyMap()))
        }
    }
}
