package com.novalpie.nativeapp.ui

/**
 * A small route-owned viewport snapshot for grids and feeds that temporarily leave composition
 * when a detail page opens. Keeping this in the ViewModel makes Android Back restore the same
 * shelf, search row, or forum card instead of silently returning to the top.
 */
internal data class GridScrollPosition(
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemScrollOffset: Int = 0,
) {
    companion object {
        fun from(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int): GridScrollPosition =
            GridScrollPosition(
                firstVisibleItemIndex = firstVisibleItemIndex.coerceAtLeast(0),
                firstVisibleItemScrollOffset = firstVisibleItemScrollOffset.coerceAtLeast(0),
            )
    }
}
