package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class BookCoverFallbackTest {
    @Test
    fun coverAspectRatioMatchesWebsitePortraitCovers() {
        assertEquals(2f / 3f, bookCoverAspectRatio(), 0.001f)
    }

    @Test
    fun novelGridColumnCountKeepsCoversAndTagsReadable() {
        assertEquals(2, novelGridColumnCount())
    }

    @Test
    fun coverFallbackTextUsesFirstNonBlankTitleCharacter() {
        assertEquals("N", bookCoverFallbackText(" NovalPie"))
        assertEquals("书", bookCoverFallbackText("  书名"))
    }

    @Test
    fun coverFallbackTextUsesDefaultForBlankTitle() {
        assertEquals("N", bookCoverFallbackText(""))
        assertEquals("N", bookCoverFallbackText("   "))
    }

    @Test
    fun bookDetailCoverUsesStationaryLongPressPreviewPolicy() {
        assertEquals(CoverPreviewPolicy.LongPressOnly, bookDetailCoverPreviewPolicy())
    }
}
