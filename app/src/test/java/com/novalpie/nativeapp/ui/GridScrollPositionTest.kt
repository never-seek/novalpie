package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GridScrollPositionTest {
    @Test
    fun clampsInvalidSavedViewportCoordinates() {
        assertEquals(
            GridScrollPosition(),
            GridScrollPosition.from(firstVisibleItemIndex = -3, firstVisibleItemScrollOffset = -12),
        )
    }

    @Test
    fun retainsValidSavedViewportCoordinates() {
        assertEquals(
            GridScrollPosition(firstVisibleItemIndex = 7, firstVisibleItemScrollOffset = 31),
            GridScrollPosition.from(firstVisibleItemIndex = 7, firstVisibleItemScrollOffset = 31),
        )
    }

}
