package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.TerminologyEntry
import com.novalpie.nativeapp.model.TerminologyPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminologyPresentationTest {
    @Test
    fun terminologyLabelsKeepMissingSourceFieldsHonest() {
        val entry = TerminologyEntry(
            id = 1,
            novelId = 95654,
            sourceName = " ",
            targetName = "",
        )

        assertEquals("未命名原文", terminologySourceLabel(entry))
        assertEquals("未填写译名", terminologyTargetLabel(entry))
        assertEquals("未锁定", terminologyLockPresentation(null).label)
        assertEquals("已锁定", terminologyLockPresentation("locked").label)
        assertTrue(terminologyLockPresentation("locked").isWarning)
        assertEquals("已启用", terminologyActivePresentation(true).label)
        assertEquals("已停用", terminologyActivePresentation(false).label)
        assertTrue(terminologyActivePresentation(false).isWarning)
    }

    @Test
    fun terminologyPagingUsesMetadataWithoutLoadingEverythingAtOnce() {
        val first = TerminologyPage(
            items = List(20) { index ->
                TerminologyEntry(
                    id = index.toLong() + 1,
                    novelId = 95654,
                    sourceName = "source $index",
                    targetName = "target $index",
                )
            },
            page = 0,
            pageSize = 20,
            total = 16507,
            totalPages = 826,
        )
        val last = first.copy(page = 825)

        assertTrue(canLoadMoreTerminologyEntries(first, loadedCount = 20))
        assertFalse(canLoadMoreTerminologyEntries(last, loadedCount = 16507))
        assertEquals("已加载 20 / 16507 条术语", terminologySummaryLabel(first, loadedCount = 20))
    }

    @Test
    fun terminologyPagingFallsBackToServerPageSizeWhenTotalMetadataIsMissing() {
        fun item(index: Int) = TerminologyEntry(
            id = index.toLong() + 1,
            novelId = 95654,
            sourceName = "source $index",
            targetName = "target $index",
        )
        val fullPage = TerminologyPage(items = List(20, ::item), page = 2, pageSize = 20)
        val shortPage = TerminologyPage(items = List(19, ::item), page = 2, pageSize = 20)

        assertTrue(canLoadMoreTerminologyEntries(fullPage, loadedCount = 60))
        assertFalse(canLoadMoreTerminologyEntries(shortPage, loadedCount = 59))
        assertEquals("已加载 59 条术语", terminologySummaryLabel(shortPage, loadedCount = 59))
    }

    @Test
    fun terminologyQueryNeverLeaksBetweenBooks() {
        val state = TerminologyState(bookId = 100, keyword = "魔力")

        assertEquals("魔力", terminologyKeywordForBook(state, 100))
        assertEquals("", terminologyKeywordForBook(state, 200))
    }
}
