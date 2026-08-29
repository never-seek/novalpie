package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.NovelCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchGridPresentationTest {
    @Test
    fun gridRowKeysAreBundleSaveableAndStillExposeVisibleBookIds() {
        val key = searchGridRowKey(listOf(6L, 7L))

        assertEquals("search-row:6,7", key)
        assertEquals(listOf(6L, 7L), searchGridRowBookIds(key))
        assertEquals(emptyList<Long>(), searchGridRowBookIds("discover-panel"))
        assertEquals(emptyList<Long>(), searchGridRowBookIds(null))
    }

    @Test
    fun derivesUsableTagWidthForPhoneAndSixColumnLandscape() {
        assertEquals(168, searchGridTagContentWidthDp(412, 2))
        assertEquals(146, searchGridTagContentWidthDp(1_066, 6))
    }

    @Test
    fun roundsEachGridRowCoverToOneSharedHeight() {
        assertEquals(276, searchGridCoverHeightDp(412, 2))
        assertEquals(129, searchGridCoverHeightDp(411, 4))
    }

    @Test
    fun usesTheRichestCardInEachGridRowWithoutDroppingTags() {
        val short = NovelCard(
            id = 1,
            title = "short",
            platform = "novelPia",
            tags = listOf("奇幻", "完结"),
        )
        val rich = NovelCard(
            id = 2,
            title = "rich",
            platform = "novelPia",
            tags = listOf("奇幻", "轻小说", "武侠", "后悔", "病娇", "执念", "误解", "后宫", "致郁"),
        )
        val tagWidth = searchGridTagContentWidthDp(1_066, 6)

        val shortLines = searchGridTagLineCount(short, tagWidth)
        val richLines = searchGridTagLineCount(rich, tagWidth)
        val rowLines = searchGridRowTagLineCount(listOf(short, rich), tagWidth)

        assertTrue(richLines > shortLines)
        assertEquals(richLines, rowLines)
        assertTrue(searchGridTagAreaMinHeightDp(rowLines) >= SEARCH_GRID_TAG_MIN_AREA_HEIGHT_DP)
    }

    @Test
    fun reservesTheSecondTagLineWhenCjkPillsExceedTheEstimatedGridWidth() {
        val book = NovelCard(
            id = 3,
            title = "CJK",
            tags = listOf("一二三四五六", "七八九十一"),
        )

        // labelSmall is 12sp in the native typography. The former 11dp estimate treated these
        // two pills as one line at a 146dp rail, letting this card grow below its row peers.
        assertEquals(2, searchGridTagLineCount(book, availableTagWidthDp = 146))
    }

    @Test
    fun reservesTheFullTagPillHeightAndGapForEveryLine() {
        // At MuMu's 240dpi, four rendered pill rows occupy 137px (91.3dp). The
        // reservation must round up both the pill padding and the 2dp gaps so the
        // FlowRow cannot push the metrics/footer below its row peer.
        assertEquals(94, searchGridTagAreaMinHeightDp(lineCount = 4))
    }

    @Test
    fun narrowTwoColumnPhoneWrapsAllThreeMetricsInsteadOfClippingWordCount() {
        val book = NovelCard(
            id = 4,
            title = "Narrow metrics",
            favoriteCount = 2_400,
            siteReadCount = 296_000,
            wordCount = 1_221_000,
        )
        val metricWidth = searchGridTagContentWidthDp(availableWidthDp = 360, columnCount = 2)

        assertEquals(142, metricWidth)
        assertEquals(2, searchGridMetricLineCount(book, availableMetricWidthDp = metricWidth))
        assertEquals(40, searchGridMetricAreaMinHeightDp(lineCount = 2))
    }

    @Test
    fun filterChangesRefreshOnlyWhenSearchPageIsVisibleAndValueChanged() {
        assertTrue(searchFilterChangeShouldRefresh(isSearchRoute = true, changed = true))
        assertTrue(!searchFilterChangeShouldRefresh(isSearchRoute = true, changed = false))
        assertTrue(!searchFilterChangeShouldRefresh(isSearchRoute = false, changed = true))
    }
}
