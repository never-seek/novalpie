package com.novalpie.nativeapp.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class ImagePreviewTransformTest {
    @Test
    fun clampsZoomAndPanToVisibleImageBounds() {
        assertEquals(1f, clampImagePreviewScale(0.2f), 0.001f)
        assertEquals(6f, clampImagePreviewScale(10f), 0.001f)
        assertEquals(Offset.Zero, clampImagePreviewOffset(Offset(50f, 50f), 1f, IntSize(900, 1600)))
        assertEquals(
            Offset(450f, -800f),
            clampImagePreviewOffset(Offset(9999f, -9999f), 2f, IntSize(900, 1600))
        )
    }
}
