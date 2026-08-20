package com.novalpie.nativeapp.data

import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.model.Chapter
import com.novalpie.nativeapp.model.ChapterIllustration
import com.novalpie.nativeapp.model.ReaderChapterCacheState
import com.novalpie.nativeapp.model.ReaderContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderChapterCacheStoreTest {
    private lateinit var store: ReaderChapterCacheStore

    @Before
    fun setUp() {
        store = ReaderChapterCacheStore(ApplicationProvider.getApplicationContext())
        store.clearBook(BOOK_ID)
    }

    @Test
    fun roundTripsAChapterBodyAndItsIllustrations() {
        val content = ReaderContent(
            title = "第一章",
            content = "第一段\n第二段",
            source = "novelPia",
            illustrations = listOf(ChapterIllustration(id = 7L, index = 1, src = "https://image.example/7.webp")),
        )

        assertTrue(
            store.save(
                bookId = BOOK_ID,
                chapterId = 11L,
                replaceMode = "india",
                showImages = true,
                sourceUpdatedAt = "2026-08-17T10:00:00",
                content = content,
            ),
        )

        val restored = store.load(BOOK_ID, 11L, replaceMode = "india", showImages = true)
        assertEquals(content, restored?.content)
        assertEquals("2026-08-17T10:00:00", restored?.sourceUpdatedAt)
        assertTrue((restored?.cachedAtMillis ?: 0L) > 0L)
    }

    @Test
    fun separatesTextAndReplacementVariants() {
        store.save(
            bookId = BOOK_ID,
            chapterId = 11L,
            replaceMode = "india",
            showImages = true,
            sourceUpdatedAt = null,
            content = sampleContent("印度模式"),
        )

        assertNull(store.load(BOOK_ID, 11L, replaceMode = "korea", showImages = true))
        assertNull(store.load(BOOK_ID, 11L, replaceMode = "india", showImages = false))
        assertEquals("印度模式", store.load(BOOK_ID, 11L, replaceMode = "india", showImages = true)?.content?.content)
    }

    @Test
    fun marksAStoredChapterCurrentOnlyWhenItsSourceRevisionMatches() {
        store.save(
            bookId = BOOK_ID,
            chapterId = 11L,
            replaceMode = "india",
            showImages = true,
            sourceUpdatedAt = "2026-08-17T10:00:00",
            content = sampleContent("正文"),
        )

        val current = store.cacheStates(
            bookId = BOOK_ID,
            replaceMode = "india",
            showImages = true,
            chapters = listOf(
                Chapter(id = 11L, title = "第1章", updatedAt = "2026-08-17T10:00:00"),
                Chapter(id = 12L, title = "第2章", updatedAt = "2026-08-17T10:00:00"),
            ),
        )
        assertEquals(ReaderChapterCacheState.Current, current[11L])
        assertEquals(ReaderChapterCacheState.Missing, current[12L])

        val stale = store.cacheStates(
            bookId = BOOK_ID,
            replaceMode = "india",
            showImages = true,
            chapters = listOf(Chapter(id = 11L, title = "第1章", updatedAt = "2026-08-18T10:00:00")),
        )
        assertEquals(ReaderChapterCacheState.Stale, stale[11L])
    }

    @Test
    fun clearBookOnlyRemovesTheRequestedBookCache() {
        store.save(BOOK_ID, 11L, "india", true, null, sampleContent("A"))
        store.save(OTHER_BOOK_ID, 22L, "india", true, null, sampleContent("B"))

        store.clearBook(BOOK_ID)

        assertNull(store.load(BOOK_ID, 11L, "india", true))
        assertTrue(store.load(OTHER_BOOK_ID, 22L, "india", true) != null)
    }

    private fun sampleContent(value: String) = ReaderContent(
        title = "章节",
        content = value,
        source = "test",
    )

    private companion object {
        const val BOOK_ID = 987_654L
        const val OTHER_BOOK_ID = 987_655L
    }
}
