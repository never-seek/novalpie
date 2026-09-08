package com.novalpie.nativeapp.feature.download

import org.junit.Assert.*
import org.junit.Test

class ExportImageReconciliationTest {
    private val a = "https://images.test/a.webp"
    private val b = "https://images.test/b.gif"
    @Test fun removesOnlyAdditionalCopiesConfirmedByTheReaderSource() {
        val body = "正文前\n[图片: $a]\n正文后\n[图片: $a]"
        val result = reconcileExportImageOccurrences(body, listOf(a))
        assertEquals(1, result.removed)
        assertEquals("正文前\n[图片: $a]\n正文后\n", result.body)
    }
    @Test fun anAuthorMayIntentionallyShowTheSameImageTwice() {
        val body = "前\n[图片: $a]\n中\n[图片: $a]\n后"
        assertEquals(ExportImageReconciliation(body, 0), reconcileExportImageOccurrences(body, listOf(a, a)))
    }
    @Test fun keepsSourceOrderAndDoesNotTouchProseOrImageBytes() {
        val body = "A[图片: $a]B[图片: $b]C[图片: $a]\n[图片: $b]"
        val result = reconcileExportImageOccurrences(body, listOf(a, b))
        assertEquals(2, result.removed)
        assertEquals("A[图片: $a]B[图片: $b]C\n", result.body)
    }
    @Test fun missingOrMismatchedSourceDataMustNotSilentlyDeletePictures() {
        val body = "[图片: $a][图片: $a]"
        assertTrue(runCatching { reconcileExportImageOccurrences(body, emptyList()) }.isFailure)
        assertTrue(runCatching { reconcileExportImageOccurrences(body, listOf(b)) }.isFailure)
        assertTrue(runCatching { reconcileExportImageOccurrences(body, listOf(a, a, a)) }.isFailure)
    }
    @Test fun emptySourcePlaceholderIsNotInventedAsAnHttpImage() {
        val body = "前[图片: ]\n[图片: $a]\n后[图片: $a]"
        val corrected = reconcileExportImageOccurrences(body, listOf(a))
        assertEquals(1, corrected.removed)
        assertTrue(corrected.body.contains("[图片: ]"))
        assertNull(normalizedExportImageUrl(":"))
    }
    @Test fun olderExportOnlyPictureIsPreservedWhileKnownDuplicateCopiesAreRemoved() {
        val extra = "https://images.test/older.png"
        val body = "前[图片: $a]中[图片: $b]后[图片: $extra]\n[图片: $a][图片: $b]"
        val result = reconcileExportImageOccurrences(body, listOf(a, b))
        assertEquals(2, result.removed)
        assertEquals(1, result.sourceOnlyOccurrences)
        assertEquals("前[图片: $a]中[图片: $b]后[图片: $extra]\n", result.body)
    }
}
