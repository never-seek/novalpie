package com.novalpie.nativeapp.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.model.ReaderViewportAnchor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderViewportProgressTest {
    @Test
    fun viewportAnchorPersistsLocallyForTheActiveReaderChapter() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = NovalPieViewModel(application)

        viewModel.openReader(bookId = 908001L, chapterId = 908010L)
        viewModel.recordReaderViewportAnchor(
            ReaderViewportAnchor(
                chapterId = 908010L,
                itemIndexWithinChapter = 4,
                itemScrollOffsetPx = 72,
            ),
        )

        assertEquals(908001L, viewModel.readerProgress?.bookId)
        assertEquals(908010L, viewModel.readerProgress?.chapterId)
        assertEquals(4, viewModel.readerProgress?.viewportItemIndex)
        assertEquals(72, viewModel.readerProgress?.viewportItemScrollOffsetPx)
    }
}
