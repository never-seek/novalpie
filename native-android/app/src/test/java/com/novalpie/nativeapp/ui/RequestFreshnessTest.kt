package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.LoadResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestFreshnessTest {
    @Test
    fun bookDetailResultIsFreshForTheSameBookAndItsNativeChildRoutes() {
        val state = BookDetailState(bookId = 200, book = LoadResult.Loading)

        assertTrue(
            isFreshBookDetailResult(
                route = AppRoute.BookDetail(200),
                state = state,
                requestedBookId = 200
            )
        )
        assertTrue(
            isFreshBookDetailResult(
                route = AppRoute.Terminology(200),
                state = state,
                requestedBookId = 200
            )
        )
        assertTrue(
            isFreshBookDetailResult(
                route = AppRoute.Reader(bookId = 200, chapterId = 9001),
                state = state,
                requestedBookId = 200
            )
        )
        assertTrue(
            isFreshBookDetailResult(
                route = AppRoute.BookEditInfo(bookId = 200),
                state = state,
                requestedBookId = 200
            )
        )
        assertTrue(
            isFreshBookDetailResult(
                route = AppRoute.BookChapters(bookId = 200),
                state = state,
                requestedBookId = 200
            )
        )
        assertTrue(
            isFreshBookDetailResult(
                route = AppRoute.BookAppend(bookId = 200),
                state = state,
                requestedBookId = 200
            )
        )
        assertFalse(
            isFreshBookDetailResult(
                route = AppRoute.BookDetail(300),
                state = state.copy(bookId = 300),
                requestedBookId = 200
            )
        )
        assertFalse(
            isFreshBookDetailResult(
                route = AppRoute.Home,
                state = state,
                requestedBookId = 200
            )
        )
    }

    @Test
    fun readerResultIsFreshOnlyForCurrentReaderRouteAndChapter() {
        val state = ReaderState(bookId = 200, chapterId = 9001, content = LoadResult.Loading)

        assertTrue(
            isFreshReaderResult(
                route = AppRoute.Reader(bookId = 200, chapterId = 9001),
                state = state,
                requestedBookId = 200,
                requestedChapterId = 9001
            )
        )
        assertFalse(
            isFreshReaderResult(
                route = AppRoute.Reader(bookId = 200, chapterId = 9002),
                state = state.copy(chapterId = 9002),
                requestedBookId = 200,
                requestedChapterId = 9001
            )
        )
        assertFalse(
            isFreshReaderResult(
                route = AppRoute.BookDetail(bookId = 200),
                state = state,
                requestedBookId = 200,
                requestedChapterId = 9001
            )
        )
    }

    @Test
    fun terminologyResultIsFreshOnlyForTheCurrentBookTerminologyRoute() {
        val state = TerminologyState(bookId = 200, entries = LoadResult.Loading)

        assertTrue(
            isFreshTerminologyResult(
                route = AppRoute.Terminology(200),
                state = state,
                requestedBookId = 200,
            )
        )
        assertFalse(
            isFreshTerminologyResult(
                route = AppRoute.Terminology(201),
                state = state.copy(bookId = 201),
                requestedBookId = 200,
            )
        )
        assertFalse(
            isFreshTerminologyResult(
                route = AppRoute.BookDetail(200),
                state = state,
                requestedBookId = 200,
            )
        )
    }

    @Test
    fun readerStateCarriesChapterCommentLoadingState() {
        val state = ReaderState(
            bookId = 200,
            chapterId = 9001,
            content = LoadResult.Loading,
            comments = LoadResult.Loading
        )

        assertTrue(state.comments is LoadResult.Loading)
        assertTrue(
            isFreshReaderResult(
                route = AppRoute.Reader(bookId = 200, chapterId = 9001),
                state = state,
                requestedBookId = 200,
                requestedChapterId = 9001
            )
        )
    }

    @Test
    fun readerStateKeepsNextChapterTerminalStateSeparateFromAnError() {
        val state = ReaderState(
            bookId = 200,
            chapterId = 9001,
            content = LoadResult.Success(
                com.novalpie.nativeapp.model.ReaderContent(
                    title = "第一章",
                    content = "正文",
                    source = "test",
                )
            ),
            nextChapterExhausted = true,
        )

        assertTrue(state.nextChapterExhausted)
        assertNull(state.nextChapterError)
        assertFalse(state.loadingNextChapter)
    }

    @Test
    fun readerStateCanRepresentARecoverableCatalogWaitSeparatelyFromBookEnd() {
        val state = ReaderState(
            bookId = 200,
            chapterId = 9001,
            content = LoadResult.Success(
                com.novalpie.nativeapp.model.ReaderContent(
                    title = "第一章",
                    content = "正文",
                    source = "test",
                )
            ),
            nextChapterWaitingForCatalog = true,
        )

        assertTrue(state.nextChapterWaitingForCatalog)
        assertFalse(state.nextChapterExhausted)
        assertNull(state.nextChapterError)
    }

    @Test
    fun searchResultIsFreshOnlyForCurrentRequestAndOptions() {
        val options = SearchOptions(sortBy = "latest", scope = "title")
        val request = SearchRequestSnapshot(
            serial = 42,
            keyword = "alpha",
            options = options,
            page = 1
        )

        assertTrue(
            isFreshSearchResult(
                request = request,
                activeSerial = 42,
                currentKeyword = "alpha",
                currentOptions = options,
                expectedPage = 1
            )
        )
        assertFalse(
            isFreshSearchResult(
                request = request,
                activeSerial = 43,
                currentKeyword = "alpha",
                currentOptions = options,
                expectedPage = 1
            )
        )
        assertFalse(
            isFreshSearchResult(
                request = request,
                activeSerial = 42,
                currentKeyword = "beta",
                currentOptions = options,
                expectedPage = 1
            )
        )
        assertFalse(
            isFreshSearchResult(
                request = request,
                activeSerial = 42,
                currentKeyword = "alpha",
                currentOptions = options.copy(sortOrder = "asc"),
                expectedPage = 1
            )
        )
        assertFalse(
            isFreshSearchResult(
                request = request.copy(page = 2),
                activeSerial = 42,
                currentKeyword = "alpha",
                currentOptions = options,
                expectedPage = 1
            )
        )
    }

    @Test
    fun searchSubmissionUsesExplicitFieldValueWhenPresent() {
        assertEquals("354491", searchKeywordForSubmission(currentKeyword = "aa", submittedKeyword = "354491"))
        assertEquals("354491", searchKeywordForSubmission(currentKeyword = "aa", submittedKeyword = " 354491 "))
        assertEquals("aa", searchKeywordForSubmission(currentKeyword = "aa", submittedKeyword = null))
        assertEquals("aa", searchKeywordForSubmission(currentKeyword = "aa", submittedKeyword = ""))
        assertEquals("", searchKeywordForSubmission(currentKeyword = "", submittedKeyword = ""))
    }

    @Test
    fun homeResultIsFreshOnlyForCurrentRequest() {
        assertTrue(isFreshRequestSerial(requestSerial = 7, activeSerial = 7))
        assertFalse(isFreshRequestSerial(requestSerial = 7, activeSerial = 8))
    }
}
