package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ReaderProgress
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookDetailProgressMarkerTest {
    @Test
    fun marksChapterOnlyWhenProgressBelongsToCurrentBookAndChapter() {
        val progress = ReaderProgress(bookId = 100, chapterId = 9001)

        assertTrue(isBookDetailProgressChapter(bookId = 100, chapterId = 9001, progress = progress))
        assertFalse(isBookDetailProgressChapter(bookId = 100, chapterId = 9002, progress = progress))
        assertFalse(isBookDetailProgressChapter(bookId = 200, chapterId = 9001, progress = progress))
    }

    @Test
    fun doesNotMarkChapterWhenThereIsNoProgress() {
        assertFalse(isBookDetailProgressChapter(bookId = 100, chapterId = 9001, progress = null))
    }
}
