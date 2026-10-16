package com.novalpie.nativeapp.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import java.io.File

private const val MAX_ALPHA_SCAN_PIXELS = 256 * 1024
private const val MAX_ALPHA_SCAN_ROWS = 128

/** hasAlpha() is only a capability flag, not proof of transparent pixels. Inspect before JPEG. */
internal fun nativeImageContainsTransparency(
    source: File,
    decoded: Bitmap,
    originalWidth: Int,
    originalHeight: Int,
): Boolean {
    if (decoded.width == originalWidth && decoded.height == originalHeight) {
        return try { bitmapContainsTransparency(decoded) }
            catch (_: Exception) { true } catch (_: OutOfMemoryError) { true }
    }
    // Sampling can erase a sparse transparent pixel. Inspect original-resolution regions rather
    // than trusting the sampled bitmap, or allocating an unbounded full-size ARGB bitmap.
    if (originalWidth <= 0 || originalHeight <= 0 || originalWidth > MAX_ALPHA_SCAN_PIXELS) return true
    return try {
        // Own/close the input explicitly: some native decoders retain path-open handles until
        // finalization, which can prevent the staging file from being renamed on Windows tests.
        source.inputStream().use { input ->
            @Suppress("DEPRECATION")
            val decoder = BitmapRegionDecoder.newInstance(input, false) ?: return true
            try {
                if (decoder.width != originalWidth || decoder.height != originalHeight) return true
                val rows = minOf(MAX_ALPHA_SCAN_ROWS, maxOf(1, MAX_ALPHA_SCAN_PIXELS / originalWidth))
                val pixels = IntArray(originalWidth * rows)
                val options = BitmapFactory.Options().apply { inSampleSize = 1; inPreferredConfig = Bitmap.Config.ARGB_8888 }
                var top = 0
                while (top < originalHeight) {
                    val bottom = minOf(originalHeight, top + rows)
                    val region = decoder.decodeRegion(Rect(0, top, originalWidth, bottom), options) ?: return true
                    try {
                        if (region.width != originalWidth || region.height != bottom - top) return true
                        if (bitmapContainsTransparency(region, pixels)) return true
                    } finally { region.recycle() }
                    top = bottom
                }
                false
            } finally { decoder.recycle() }
        }
    } catch (_: Exception) { true // Unsupported/corrupt image: keep its original bytes.
    } catch (_: OutOfMemoryError) { true }

}

internal fun nativeBitmapContainsFullyTransparentPixels(bitmap: Bitmap): Boolean =
    try { bitmapContainsTransparency(bitmap, onlyFullyTransparent = true) }
    catch (_: Exception) { true } catch (_: OutOfMemoryError) { true }

private fun bitmapContainsTransparency(bitmap: Bitmap, scratch: IntArray? = null, onlyFullyTransparent: Boolean = false): Boolean {
    if (!bitmap.hasAlpha()) return false
    if (bitmap.width <= 0 || bitmap.width > MAX_ALPHA_SCAN_PIXELS) return true
    val rows = minOf(MAX_ALPHA_SCAN_ROWS, maxOf(1, MAX_ALPHA_SCAN_PIXELS / bitmap.width))
    val pixels = scratch ?: IntArray(bitmap.width * rows)
    var top = 0
    while (top < bitmap.height) {
        val height = minOf(rows, bitmap.height - top)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, top, bitmap.width, height)
        for (index in 0 until bitmap.width * height) {
            val alpha = pixels[index] ushr 24
            if (if (onlyFullyTransparent) alpha == 0 else alpha != 255) return true
        }
        top += height
    }
    return false
}
