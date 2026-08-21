package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTagFiltersTest {
    @Test
    fun parserTrimsChineseAndAsciiCommaSeparatedTagsWithoutDuplicates() {
        assertEquals(
            listOf("奇幻", "同人", "Fantasy"),
            parseSearchTagInput(" 奇幻， 同人, Fantasy, fantasy ")
        )
    }

    @Test
    fun requiredTagMovesFromBlockedSetAndTogglesOffOnSecondTap() {
        val moved = toggleSearchTagFilters(
            requiredTags = listOf("轻小说"),
            blockedTags = listOf("奇幻", "后宫"),
            input = "奇幻",
            mode = SearchTagFilterMode.Required
        )

        assertEquals(listOf("轻小说", "奇幻"), moved.first)
        assertEquals(listOf("后宫"), moved.second)

        val removed = toggleSearchTagFilters(
            requiredTags = moved.first,
            blockedTags = moved.second,
            input = "奇幻",
            mode = SearchTagFilterMode.Required
        )

        assertEquals(listOf("轻小说"), removed.first)
        assertEquals(listOf("后宫"), removed.second)
    }

    @Test
    fun blockedTagsSupportMultipleManualEntriesAndExplicitRemoval() {
        val added = toggleSearchTagFilters(
            requiredTags = listOf("奇幻"),
            blockedTags = emptyList(),
            input = "后宫, 病娇",
            mode = SearchTagFilterMode.Blocked
        )

        assertEquals(listOf("奇幻"), added.first)
        assertEquals(listOf("后宫", "病娇"), added.second)
        assertEquals(
            listOf("病娇"),
            removeSearchTagFilter(added.first, added.second, "后宫").second
        )
    }
}
