package com.novalpie.nativeapp.feature.reader.preferences

import com.novalpie.nativeapp.ui.ReaderUiOptions
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderPreferenceCodecTest {
    @Test fun explicitCloudAnimationCannotReplaceNativeNoAnimation() {
        val merged = ReaderPreferenceCodec.merge(JSONObject("""{"pageTurnMode":true,"pageTurnEffect":"slide","fontSize":24}"""),
            ReaderUiOptions(pageTurnEffect = "none", volumeKeyPageTurn = false))
        assertEquals("none", merged.pageTurnEffect)
        assertEquals(24, merged.fontSizeSp)
        assertTrue(merged.pageTurnMode)
        assertFalse(merged.volumeKeyPageTurn)
    }

    @Test fun cloudAnimationStillAppliesWhenNativeAnimationIsEnabled() {
        val merged = ReaderPreferenceCodec.merge(JSONObject("""{"pageTurnEffect":"slide"}"""),
            ReaderUiOptions(pageTurnEffect = "fade"))
        assertEquals("slide", merged.pageTurnEffect)
    }
    @Test fun websiteFieldsMapWithoutRevivingRetiredRadialMenuOrOverwritingNativeVolumePreference() {
        val source = JSONObject("""{"fontSize":24,"lineHeight":2,"showTTS":false,"showImages":false,"showRadialMenu":true,"theme":"theme-dark","screenPadding":{"top":12,"bottom":16},"pageTurnMode":true,"useInfiniteScroll":true}""")
        val merged = ReaderPreferenceCodec.merge(source, ReaderUiOptions(volumeKeyPageTurn = false, pageTurnEffect = "none"))
        assertEquals(24, merged.fontSizeSp); assertEquals(2f, merged.lineHeight)
        assertFalse(merged.showTts); assertFalse(merged.showImages); assertFalse(merged.showRadialMenu)
        assertFalse(merged.volumeKeyPageTurn); assertEquals("none", merged.pageTurnEffect)
        assertEquals("dark", merged.theme); assertEquals(12, merged.screenPaddingTopDp); assertFalse(merged.useInfiniteScroll)
    }
    @Test fun outgoingFieldsUseWebsiteVocabularyAndExcludeDeviceLocalPaths() {
        val encoded = ReaderPreferenceCodec.encode(ReaderUiOptions(fontSizeSp = 22, showTts = false, theme = "dark", fontFamily = "custom:file:///private-font"))
        assertEquals(22, encoded.getInt("fontSize")); assertFalse(encoded.getBoolean("showTTS"))
        assertEquals("theme-dark", encoded.getString("theme"))
        assertFalse(encoded.toString().contains("file:///")); assertFalse(encoded.has("showRadialMenu"))
        assertFalse(encoded.has("volumeKeyPageTurn"))
    }
}
