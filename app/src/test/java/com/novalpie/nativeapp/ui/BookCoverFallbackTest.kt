package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun coverPreviewWaitsPastThePlatformLongPressThreshold() {
        assertFalse(
            coverPreviewLongPressShouldTrigger(
                durationMillis = 800L,
                distancePx = 0f,
                touchSlopPx = 8f,
                platformTimeoutMillis = 500L,
            )
        )
        assertFalse(
            coverPreviewLongPressShouldTrigger(
                durationMillis = 899L,
                distancePx = 0f,
                touchSlopPx = 8f,
                platformTimeoutMillis = 500L,
            )
        )
        assertTrue(
            coverPreviewLongPressShouldTrigger(
                durationMillis = 900L,
                distancePx = 0f,
                touchSlopPx = 8f,
                platformTimeoutMillis = 500L,
            )
        )
    }

    @Test
    fun coverPreviewCancelsWhenThePointerReachesScrollSlop() {
        assertFalse(
            coverPreviewLongPressShouldTrigger(
                durationMillis = 1_000L,
                distancePx = 8f,
                touchSlopPx = 8f,
                platformTimeoutMillis = 500L,
            )
        )
        assertTrue(
            coverPreviewLongPressShouldTrigger(
                durationMillis = 1_000L,
                distancePx = 2f,
                touchSlopPx = 8f,
                platformTimeoutMillis = 500L,
            )
        )
    }
}
