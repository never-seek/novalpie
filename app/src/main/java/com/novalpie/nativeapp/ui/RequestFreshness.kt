package com.novalpie.nativeapp.ui

internal fun isFreshBookDetailResult(
    route: AppRoute,
    state: BookDetailState,
    requestedBookId: Long
): Boolean {
    if (state.bookId != requestedBookId) return false
    return when (route) {
        is AppRoute.BookDetail -> route.bookId == requestedBookId
        is AppRoute.Reader -> route.bookId == requestedBookId
        else -> false
    }
}

internal fun isFreshReaderResult(
    route: AppRoute,
    state: ReaderState,
    requestedBookId: Long,
    requestedChapterId: Long
): Boolean {
    if (state.bookId != requestedBookId || state.chapterId != requestedChapterId) return false
    return route is AppRoute.Reader &&
        route.bookId == requestedBookId &&
        route.chapterId == requestedChapterId
}

internal data class SearchRequestSnapshot(
    val serial: Long,
    val keyword: String,
    val options: SearchOptions,
    val page: Int
)

internal fun isFreshRequestSerial(requestSerial: Long, activeSerial: Long): Boolean =
    requestSerial == activeSerial

internal fun isFreshSearchResult(
    request: SearchRequestSnapshot,
    activeSerial: Long,
    currentKeyword: String,
    currentOptions: SearchOptions,
    expectedPage: Int
): Boolean =
    isFreshRequestSerial(request.serial, activeSerial) &&
        request.keyword == currentKeyword &&
        request.options == currentOptions &&
        request.page == expectedPage

internal fun searchKeywordForSubmission(currentKeyword: String, submittedKeyword: String?): String =
    (submittedKeyword ?: currentKeyword).trim()
