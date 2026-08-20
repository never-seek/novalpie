package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ChineseVariant
import com.novalpie.nativeapp.model.next
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ChineseVariantPresentationTest {
    @Test
    fun followsTheSourceThreeStateCycle() {
        assertEquals(ChineseVariant.Traditional, ChineseVariant.Original.next())
        assertEquals(ChineseVariant.Simplified, ChineseVariant.Traditional.next())
        assertEquals(ChineseVariant.Original, ChineseVariant.Simplified.next())
    }

    @Test
    fun originalModeNeverMutatesTheSourceText() {
        assertEquals("阅读小说", convertChineseVariantText("阅读小说", ChineseVariant.Original))
    }

    @Test
    fun translatesBothDirectionsForVisibleReaderContent() {
        assertEquals("閱讀小說", convertChineseVariantText("阅读小说", ChineseVariant.Traditional))
        assertEquals("阅读小说", convertChineseVariantText("閱讀小說", ChineseVariant.Simplified))
    }

    @Test
    @Config(sdk = [28])
    fun usesTheLocalCompatibilityMapBeforeAndroidIcuIsAvailable() {
        assertEquals("\u66F8\u8B80", convertChineseVariantText("\u4E66\u8BFB", ChineseVariant.Traditional))
        assertEquals("\u4E66\u8BFB", convertChineseVariantText("\u66F8\u8B80", ChineseVariant.Simplified))
    }

    @Test
    fun labelsMatchTheWebsiteDrawer() {
        assertEquals("原文模式", ChineseVariant.Original.displayLabel())
        assertEquals("繁体模式", ChineseVariant.Traditional.displayLabel())
        assertEquals("简体模式", ChineseVariant.Simplified.displayLabel())
    }
}
