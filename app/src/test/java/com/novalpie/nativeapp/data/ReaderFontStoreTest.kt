package com.novalpie.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderFontStoreTest {
    @Test
    fun acceptsOnlySupportedCustomFontKeysAndKeepsTheirFilename() {
        val key = ReaderFontStore.customFontKey("NotoSansCJK.otf")

        assertTrue(ReaderFontStore.isSupportedFamily(key))
        assertEquals("NotoSansCJK.otf", ReaderFontStore.customFontFileName(key))
        assertEquals("NotoSansCJK.otf", ReaderFontStore.displayName(key))
        assertFalse(ReaderFontStore.isSupportedFamily("custom-font:font.woff2"))
        assertNull(ReaderFontStore.customFontFileName("custom-font:../font.otf"))
    }
}
