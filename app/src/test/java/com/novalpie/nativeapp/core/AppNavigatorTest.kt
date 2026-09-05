package com.novalpie.nativeapp.core

import com.novalpie.nativeapp.ui.AppRoute
import org.junit.Assert.*
import org.junit.Test

class AppNavigatorTest {
    @Test fun distinctPushAndReaderReplacementKeepTheMeaningfulBackChain() {
        val navigator=AppNavigator(listOf(AppRoute.Home))
        navigator.push(AppRoute.BookDetail(10))
        navigator.push(AppRoute.BookDetail(10))
        navigator.replaceReader(AppRoute.Reader(10,100))
        navigator.replaceReader(AppRoute.Reader(10,101))
        assertEquals(listOf(AppRoute.Home,AppRoute.BookDetail(10),AppRoute.Reader(10,101)),navigator.entries)
        assertTrue(navigator.pop())
        assertEquals(AppRoute.BookDetail(10),navigator.current)
        assertTrue(navigator.pop())
        assertFalse(navigator.pop())
        assertEquals(AppRoute.Home,navigator.current)
    }
    @Test fun resettingTheStackCannotExposeTransientEmptyNavigation() {
        val navigator=AppNavigator(listOf(AppRoute.Home,AppRoute.BookDetail(10)))
        navigator.replaceAll(emptyList())
        assertEquals(listOf(AppRoute.Home),navigator.entries)
        navigator.reset(AppRoute.Search)
        assertEquals(listOf(AppRoute.Search),navigator.entries)
    }
}
