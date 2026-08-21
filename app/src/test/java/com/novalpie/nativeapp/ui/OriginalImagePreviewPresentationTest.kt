package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.data.BookCoverPhoto
import com.novalpie.nativeapp.model.NovelCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OriginalImagePreviewPresentationTest {
    @Test
    fun bookCoverPreviewUsesTheServerOriginalInsteadOfTheSearchThumbnail() {
        val outer = "https://images.novelpia.com/imagebox/cover/outer.file"
        val inner = "https://images.novelpia.com/imagebox/cover/inner.file"

        assertEquals(
            inner,
            originalBookCoverPreviewUrl(
                book = NovelCard(id = 350259, title = "Layered cover", coverUrl = outer),
                photo = BookCoverPhoto(previewUrl = outer, originalUrl = inner),
            ),
        )
    }

    @Test
    fun bookCoverPreviewNeverPromotesTheOuterThumbnailWhenNoOriginalIsKnown() {
        val outer = "https://images.novelpia.com/imagebox/cover/outer.file"

        assertNull(
            originalBookCoverPreviewUrl(
                book = NovelCard(id = 350259, title = "Layered cover", coverUrl = outer),
                photo = null,
            ),
        )
    }

    @Test
    fun readerPreviewUsesItsExplicitOriginalOrMatchingBookCoverOriginal() {
        val outer = "https://images.novelpia.com/imagebox/cover/outer.file"
        val inner = "https://images.novelpia.com/imagebox/cover/inner.file"

        assertEquals(
            "https://images.novelpia.com/chapter/inner.webp",
            originalReaderImagePreviewUrl(
                image = ReaderContentBlock.Image(
                    url = "https://images.novelpia.com/chapter/outer.webp",
                    originalUrl = "https://images.novelpia.com/chapter/inner.webp",
                ),
                bookPhoto = null,
            ),
        )
        assertEquals(
            inner,
            originalReaderImagePreviewUrl(
                image = ReaderContentBlock.Image(url = outer),
                bookPhoto = BookCoverPhoto(previewUrl = outer, originalUrl = inner),
            ),
        )
    }
}
