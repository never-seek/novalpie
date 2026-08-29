package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderActionPresentationTest {
    @Test
    fun sourceRailHasStableActionIdsAndLabelsInSourceOrder() {
        assertEquals(
            listOf(
                ReaderRailActionId.Close,
                ReaderRailActionId.Help,
                ReaderRailActionId.Catalog,
                ReaderRailActionId.Settings,
                ReaderRailActionId.Theme,
                ReaderRailActionId.Previous,
                ReaderRailActionId.Next,
                ReaderRailActionId.ReadingMode,
                ReaderRailActionId.Tts,
                ReaderRailActionId.Fullscreen,
                ReaderRailActionId.Navigation,
            ),
            readerRailActionSpecs().map(ReaderRailActionSpec::id),
        )
        assertEquals(
            listOf("关闭", "帮助", "目录", "设置", "主题", "上章", "下章", "滑动", "听书", "全屏", "导航"),
            readerRailActionSpecs().map(ReaderRailActionSpec::label),
        )
    }

    @Test
    fun availabilityKeepsChapterAndTtsActionsHonest() {
        assertFalse(readerRailActionEnabled(ReaderRailActionId.Previous, hasPrevious = false, hasNext = true, showTts = true))
        assertTrue(readerRailActionEnabled(ReaderRailActionId.Next, hasPrevious = false, hasNext = true, showTts = true))
        assertFalse(readerRailActionEnabled(ReaderRailActionId.Tts, hasPrevious = true, hasNext = true, showTts = false))
        assertTrue(readerRailActionEnabled(ReaderRailActionId.Tts, hasPrevious = true, hasNext = true, showTts = true))
    }

    @Test
    fun disablingTtsRemovesItsRailControlInsteadOfShowingADisabledButton() {
        val visible = readerVisibleRailActionSpecs(showTts = false)

        assertFalse(visible.any { it.id == ReaderRailActionId.Tts })
        assertEquals(
            listOf(
                ReaderRailActionId.Close,
                ReaderRailActionId.Help,
                ReaderRailActionId.Catalog,
                ReaderRailActionId.Settings,
                ReaderRailActionId.Theme,
                ReaderRailActionId.Previous,
                ReaderRailActionId.Next,
                ReaderRailActionId.ReadingMode,
                ReaderRailActionId.Fullscreen,
                ReaderRailActionId.Navigation,
            ),
            visible.map(ReaderRailActionSpec::id),
        )
    }
}
