package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ReaderSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RouteStackPolicyTest {
    @Test
    fun readerSessionBuildsABackStackThatReturnsToBookDetail() {
        assertEquals(
            listOf(AppRoute.Home, AppRoute.BookDetail(354491), AppRoute.Reader(354491, 6992449)),
            readerSessionRouteStack(ReaderSession(bookId = 354491, chapterId = 6992449)),
        )
    }

    @Test
    fun invalidOrMissingReaderSessionFallsBackToCollection() {
        assertEquals(listOf(AppRoute.Home), readerSessionRouteStack(null))
        assertEquals(listOf(AppRoute.Home), readerSessionRouteStack(ReaderSession(bookId = 0, chapterId = 1)))
    }

    @Test
    fun pushDistinctRouteDoesNotDuplicateCurrentTopRoute() {
        val stack = listOf(AppRoute.Home, AppRoute.BookDetail(354491))

        val next = pushDistinctRoute(stack, AppRoute.BookDetail(354491))

        assertSame(stack, next)
    }

    @Test
    fun pushDistinctRouteAddsDifferentDetailRoute() {
        val stack = listOf(AppRoute.Home, AppRoute.BookDetail(100))

        val next = pushDistinctRoute(stack, AppRoute.BookDetail(200))

        assertEquals(listOf(AppRoute.Home, AppRoute.BookDetail(100), AppRoute.BookDetail(200)), next)
    }

    @Test
    fun replaceTopReaderRouteSkipsReloadForSameReaderChapter() {
        val stack = listOf(AppRoute.Home, AppRoute.BookDetail(354491), AppRoute.Reader(354491, 9001))

        val next = replaceTopReaderRoute(stack, AppRoute.Reader(354491, 9001))

        assertSame(stack, next)
    }

    @Test
    fun replaceTopReaderRouteReplacesCurrentReaderChapter() {
        val stack = listOf(AppRoute.Home, AppRoute.BookDetail(354491), AppRoute.Reader(354491, 9001))

        val next = replaceTopReaderRoute(stack, AppRoute.Reader(354491, 9002))

        assertEquals(listOf(AppRoute.Home, AppRoute.BookDetail(354491), AppRoute.Reader(354491, 9002)), next)
    }

    @Test
    fun webFallbackRouteIsNotDuplicatedOnDoubleTap() {
        val stack = listOf(AppRoute.Home, AppRoute.WebFallback("https://novalpie.cc/favorites"))

        val next = pushDistinctRoute(stack, AppRoute.WebFallback("https://novalpie.cc/favorites"))

        assertSame(stack, next)
    }

    @Test
    fun forumPostDetailRouteIsNotDuplicatedOnDoubleTap() {
        val stack = listOf(AppRoute.Forum, AppRoute.ForumPostDetail(91))

        val next = pushDistinctRoute(stack, AppRoute.ForumPostDetail(91))

        assertSame(stack, next)
    }

    @Test
    fun forumPostDetailRouteCanMoveBetweenDifferentPosts() {
        val stack = listOf(AppRoute.Forum, AppRoute.ForumPostDetail(91))

        val next = pushDistinctRoute(stack, AppRoute.ForumPostDetail(92))

        assertEquals(listOf(AppRoute.Forum, AppRoute.ForumPostDetail(91), AppRoute.ForumPostDetail(92)), next)
    }
}
