package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ReaderProgress

internal fun isBookDetailProgressChapter(
    bookId: Long,
    chapterId: Long,
    progress: ReaderProgress?
): Boolean =
    progress?.bookId == bookId && progress.chapterId == chapterId
