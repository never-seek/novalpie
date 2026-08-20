package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ReaderSession

internal fun pushDistinctRoute(stack: List<AppRoute>, route: AppRoute): List<AppRoute> {
    if (stack.lastOrNull() == route) return stack
    return stack + route
}

internal fun replaceTopReaderRoute(stack: List<AppRoute>, route: AppRoute.Reader): List<AppRoute> {
    if (stack.lastOrNull() == route) return stack
    return if (stack.lastOrNull() is AppRoute.Reader) {
        stack.dropLast(1) + route
    } else {
        stack + route
    }
}

/** Rebuild the minimum meaningful Back stack after Android killed the reader process. */
internal fun readerSessionRouteStack(session: ReaderSession?): List<AppRoute> =
    session
        ?.takeIf { it.bookId > 0L && it.chapterId > 0L }
        ?.let { listOf(AppRoute.Home, AppRoute.BookDetail(it.bookId), AppRoute.Reader(it.bookId, it.chapterId)) }
        ?: listOf(AppRoute.Home)
