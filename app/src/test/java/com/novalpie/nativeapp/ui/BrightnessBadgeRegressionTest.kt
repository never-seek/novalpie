package com.novalpie.nativeapp.ui

import android.app.Activity
import androidx.compose.ui.layout.ContentScale
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrightnessBadgeRegressionTest {
    @Test fun hoverContainCannotOverrideRestingCoverOrBaseColor() {
        val css=".badge{background-size:cover;background-color:#ff0000;color:white}.badge:hover{background-size:contain;background-color:#00ff00;color:transparent}"
        assertEquals(ContentScale.Crop,adminShopBadgePreviewContentScale(css))
        assertFalse(adminShopBadgePreviewResolvedCss(css).contains("#00ff00"))
        assertEquals(ContentScale.Fit,adminShopBadgePreviewContentScale(".badge{background-size:contain}.badge:hover{background-size:cover}"))
    }
    @Test fun nestedHoverAndKeyframesCannotLeakPaintOrDimensions() {
        val css=".badge{background-size:cover; &:hover{background-size:contain;}} @keyframes size{0%{width:999px;color:transparent}100%{width:1000px}}"
        assertEquals(ContentScale.Crop,adminShopBadgePreviewContentScale(css))
        assertFalse(adminShopBadgePreviewResolvedCss(css).contains("999px"))
    }
    @Test fun brightnessSettingsRoundTripAndUnsafeValuesAreNormalized() {
        val store=ReaderSettingsStore(ApplicationProvider.getApplicationContext())
        val before=store.load()
        try {
            store.save(before.copy(brightness=.27f))
            assertEquals(.27f,store.load().toReaderUiOptions().toReaderSettingsValues().brightness,.0001f)
            assertEquals(-1f,normalizeReaderBrightness(Float.NaN),0f)
            assertEquals(-1f,normalizeReaderBrightness(-1f),0f)
            assertEquals(1f,normalizeReaderBrightness(2f),0f);assertEquals(.01f,normalizeReaderBrightness(0f),0f)
        }finally {store.save(before)}
    }
    @Test fun readerWindowBrightnessRestoresPreexistingSystemOrAppValue() {
        val activity=Robolectric.buildActivity(Activity::class.java).setup().get()
        val window=activity.window
        for(previous in listOf(-1f,.68f)) {
            window.attributes=window.attributes.apply{screenBrightness=previous}
            val lease=ReaderBrightnessLease(window);lease.apply(.3f);assertEquals(.3f,window.attributes.screenBrightness,.0001f)
            lease.apply(.8f);lease.restore();assertEquals(previous,window.attributes.screenBrightness,.0001f)
        }
        activity.finish()
    }
}
